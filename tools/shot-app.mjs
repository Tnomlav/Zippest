#!/usr/bin/env node
// Capture phone-sized screenshots of the app prototype.
import { spawn } from "node:child_process";
import { mkdtemp, mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

const CHROME = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const PAGE = "file:///C:/Decompress/design/decompress-app.html";
const OUT = "C:\\Decompress\\design\\screens";
const PORT = 9334;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

let child, ws, seq = 1;
const pending = new Map();

function cmd(method, params = {}) {
  const id = seq++;
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
    ws.send(JSON.stringify({ id, method, params }));
  });
}

async function js(expression) {
  const r = await cmd("Runtime.evaluate", { expression, returnByValue: true, awaitPromise: true, userGesture: true });
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text);
  return r.result?.value;
}

async function shot(name) {
  const r = await cmd("Page.captureScreenshot", { format: "png", captureBeyondViewport: false });
  const path = join(OUT, name + ".png");
  await writeFile(path, Buffer.from(r.data, "base64"));
  console.log("saved " + path);
}

async function main() {
  await mkdir(OUT, { recursive: true });
  const userDir = await mkdtemp(join(tmpdir(), "od-shot-"));
  child = spawn(CHROME, [
    "--headless=new", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
    `--remote-debugging-port=${PORT}`, `--user-data-dir=${userDir}`,
    "--hide-scrollbars", "about:blank",
  ], { stdio: "ignore" });

  for (let i = 0; i < 60; i++) {
    try { if ((await fetch(`http://127.0.0.1:${PORT}/json/version`)).ok) break; } catch {}
    await sleep(250);
  }
  const target = await (await fetch(`http://127.0.0.1:${PORT}/json/new?${encodeURIComponent(PAGE)}`, { method: "PUT" })).json();
  ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((r, j) => { ws.addEventListener("open", r, { once: true }); ws.addEventListener("error", j, { once: true }); });
  ws.addEventListener("message", (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      const { resolve, reject } = pending.get(msg.id);
      pending.delete(msg.id);
      msg.error ? reject(new Error(msg.error.message)) : resolve(msg.result);
    }
  });

  await cmd("Runtime.enable");
  await cmd("Page.enable");
  await cmd("Emulation.setDeviceMetricsOverride", { width: 393, height: 852, deviceScaleFactor: 2, mobile: true });
  for (let i = 0; i < 60; i++) {
    if ((await js("document.readyState")) === "complete") break;
    await sleep(200);
  }
  await sleep(900);
  await shot("01-home");

  await js("document.getElementById('pickSampleButton').click()");
  await sleep(700);
  await shot("02-detail");

  await js("document.getElementById('detailExtractButton').click()");
  await sleep(1600);
  await shot("03-progress");

  await js("document.getElementById('cancelProgressButton')?.click()");
  await sleep(600);
  await js("document.getElementById('detailBackButton')?.click()");
  await sleep(600);

  await js(
    "(() => { const r=[...document.querySelectorAll('[data-recent-id]')].find(x=>x.textContent.includes('设计素材')); r?.click(); })()"
  );
  await sleep(600);
  await js("document.getElementById('detailExtractButton').click()");
  await sleep(600);
  await shot("04-password");
}

main()
  .catch((e) => { console.error("ERROR:", e.message); process.exitCode = 1; })
  .finally(() => {
    try { ws?.close(); } catch {}
    try { child?.kill(); } catch {}
  });
