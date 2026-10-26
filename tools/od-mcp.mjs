#!/usr/bin/env node
// Small stdio MCP bridge to the locally running Open Design app.
// Usage:
//   node tools/od-mcp.mjs call <tool> ['<json-args>']
//   node tools/od-mcp.mjs run --project <id> --prompt <file> [--create <name>] [--skill <id>] [--poll 30]
import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import { randomUUID } from "node:crypto";

const OD_EXE = "C:\\Open Design\\Open Design.exe";
const OD_CLI = "C:\\Open Design\\resources\\app\\prebundled\\daemon\\daemon-cli.mjs";
const OD_DATA = "C:\\Users\\23399\\AppData\\Roaming\\Open Design\\namespaces\\release-stable-win\\data";
const OD_PIPE = "\\\\.\\pipe\\open-design-sidecar-cc708405e9480d9de9afd0d4120385fd";

class OdClient {
  constructor() {
    this.child = spawn(OD_EXE, [OD_CLI, "mcp"], {
      env: {
        ...process.env,
        ELECTRON_RUN_AS_NODE: "1",
        OD_DATA_DIR: OD_DATA,
        OD_SIDECAR_CLIENT_ENDPOINT: OD_PIPE,
        OD_MCP_BOOTSTRAP_COMMAND: OD_EXE,
        OD_MCP_BOOTSTRAP_ARGS: '["--headless"]',
      },
      stdio: ["pipe", "pipe", "pipe"],
    });
    this.pending = new Map();
    this.nextId = 1;
    this.buffer = "";

    this.child.stdout.on("data", (chunk) => {
      this.buffer += chunk.toString("utf8");
      let idx;
      while ((idx = this.buffer.indexOf("\n")) >= 0) {
        const line = this.buffer.slice(0, idx).trim();
        this.buffer = this.buffer.slice(idx + 1);
        if (!line) continue;
        let msg;
        try {
          msg = JSON.parse(line);
        } catch {
          continue;
        }
        if (msg.id != null && this.pending.has(msg.id)) {
          const { resolve, reject } = this.pending.get(msg.id);
          this.pending.delete(msg.id);
          if (msg.error) reject(new Error(msg.error.message || JSON.stringify(msg.error)));
          else resolve(msg.result);
        }
      }
    });

    this.child.stderr.on("data", (d) => process.stderr.write(d));
    this.child.on("exit", (code) => {
      for (const { reject } of this.pending.values()) {
        reject(new Error(`Open Design MCP exited (code ${code})`));
      }
      this.pending.clear();
    });
  }

  request(method, params) {
    const id = this.nextId++;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      this.child.stdin.write(JSON.stringify({ jsonrpc: "2.0", id, method, params }) + "\n");
    });
  }

  notify(method, params) {
    this.child.stdin.write(JSON.stringify({ jsonrpc: "2.0", method, params }) + "\n");
  }

  async init() {
    await this.request("initialize", {
      protocolVersion: "2024-11-05",
      capabilities: {},
      clientInfo: { name: "codex-bridge", version: "1.0" },
    });
    this.notify("notifications/initialized", {});
  }

  async call(name, args = {}) {
    const res = await this.request("tools/call", { name, arguments: args });
    const text = res?.content?.find((c) => c.type === "text")?.text;
    if (!text) return res;
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }

  close() {
    this.child.kill();
  }
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function parseRunArgs(argv) {
  const out = { poll: 30 };
  for (let i = 0; i < argv.length; i++) {
    const key = argv[i];
    const val = argv[i + 1];
    if (key === "--project") { out.project = val; i++; }
    else if (key === "--prompt") { out.promptFile = val; i++; }
    else if (key === "--create") { out.create = val; i++; }
    else if (key === "--skill") { out.skill = val; i++; }
    else if (key === "--agent") { out.agent = val; i++; }
    else if (key === "--poll") { out.poll = Number(val) || 30; i++; }
    else if (key === "--no-wait") { out.noWait = true; }
  }
  return out;
}

async function main() {
  const [cmd, ...argv] = process.argv.slice(2);
  if (!cmd) {
    console.error("usage: od-mcp.mjs call <tool> [json] | run --project <id> --prompt <file>");
    process.exit(2);
  }

  const client = new OdClient();
  try {
    await client.init();

    if (cmd === "call") {
      const tool = argv[0];
      const payload = argv[1] ? JSON.parse(argv[1]) : {};
      const result = await client.call(tool, payload);
      console.log(JSON.stringify(result, null, 2));
      return;
    }

    if (cmd === "run") {
      const opts = parseRunArgs(argv);
      if (!opts.project) throw new Error("--project is required");

      if (opts.create) {
        const project = await client.call("create_project", { name: opts.create, id: opts.project });
        console.log("PROJECT=" + JSON.stringify(project));
      }

      const prompt = opts.promptFile ? await readFile(opts.promptFile, "utf8") : "";
      const requestId = randomUUID();
      const startArgs = { project: opts.project, prompt, requestId };
      if (opts.skill) startArgs.skill = opts.skill;
      if (opts.agent) startArgs.agent = opts.agent;
      const started = await client.call("start_run", startArgs);
      console.log("START=" + JSON.stringify(started));

      const runId = started?.runId || started?.run?.id;
      if (!runId) throw new Error("start_run returned no runId");
      console.log("RUN_ID=" + runId);
      if (opts.noWait) return;

      for (;;) {
        await sleep(opts.poll * 1000);
        const run = await client.call("get_run", { runId });
        const status = run?.status || "unknown";
        console.log(`[${new Date().toISOString()}] status=${status}` + (run?.agentMessage ? ` msg=${String(run.agentMessage).slice(0, 300)}` : ""));
        if (["succeeded", "failed", "canceled"].includes(status)) {
          console.log("FINAL=" + JSON.stringify(run, null, 2));
          if (status === "succeeded") {
            const files = await client.call("list_files", { project: opts.project });
            console.log("FILES=" + JSON.stringify(files, null, 2));
          }
          process.exit(status === "succeeded" ? 0 : 1);
        }
      }
    }

    throw new Error(`unknown command: ${cmd}`);
  } finally {
    client.close();
  }
}

main().catch((err) => {
  console.error("ERROR: " + err.message);
  process.exit(1);
});
