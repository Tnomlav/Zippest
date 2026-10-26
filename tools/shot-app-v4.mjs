#!/usr/bin/env node
// Capture phone-sized screenshots of the file-manager root prototype.
import { spawn } from "node:child_process";
import { mkdtemp, mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

const CHROME = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const PAGE = "file:///C:/Decompress/design/decompress-app.html";
const OUT = "C:\\Decompress\\design\\screens";
const PORT = 9338;
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
  const userDir = await mkdtemp(join(tmpdir(), "od-v4-shot-"));
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
  await shot("01-root");

  await js("document.querySelector('[data-row-id=\"downloadDir\"]').click()");
  await sleep(650);
  await shot("02-download");

  await js("(() => { const r=document.querySelector('[data-row-id=\"sampleZip\"]'); r.dispatchEvent(new PointerEvent('pointerdown',{bubbles:true,button:0,clientX:120,clientY:240})); })()");
  await sleep(600);
  await js("document.querySelector('[data-row-id=\"assetsRar\"]').click()");
  await sleep(450);
  await shot("03-multiselect");
  await js("document.getElementById('defaultExtract').click()");
  await sleep(500);
  await shot("03b-multi-password");
  await js("document.getElementById('sheetScrim').click()");
  await sleep(400);
  await js("document.getElementById('selectionClose').click()");
  await sleep(350);

  await js("document.querySelector('[data-row-id=\"sampleZip\"]').click()");
  await sleep(700);
  await shot("04-archive");

  await js("document.getElementById('archiveExtract').click()");
  await sleep(600);
  await shot("05-extract-sheet");
  await js("document.getElementById('extractClipboardButton').click()");
  await sleep(400);
  await shot("05b-clipboard");
  await js("document.getElementById('extractClipboardButton').click()");
  await sleep(200);
  await js("document.querySelector('[data-dest-mode=\"custom\"]').click()");
  await sleep(500);
  await shot("05c-folder-picker");
  await js("document.getElementById('folderPickerClose').click()");
  await sleep(300);
  await js("document.querySelector('[data-dest-mode=\"new-folder\"]').click()");
  await sleep(200);

  await js("document.querySelector('[data-sheet-action=\"start-extract\"]').click()");
  await sleep(1400);
  await shot("06-progress");

  await js("document.getElementById('drawerClose')?.click()");
  await sleep(400);
  for (let i = 0; i < 8; i++) {
    const p = await js("document.getElementById('folderTitle').textContent.trim()");
    if (p === "内部存储") break;
    await js("document.getElementById('topBackButton').click()");
    await sleep(350);
  }
  await js("document.getElementById('moreButton').click()");
  await sleep(250);
  await js("document.querySelector('[data-more-action=\"settings\"]').click()");
  await sleep(600);
  await shot("07-settings");
  await js("document.getElementById('settingsBack').click()");
  await sleep(450);
  await js("document.getElementById('storageSwitchButton').click()");
  await sleep(250);
  await js("document.querySelector('[data-location=\"album\"]').click()");
  await sleep(650);
  await shot("08-album");
}

main()
  .catch((e) => { console.error("ERROR:", e.message); process.exitCode = 1; })
  .finally(() => {
    try { ws?.close(); } catch {}
    try { child?.kill(); } catch {}
  });
