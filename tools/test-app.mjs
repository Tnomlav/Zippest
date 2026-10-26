#!/usr/bin/env node
// Headless interaction test for the Open Design app prototype.
import { spawn } from "node:child_process";
import { mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

const CHROME = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const PAGE = "file:///C:/Decompress/design/decompress-app.html";
const PORT = 9333;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

let child;
let ws;
let seq = 1;
const pending = new Map();

async function waitForChrome() {
  for (let i = 0; i < 60; i++) {
    try {
      const r = await fetch(`http://127.0.0.1:${PORT}/json/version`);
      if (r.ok) return;
    } catch {}
    await sleep(250);
  }
  throw new Error("Chrome did not start");
}

function cmd(method, params = {}) {
  const id = seq++;
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
    ws.send(JSON.stringify({ id, method, params }));
  });
}

async function js(expression) {
  const r = await cmd("Runtime.evaluate", {
    expression,
    returnByValue: true,
    awaitPromise: true,
    userGesture: true,
  });
  if (r.exceptionDetails) {
    throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text);
  }
  return r.result?.value;
}

const results = [];
function check(name, ok, detail = "") {
  results.push({ name, ok, detail });
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? "  — " + detail : ""}`);
}

async function main() {
  const userDir = await mkdtemp(join(tmpdir(), "od-app-test-"));
  child = spawn(
    CHROME,
    [
      "--headless=new",
      "--disable-gpu",
      "--no-first-run",
      "--no-default-browser-check",
      `--remote-debugging-port=${PORT}`,
      `--user-data-dir=${userDir}`,
      "--window-size=393,852",
      "--hide-scrollbars",
      "about:blank",
    ],
    { stdio: "ignore" }
  );

  await waitForChrome();
  const target = await (
    await fetch(`http://127.0.0.1:${PORT}/json/new?${encodeURIComponent(PAGE)}`, {
      method: "PUT",
    })
  ).json();
  ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((r, j) => {
    ws.addEventListener("open", r, { once: true });
    ws.addEventListener("error", j, { once: true });
  });
  ws.addEventListener("message", (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      const { resolve, reject } = pending.get(msg.id);
      pending.delete(msg.id);
      if (msg.error) reject(new Error(msg.error.message));
      else resolve(msg.result);
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
  check(
    "starts on home",
    (await js("document.getElementById('page-home').getAttribute('aria-hidden')")) === "false"
  );
  const appBox = await js(
    "(() => { const r = document.getElementById('app').getBoundingClientRect(); return {w: Math.round(r.width), h: Math.round(r.height)}; })()"
  );
  check("app viewport sized", appBox.w >= 320 && appBox.w <= 480 && appBox.h > 600, JSON.stringify(appBox));
  check("status bar present", (await js("!!document.querySelector('.statusbar')")) === true);
  check("gesture bar present", (await js("!!document.querySelector('.gesture-bar')")) === true);
  const navLabels = await js(
    "[...document.querySelectorAll('#bottomNav [data-nav]')].map(b => b.textContent.trim()).join('|')"
  );
  check("bottom nav has 3 tabs", navLabels.split("|").length === 3, navLabels);
  check(
    "no horizontal overflow",
    (await js("document.documentElement.scrollWidth <= window.innerWidth + 1")) === true
  );
  const smallTargets = await js(
    "(() => { const a=document.getElementById('app').getBoundingClientRect(); return [...document.querySelectorAll('#app button')].filter(b=>{const r=b.getBoundingClientRect(); return r.width>0 && r.height>0 && (r.height<44 || r.width<44);}).map(b=>(b.id||b.className)+':'+Math.round(b.getBoundingClientRect().width)+'x'+Math.round(b.getBoundingClientRect().height)); })()"
  );
  check("touch targets >= 44dp", smallTargets.length === 0, smallTargets.join(", ") || "all ok");

  // Home -> detail
  await js("document.getElementById('pickSampleButton').click()");
  await sleep(500);
  check(
    "opens detail",
    (await js("document.getElementById('page-detail').getAttribute('aria-hidden')")) === "false"
  );
  const detailName = await js("document.getElementById('detailFileName').textContent.trim()");
  check("detail shows archive", detailName.includes("sample.zip"), detailName);

  // Detail -> progress -> success
  await js("document.getElementById('detailExtractButton').click()");
  await sleep(400);
  check(
    "starts extraction",
    (await js("document.getElementById('page-progress').getAttribute('aria-hidden')")) === "false"
  );
  await sleep(5200);
  const successVisible = await js("!document.getElementById('progressSuccess').hidden");
  check("extraction reaches success", successVisible === true);

  await js("document.getElementById('openFolderButton').click()");
  await sleep(600);
  check(
    "returns home after success",
    (await js("document.getElementById('page-home').getAttribute('aria-hidden')")) === "false"
  );

  // Encrypted archive -> password sheet
  const openedEncrypted = await js(
    "(() => { const rows=[...document.querySelectorAll('[data-recent-id]')]; const r=rows.find(x=>x.textContent.includes('设计素材')); if(!r) return null; r.click(); return r.getAttribute('data-recent-id'); })()"
  );
  check("encrypted recent item exists", !!(await js("document.querySelectorAll('[data-recent-id]').length")), openedEncrypted);
  await sleep(500);
  check("encrypted chip shown", (await js("!document.getElementById('detailEncryptedChip').hidden")) === true);

  await js("document.getElementById('detailExtractButton').click()");
  await sleep(500);
  check("password sheet opens", (await js("!document.getElementById('passwordLayer').hidden")) === true);

  await js(
    "(() => { const i=document.getElementById('passwordInput'); i.value='12'; i.dispatchEvent(new Event('input',{bubbles:true})); document.getElementById('passwordSubmit').click(); })()"
  );
  await sleep(300);
  check("short password rejected", (await js("!document.getElementById('passwordError').hidden")) === true);

  await js(
    "(() => { const i=document.getElementById('passwordInput'); i.value='1234'; i.dispatchEvent(new Event('input',{bubbles:true})); document.getElementById('passwordSubmit').click(); })()"
  );
  await sleep(600);
  check("valid password starts extraction", (await js("document.getElementById('page-progress').getAttribute('aria-hidden')")) === "false");

  await js("document.getElementById('cancelProgressButton')?.click()");
  await sleep(500);

  // Tabs
  await js("document.querySelector('[data-nav=\"tasks\"]').click()");
  await sleep(400);
  check("tasks tab", (await js("document.getElementById('page-tasks').getAttribute('aria-hidden')")) === "false");
  const taskStats = await js(
    "[...document.querySelectorAll('.task-stat strong')].map(e=>e.textContent.trim()).join(',')"
  );
  check("task stats render", taskStats.split(",").length >= 3, taskStats);

  const toggled = await js(
    "(() => { const b=document.querySelector('[data-task-action]'); if(!b) return null; const id=b.dataset.taskAction; const before=b.closest('[data-task-row]').querySelector('.task-status').textContent.trim(); b.click(); const row=document.querySelector('[data-task-row=\"'+id+'\"]'); const after=row.querySelector('.task-status').textContent.trim(); return before + ' -> ' + after + ' | ' + row.className; })()"
  );
  check("task pause/resume toggles", !!toggled && toggled.split(" -> ")[0] !== toggled.split(" -> ")[1].split(" | ")[0], String(toggled));

  await js("document.querySelector('[data-nav=\"settings\"]').click()");
  await sleep(400);
  check("settings tab", (await js("document.getElementById('page-settings').getAttribute('aria-hidden')")) === "false");

  await js("document.getElementById('themeSwitch').click()");
  await sleep(300);
  check("dark mode toggles", (await js("document.documentElement.dataset.theme")) === "dark");
  await js("document.getElementById('themeSwitch').click()");
  await sleep(200);

  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} checks passed`);
  process.exitCode = failed.length ? 1 : 0;
}

main()
  .catch((e) => {
    console.error("ERROR:", e.message);
    process.exitCode = 1;
  })
  .finally(() => {
    try { ws?.close(); } catch {}
    try { child?.kill(); } catch {}
  });
