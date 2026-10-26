#!/usr/bin/env node
// Headless interaction test for the ZArchiver-style file browser prototype.
import { spawn } from "node:child_process";
import { mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

const CHROME = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const PAGE = "file:///C:/Decompress/design/decompress-app.html";
const PORT = 9335;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

let child, ws, seq = 1;
const pending = new Map();
const results = [];

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

function check(name, ok, detail = "") {
  results.push({ name, ok });
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? "  — " + detail : ""}`);
}

async function main() {
  const userDir = await mkdtemp(join(tmpdir(), "od-v3-test-"));
  child = spawn(CHROME, [
    "--headless=new", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
    `--remote-debugging-port=${PORT}`, `--user-data-dir=${userDir}`,
    "--window-size=393,852", "--hide-scrollbars", "about:blank",
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
  for (let i = 0; i < 60; i++) {
    if ((await js("document.readyState")) === "complete") break;
    await sleep(200);
  }
  await sleep(800);

  check("page title", (await js("document.title")) === "解压", await js("document.title"));
  check("opens in file browser", (await js("document.getElementById('folderTitle').textContent.trim()")) === "Download");
  const crumb = await js("document.getElementById('breadcrumb').textContent.replace(/\\s+/g,' ').trim()");
  check("breadcrumb shows storage path", crumb.includes("内部存储") && crumb.includes("Download"), crumb);
  const rowCount = await js("document.querySelectorAll('#fileList [data-row-id]').length");
  check("file list populated", rowCount >= 5, String(rowCount));
  const firstRow = await js("document.querySelector('#fileList [data-row-id]')?.textContent.trim() || ''");
  check("folders sorted first", firstRow.includes("素材"), firstRow.slice(0, 40));
  check("no horizontal overflow", (await js("document.documentElement.scrollWidth <= window.innerWidth + 1")) === true);
  const small = await js("(() => [...document.querySelectorAll('#app button')].filter(b=>{const r=b.getBoundingClientRect(); return r.width>0&&r.height>0&&(r.height<44||r.width<44);}).map(b=>b.id||b.className))()");
  check("touch targets >= 44dp", small.length === 0, small.join(",") || "all ok");
  const homeToolbars = await js(
    "(() => { const d=document.getElementById('defaultToolbar'), a=document.getElementById('archiveToolbar'); return {d:getComputedStyle(d).display, a:getComputedStyle(a).display}; })()"
  );
  check("home shows only default toolbar", homeToolbars.d !== "none" && homeToolbars.a === "none", JSON.stringify(homeToolbars));

  // Folder navigation
  await js("document.querySelector('[data-row-id=\"materialsDir\"]').click()");
  await sleep(450);
  check("enters folder", (await js("document.getElementById('folderTitle').textContent.trim()")) === "素材");
  check("breadcrumb updates", (await js("document.getElementById('breadcrumb').textContent")).includes("素材"));
  await js("document.getElementById('upButton').click()");
  await sleep(450);
  check("up returns to Download", (await js("document.getElementById('folderTitle').textContent.trim()")) === "Download");

  // Search
  await js("document.getElementById('searchButton').click()");
  await sleep(250);
  await js("(() => { const i=document.getElementById('searchInput'); i.value='zip'; i.dispatchEvent(new Event('input',{bubbles:true})); })()");
  await sleep(350);
  const searchNames = await js("[...document.querySelectorAll('#fileList .file-name')].map(e=>e.textContent.trim())");
  check("search filters list", searchNames.length >= 1 && searchNames.every(n => n.toLowerCase().includes("zip")), searchNames.join(","));
  await js("document.getElementById('searchClear').click()");
  await sleep(300);
  check(
    "search clears",
    (await js("document.getElementById('searchInput').value === '' && document.querySelectorAll('#fileList [data-row-id]').length >= 5")) === true
  );

  // Sort
  const beforeOrder = await js("[...document.querySelectorAll('#fileList [data-row-id]')].map(e=>e.dataset.rowId).join(',')");
  await js("document.getElementById('sortButton').click()");
  await sleep(250);
  await js("document.querySelector('[data-sort-key=\"size\"]').click()");
  await sleep(400);
  const afterOrder = await js("[...document.querySelectorAll('#fileList [data-row-id]')].map(e=>e.dataset.rowId).join(',')");
  check("sort by size reorders", beforeOrder !== afterOrder, `${beforeOrder.slice(0, 30)} -> ${afterOrder.slice(0, 30)}`);

  // Archive browsing
  await js("document.querySelector('[data-row-id=\"sampleZip\"]').click()");
  await sleep(450);
  const archiveCrumb = await js("document.getElementById('breadcrumb').textContent.replace(/\\s+/g,' ').trim()");
  check("opens archive contents", archiveCrumb.includes("sample.zip"), archiveCrumb);
  const toolbarState = await js(
    "(() => { const a=document.getElementById('archiveToolbar'); const d=document.getElementById('defaultToolbar'); return {hidden:a.hidden, display:getComputedStyle(a).display, dHidden:d.hidden, dDisplay:getComputedStyle(d).display}; })()"
  );
  check(
    "archive toolbar replaces default",
    toolbarState.display !== "none" && toolbarState.dDisplay === "none",
    JSON.stringify(toolbarState)
  );

  // Extract flow
  await js("document.getElementById('archiveExtract').click()");
  await sleep(400);
  check("extract sheet opens", (await js("document.getElementById('sheetLayer').hidden")) === false);
  await js("document.querySelector('[data-sheet-action=\"start-extract\"]').click()");
  await sleep(600);
  check("task drawer appears", (await js("document.getElementById('taskDrawer').hidden")) === false);
  const stateBefore = await js("document.getElementById('currentTaskState').textContent.trim()");
  await js("document.getElementById('currentTaskToggle').click()");
  await sleep(300);
  const stateAfter = await js("document.getElementById('currentTaskState').textContent.trim()");
  check("pause/resume works", stateBefore !== stateAfter, `${stateBefore} -> ${stateAfter}`);
  await js("document.getElementById('currentTaskToggle').click()");
  await sleep(200);
  await sleep(5000);
  const finalState = await js("document.getElementById('currentTaskState').textContent.trim()");
  check("extraction completes", finalState.includes("完成"), finalState);
  await js("document.getElementById('drawerClose')?.click()");
  await sleep(300);

  // Back to download, encrypted flow
  for (let i = 0; i < 4; i++) {
    const title = await js("document.getElementById('folderTitle').textContent.trim()");
    if (title === "Download") break;
    await js("document.getElementById('upButton').click()");
    await sleep(350);
  }
  await js("document.querySelector('[data-row-id=\"assetsRar\"]').click()");
  await sleep(450);
  await js("document.getElementById('archiveExtract').click()");
  await sleep(400);
  await js("(() => { const i=document.getElementById('extractPassword'); i.value='12'; i.dispatchEvent(new Event('input',{bubbles:true})); })()");
  await js("document.querySelector('[data-sheet-action=\"start-extract\"]').click()");
  await sleep(350);
  check("short password rejected", (await js("document.getElementById('extractPasswordError').hidden")) === false);
  await js("(() => { const i=document.getElementById('extractPassword'); i.value='1234'; i.dispatchEvent(new Event('input',{bubbles:true})); })()");
  await js("document.querySelector('[data-sheet-action=\"start-extract\"]').click()");
  await sleep(500);
  check("valid password starts", (await js("document.getElementById('taskDrawer').hidden")) === false);
  await js("document.getElementById('drawerClose')?.click()");
  await sleep(300);

  // Multi-select
  for (let i = 0; i < 4; i++) {
    const title = await js("document.getElementById('folderTitle').textContent.trim()");
    if (title === "Download") break;
    await js("document.getElementById('upButton').click()");
    await sleep(350);
  }
  await js("document.querySelector('[data-check-id=\"sampleZip\"]').click()");
  await sleep(200);
  await js("document.querySelector('[data-check-id=\"readmeTxt\"]')?.click()");
  await sleep(300);
  const selectionTitle = await js("document.getElementById('selectionTitle').textContent.trim()");
  check("multi-select shows count", selectionTitle.includes("已选 2 项"), selectionTitle);
  check("multi toolbar visible", (await js("document.getElementById('multiToolbar').hidden")) === false);
  await js("document.getElementById('selectionClose').click()");
  await sleep(300);
  check("selection clears", (await js("document.getElementById('selectionAppbar').hidden")) === true);

  // Settings + theme
  await js("document.getElementById('moreButton').click()");
  await sleep(250);
  await js("document.querySelector('[data-more-action=\"settings\"]').click()");
  await sleep(450);
  check("settings opens", (await js("document.getElementById('settingsView').classList.contains('open')")) === true);
  await js("document.getElementById('themeSwitch').click()");
  await sleep(300);
  check("dark mode toggles", (await js("document.documentElement.dataset.theme")) === "dark");
  await js("document.getElementById('themeSwitch').click()");
  await sleep(200);

  const failed = results.filter(r => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} checks passed`);
  process.exitCode = failed.length ? 1 : 0;
}

main()
  .catch((e) => { console.error("ERROR:", e.message); process.exitCode = 1; })
  .finally(() => {
    try { ws?.close(); } catch {}
    try { child?.kill(); } catch {}
  });
