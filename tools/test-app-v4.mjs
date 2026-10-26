#!/usr/bin/env node
// Headless interaction test for the phone-file-manager root screen.
import { spawn } from "node:child_process";
import { mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

const CHROME = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const PAGE = "file:///C:/Decompress/design/decompress-app.html";
const PORT = 9337;
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

async function longPressRow(id) {
  await js(
    `(() => { const row=document.querySelector('[data-row-id="${id}"]'); if(!row) return false; row.dispatchEvent(new PointerEvent("pointerdown", {bubbles:true, button:0, clientX:120, clientY:240})); return true; })()`
  );
  await sleep(600);
}

async function main() {
  const userDir = await mkdtemp(join(tmpdir(), "od-v4-test-"));
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
  check("opens at internal storage root", (await js("document.getElementById('folderTitle').textContent.trim()")) === "内部存储");
  const usage = await js("document.getElementById('storageUsage').textContent.trim()");
  check("storage capacity shown", usage.includes("128 GB"), usage);
  const storageBox = await js(
    "(() => { const m=document.getElementById('storageMeter'); const mr=m.getBoundingClientRect(); const a=document.getElementById('app').getBoundingClientRect(); const f=document.querySelector('.file-area').getBoundingClientRect(); return {hidden:m.hidden,meterBottom:Math.round(mr.bottom),appBottom:Math.round(a.bottom),meterTop:Math.round(mr.top),fileBottom:Math.round(f.bottom)}; })()"
  );
  check(
    "storage bar at page bottom",
    !storageBox.hidden && Math.abs(storageBox.meterBottom - storageBox.appBottom) <= 2 && Math.abs(storageBox.meterTop - storageBox.fileBottom) <= 2,
    JSON.stringify(storageBox)
  );
  const rootNames = await js("[...document.querySelectorAll('#fileList .file-name')].map(e=>e.textContent.trim())");
  check("root has 14 system folders", rootNames.length === 14, rootNames.join(","));
  const required = ["Android", "DCIM", "Download", "Documents", "Movies", "Music", "Pictures", "微信", "腾讯"];
  const missing = required.filter(n => !rootNames.includes(n));
  check("real Android folders present", missing.length === 0, missing.join(",") || "all present");
  check(
    "root uses folder icons",
    (await js("document.querySelectorAll('#fileList .file-icon').length > 0 && document.querySelectorAll('#fileList .file-thumb').length === 0")) === true
  );
  check("no horizontal overflow", (await js("document.documentElement.scrollWidth <= window.innerWidth + 1")) === true);
  const small = await js("(() => [...document.querySelectorAll('#app button')].filter(b=>b.id!=='gestureHomeButton').filter(b=>{const r=b.getBoundingClientRect(); return r.width>0&&r.height>0&&(r.height<44||r.width<44);}).map(b=>b.id||b.className))()");
  check("touch targets >= 44dp", small.length === 0, small.join(",") || "all ok");

  const toolbar = await js(
    "(() => { const ids=['defaultExtract','defaultCopy','defaultMove','defaultDelete']; const o={}; for(const id of ids){o[id]=document.getElementById(id)?.disabled;} return o; })()"
  );
  check(
    "bottom actions disabled without selection",
    toolbar.defaultExtract === true && toolbar.defaultCopy === true && toolbar.defaultMove === true && toolbar.defaultDelete === true,
    JSON.stringify(toolbar)
  );
  check("no checkboxes at rest", (await js("document.querySelectorAll('#fileList [data-check-id]').length")) === 0);
  check("breadcrumb row removed", (await js("document.getElementById('breadcrumb') === null")) === true);
  const backIcons = await js(
    "(() => { const t=document.querySelector('#topBackButton use')?.getAttribute('href'); const s=document.querySelector('#searchBack use')?.getAttribute('href'); const g=document.querySelector('#settingsBack use')?.getAttribute('href'); const size=getComputedStyle(document.querySelector('#topBackButton .icon')).width; return {top:t,search:s,settings:g,size}; })()"
  );
  check(
    "all back buttons share search icon",
    backIcons.top === "#i-back" && backIcons.search === "#i-back" && backIcons.settings === "#i-back" && backIcons.size === "24px",
    JSON.stringify(backIcons)
  );
  check("top back disabled at root", (await js("document.getElementById('topBackButton').disabled")) === true);
  const topLayout = await js(
    "(() => { const b=document.getElementById('topBackButton').getBoundingClientRect(); const s=document.getElementById('storageSwitchButton').getBoundingClientRect(); const r=document.querySelector('.toolbar-icon-row').getBoundingClientRect(); const t=document.getElementById('folderTitle').getBoundingClientRect(); return {bRight:Math.round(b.right),sLeft:Math.round(s.left),sRight:Math.round(s.right),rLeft:Math.round(r.left),titleW:Math.round(t.width)}; })()"
  );
  check(
    "top bar items do not overlap",
    topLayout.bRight <= topLayout.sLeft + 1 && topLayout.sRight <= topLayout.rLeft + 1 && topLayout.titleW > 40,
    JSON.stringify(topLayout)
  );

  // Top menu and standalone new button
  await js("document.getElementById('moreButton').click()");
  await sleep(250);
  check("top menu opens", (await js("!document.getElementById('moreMenu').hidden")) === true);
  const topMenuText = await js("document.getElementById('moreMenu').textContent.replace(/\\s+/g,' ').trim()");
  check(
    "top menu only tasks/settings/about",
    topMenuText.includes("任务队列") && topMenuText.includes("设置") && topMenuText.includes("关于") && !topMenuText.includes("刷新") && !topMenuText.includes("全选") && !topMenuText.includes("新建"),
    topMenuText
  );
  await js("document.getElementById('moreButton').click()");
  await sleep(200);
  check("bottom toolbar hidden without selection", (await js("document.getElementById('defaultToolbar').hidden")) === true);
  check("bottom toolbar has no new action", (await js("document.querySelector('#defaultToolbar #newArchiveButton') === null && !document.getElementById('defaultToolbar').textContent.includes('新建')")) === true);
  check(
    "standalone new button in top bar",
    (await js("!!document.querySelector('#normalAppbar #topNewButton') && !document.getElementById('topNewButton').disabled")) === true
  );
  await js("document.getElementById('topNewButton').click()");
  await sleep(400);
  const newSheetText = await js("document.getElementById('sheetContent').textContent.replace(/\\s+/g,' ').trim()");
  check(
    "new button opens create options",
    (await js("!document.getElementById('sheetLayer').hidden")) === true && newSheetText.includes("新建文件夹") && newSheetText.includes("新建压缩包"),
    newSheetText
  );
  await js("document.getElementById('sheetScrim').click()");
  await sleep(400);

  // Enter Download
  await js("document.querySelector('[data-row-id=\"downloadDir\"]').click()");
  await sleep(450);
  check("enters Download", (await js("document.getElementById('folderTitle').textContent.trim()")) === "Download");
  const crumb = await js("document.getElementById('folderTitle').textContent.trim()");
  check("top bar shows current folder", crumb === "Download", crumb);
  check("top back enabled in folder", (await js("document.getElementById('topBackButton').disabled")) === false);
  const dlNames = await js("[...document.querySelectorAll('#fileList .file-name')].map(e=>e.textContent.trim())");
  check("Download has archives", dlNames.includes("sample.zip") && dlNames.includes("assets.rar"), dlNames.join(","));
  check("bottom toolbar hidden in folder without selection", (await js("document.getElementById('defaultToolbar').hidden")) === true);
  check("no checkboxes in folder at rest", (await js("document.querySelectorAll('#fileList [data-check-id]').length")) === 0);

  // Android-style gestures
  await js("(() => { const app=document.getElementById('app'); const r=app.getBoundingClientRect(); app.dispatchEvent(new PointerEvent('pointerdown',{bubbles:true,pointerId:11,button:0,clientX:r.left+6,clientY:r.top+300})); app.dispatchEvent(new PointerEvent('pointerup',{bubbles:true,pointerId:11,button:0,clientX:r.left+110,clientY:r.top+300})); })()");
  await sleep(400);
  check("edge swipe goes back", (await js("document.getElementById('folderTitle').textContent.trim()")) === "内部存储");
  await js("document.querySelector('[data-row-id=\"downloadDir\"]').click()");
  await sleep(400);
  await js("(() => { const app=document.getElementById('app'); const r=app.getBoundingClientRect(); app.dispatchEvent(new PointerEvent('pointerdown',{bubbles:true,pointerId:12,button:0,clientX:r.left+r.width/2,clientY:r.top+r.height-8})); app.dispatchEvent(new PointerEvent('pointerup',{bubbles:true,pointerId:12,button:0,clientX:r.left+r.width/2,clientY:r.top+r.height-110})); })()");
  await sleep(400);
  check("bottom swipe goes home", (await js("document.getElementById('folderTitle').textContent.trim()")) === "内部存储");
  await js("document.querySelector('[data-row-id=\"downloadDir\"]').click()");
  await sleep(400);
  await js("document.getElementById('gestureHomeButton').click()");
  await sleep(400);
  check("gesture bar tap goes home", (await js("document.getElementById('folderTitle').textContent.trim()")) === "内部存储");

  // Quick locations
  await js("document.getElementById('topBackButton').click()");
  await sleep(400);
  check(
    "top back returns to root",
    (await js("document.getElementById('folderTitle').textContent.trim() === '内部存储' && document.getElementById('topBackButton').disabled")) === true
  );
  await js("document.getElementById('storageSwitchButton').click()");
  await sleep(300);
  const locationLabels = await js("[...document.querySelectorAll('#storageMenu [data-location]')].map(b=>b.textContent.trim())");
  check(
    "quick locations listed",
    ["目录首页", "下载", "相册", "文档", "微信"].every((label) => locationLabels.includes(label)),
    locationLabels.join(",")
  );
  check("volume items removed", (await js("document.querySelectorAll('#storageMenu [data-volume]').length")) === 0);
  const locationMenuBox = await js(
    "(() => { const b=document.getElementById('storageSwitchButton').getBoundingClientRect(); const m=document.getElementById('storageMenu').getBoundingClientRect(); const a=document.getElementById('app').getBoundingClientRect(); return {btnLeft:Math.round(b.left),menuLeft:Math.round(m.left),menuRight:Math.round(m.right),appRight:Math.round(a.right)}; })()"
  );
  check(
    "location menu anchored left",
    Math.abs(locationMenuBox.menuLeft - locationMenuBox.btnLeft) <= 24 && locationMenuBox.menuRight <= locationMenuBox.appRight,
    JSON.stringify(locationMenuBox)
  );
  const compactSelector = await js(
    "(() => { const t=document.getElementById('folderTitle'); const c=document.querySelector('.volume-chevron'); const tr=t.getBoundingClientRect(); const cr=c.getBoundingClientRect(); return {font:getComputedStyle(t).fontSize, chevron:getComputedStyle(c).width, gap:Math.round(cr.left-tr.right)}; })()"
  );
  check(
    "title and arrow compact",
    compactSelector.font === "15px" && compactSelector.chevron === "20px" && compactSelector.gap <= 4,
    JSON.stringify(compactSelector)
  );

  await js("document.querySelector('[data-location=\"album\"]').click()");
  await sleep(500);
  check("album location opens", (await js("document.getElementById('folderTitle').textContent.trim()")) === "Camera");
  const mediaThumbs = await js("[...document.querySelectorAll('#fileList .file-thumb')].map(e=>e.className)");
  check(
    "media files use thumbnails",
    mediaThumbs.some((name) => name.includes("photo")) && mediaThumbs.some((name) => name.includes("gif")) && mediaThumbs.some((name) => name.includes("video")),
    mediaThumbs.join(" | ")
  );
  check("gif thumbnail badge", (await js("!!document.querySelector('#fileList .file-thumb.gif .thumb-gif')")) === true);
  check("video thumbnail play badge", (await js("!!document.querySelector('#fileList .file-thumb.video .thumb-play')")) === true);
  await js("document.getElementById('storageSwitchButton').click()");
  await sleep(250);
  check("active location marked", (await js("document.querySelector('[data-location=\"album\"]').classList.contains('active')")) === true);
  await js("document.querySelector('[data-location=\"download\"]').click()");
  await sleep(500);
  check("download location opens", (await js("document.getElementById('folderTitle').textContent.trim()")) === "Download");
  await js("document.getElementById('topBackButton').click()");
  await sleep(400);
  check("back from quick download returns root", (await js("document.getElementById('folderTitle').textContent.trim()")) === "内部存储");

  // Back to Download: search + sort + multi-select
  await js("document.querySelector('[data-row-id=\"downloadDir\"]').click()");
  await sleep(450);
  await js("document.getElementById('searchButton').click()");
  await sleep(250);
  await js("(() => { const i=document.getElementById('searchInput'); i.value='zip'; i.dispatchEvent(new Event('input',{bubbles:true})); })()");
  await sleep(350);
  const searchNames = await js("[...document.querySelectorAll('#fileList .file-name')].map(e=>e.textContent.trim())");
  check("search filters current folder", searchNames.length >= 1 && searchNames.every(n => n.toLowerCase().includes("zip")), searchNames.join(","));
  await js("document.getElementById('searchClear').click()");
  await sleep(250);
  await js("document.getElementById('searchBack').click()");
  await sleep(250);

  const beforeOrder = await js("[...document.querySelectorAll('#fileList [data-row-id]')].map(e=>e.dataset.rowId).join(',')");
  await js("document.getElementById('sortButton').click()");
  await sleep(250);
  await js("document.querySelector('[data-sort-key=\"size\"]').click()");
  await sleep(400);
  const afterOrder = await js("[...document.querySelectorAll('#fileList [data-row-id]')].map(e=>e.dataset.rowId).join(',')");
  check("sort by size reorders", beforeOrder !== afterOrder);

  await longPressRow("sampleZip");
  const selTitle = await js("document.getElementById('selectionTitle').textContent.trim()");
  check("long press enters selection", selTitle.includes("已选 1 项"), selTitle);
  check("checkboxes appear after long press", (await js("document.querySelectorAll('#fileList [data-check-id]').length > 0")) === true);
  check(
    "select-all is a checkbox",
    (await js("!!document.querySelector('#selectAllButton .checkbox') && document.getElementById('selectAllButton').textContent.trim() === ''")) === true
  );
  const iconColors = await js(
    "(() => { const a=getComputedStyle(document.querySelector('#searchButton .icon')).color; const b=getComputedStyle(document.querySelector('#topNewButton .icon')).color; const c=getComputedStyle(document.querySelector('#defaultExtract .icon')).color; return {top:a,new:b,bottom:c}; })()"
  );
  check("top and bottom icons share color", iconColors.top === iconColors.bottom && iconColors.top === iconColors.new, JSON.stringify(iconColors));
  await js("document.getElementById('selectAllButton').click()");
  await sleep(350);
  check(
    "select-all checks all",
    (await js("document.getElementById('selectAllButton').getAttribute('aria-pressed') === 'true' && document.querySelectorAll('#fileList .file-row.selected').length === document.querySelectorAll('#fileList [data-row-id]').length")) === true
  );
  await js("document.getElementById('selectAllButton').click()");
  await sleep(350);
  check(
    "select-all clears, keeps mode, restores storage",
    (await js("!document.getElementById('selectionAppbar').hidden && document.getElementById('selectAllButton').getAttribute('aria-pressed') === 'false' && document.getElementById('selectionTitle').textContent.includes('已选 0 项') && document.getElementById('defaultToolbar').hidden && !document.getElementById('storageMeter').hidden")) === true
  );
  await js("document.getElementById('selectionClose').click()");
  await sleep(300);
  check("storage bar returns after selection closes", (await js("!document.getElementById('storageMeter').hidden")) === true);
  await longPressRow("sampleZip");
  const selTitle2 = await js("document.getElementById('selectionTitle').textContent.trim()");
  check("long press selects again", selTitle2.includes("已选 1 项"), selTitle2);
  check("extract enabled after selection", (await js("document.getElementById('defaultExtract').disabled")) === false);
  check("bottom toolbar appears with selection", (await js("document.getElementById('defaultToolbar').hidden")) === false);
  check("storage bar hidden with selection", (await js("document.getElementById('storageMeter').hidden")) === true);
  await js("document.getElementById('defaultMore').click()");
  await sleep(250);
  const bottomMenuText = await js("document.getElementById('fileActionsMenu').textContent.replace(/\\s+/g,' ').trim()");
  check(
    "bottom menu has file actions",
    bottomMenuText.includes("重命名") && bottomMenuText.includes("分享") && bottomMenuText.includes("属性") && !bottomMenuText.includes("刷新当前目录") && !bottomMenuText.includes("设置"),
    bottomMenuText
  );
  await js("document.querySelector('[data-file-action=\"rename\"]').click()");
  await sleep(400);
  check("rename sheet opens", (await js("!document.getElementById('sheetLayer').hidden && !!document.getElementById('renameInput')")) === true);
  await js("(() => { const i=document.getElementById('renameInput'); i.value='sample-renamed.zip'; })()");
  await js("document.querySelector('[data-sheet-action=\"rename-confirm\"]').click()");
  await sleep(500);
  const renamedNames = await js("[...document.querySelectorAll('#fileList .file-name')].map(e=>e.textContent.trim())");
  check("rename applies", renamedNames.includes("sample-renamed.zip"), renamedNames.join(","));
  check("bottom toolbar hides after selection clears", (await js("document.getElementById('defaultToolbar').hidden")) === true);
  check("storage bar returns after rename", (await js("!document.getElementById('storageMeter').hidden")) === true);
  await js("document.getElementById('selectionClose').click()");
  await sleep(350);

  // Shared password for multiple archives
  await longPressRow("sampleZip");
  await js("document.querySelector('[data-row-id=\"assetsRar\"]').click()");
  await sleep(350);
  const multiSel = await js("document.getElementById('selectionTitle').textContent.trim()");
  check("two archives selected", multiSel.includes("已选 2 项"), multiSel);
  await js("document.getElementById('defaultExtract').click()");
  await sleep(450);
  check(
    "same password option appears for multiple archives",
    (await js("!!document.getElementById('samePasswordToggle') && document.getElementById('samePasswordToggle').getAttribute('aria-checked') === 'false'")) === true
  );
  check("same password off hint", (await js("document.getElementById('samePasswordHint').textContent.includes('逐个输入密码')")) === true);
  await js("document.getElementById('samePasswordToggle').click()");
  await sleep(250);
  check(
    "same password toggles",
    (await js("document.getElementById('samePasswordToggle').getAttribute('aria-checked') === 'true' && document.getElementById('samePasswordHint').textContent.includes('复用此密码')")) === true
  );
  await js("document.getElementById('sheetScrim').click()");
  await sleep(400);
  await js("document.getElementById('selectionClose').click()");
  await sleep(350);

  // Archive browsing and extraction
  await js("document.querySelector('[data-row-id=\"sampleZip\"]').click()");
  await sleep(500);
  const archCrumb = await js("document.getElementById('folderTitle').textContent.trim()");
  check("opens archive contents", archCrumb === "sample.zip", archCrumb);
  check("new button disabled in archive", (await js("document.getElementById('topNewButton').disabled")) === true);
  check("storage bar hidden in archive", (await js("document.getElementById('storageMeter').hidden")) === true);
  const archToolbar = await js(
    "(() => { const a=document.getElementById('archiveToolbar'), d=document.getElementById('defaultToolbar'); return {a:getComputedStyle(a).display, d:getComputedStyle(d).display}; })()"
  );
  check("archive toolbar replaces default", archToolbar.a !== "none" && archToolbar.d === "none", JSON.stringify(archToolbar));

  await js("document.getElementById('archiveExtract').click()");
  await sleep(400);
  check("extract sheet opens", (await js("document.getElementById('sheetLayer').hidden")) === false);
  const firstSectionTitle = await js("document.querySelector('#sheetContent .sheet-section .sheet-section-title').textContent.trim()");
  check("password label has no optional", firstSectionTitle === "密码", firstSectionTitle);
  check("extract header row removed", (await js("document.querySelector('#sheetContent .sheet-head') === null")) === true);
  check(
    "clipboard value and chevron share row",
    (await js("!!document.querySelector('#extractClipboardButton .icon') && !document.getElementById('extractClipboardButton').textContent.includes('上次复制') && !!document.querySelector('#extractClipboardButton #lastClipboardPreview') && !!document.querySelector('#extractClipboardButton .clipboard-chevron')")) === true
  );
  const clipboardLayout = await js(
    "(() => { const t=document.getElementById('lastClipboardPreview').getBoundingClientRect(); const c=document.querySelector('#extractClipboardButton .clipboard-chevron').getBoundingClientRect(); return {textRight:Math.round(t.right), chevronLeft:Math.round(c.left)}; })()"
  );
  check("clipboard text left and chevron right", clipboardLayout.chevronLeft >= clipboardLayout.textRight, JSON.stringify(clipboardLayout));
  const sheetGeometry = await js(
    "(() => { const sections=[...document.querySelectorAll('#sheetContent .sheet-section')]; const actions=document.querySelector('#sheetContent .sheet-actions'); const conflict=sections[2].getBoundingClientRect(); const encoding=sections[3].getBoundingClientRect(); const actionRect=actions.getBoundingClientRect(); return {conflictBottom:Math.round(conflict.bottom),actionTop:Math.round(actionRect.top),encodingTop:Math.round(encoding.top)}; })()"
  );
  check("conflict fully visible above actions", sheetGeometry.conflictBottom <= sheetGeometry.actionTop + 1, JSON.stringify(sheetGeometry));
  check("encoding requires scrolling", sheetGeometry.encodingTop >= sheetGeometry.actionTop - 1, JSON.stringify(sheetGeometry));
  const destLabels = await js("[...document.querySelectorAll('#sheetContent [data-dest-mode]')].map(b=>b.textContent.replace(/\\s+/g,' ').trim())");
  check(
    "three destination options",
    destLabels.length === 3 && destLabels.some((label) => label.includes("当前文件夹")) && destLabels.some((label) => label.includes("新建同名文件夹")) && destLabels.some((label) => label.includes("选择其他位置")),
    destLabels.join(" | ")
  );
  check("new folder selected by default", (await js("document.querySelector('[data-dest-mode=\"new-folder\"]').classList.contains('active')")) === true);
  check("same password hidden for one archive", (await js("document.getElementById('samePasswordToggle') === null")) === true);
  const newFolderDetail = await js("document.querySelector('[data-dest-mode=\"new-folder\"] small').textContent.trim()");
  check("same-name folder uses archive location", newFolderDetail.includes("Download") && newFolderDetail.includes("sample"), newFolderDetail);
  const customDetail = await js("document.querySelector('[data-dest-mode=\"custom\"] small').textContent.trim()");
  check("custom option shows actual path", customDetail === "内部存储 / Download", customDetail);
  check("last clipboard shown", (await js("document.getElementById('lastClipboardPreview').textContent.trim() === 'Unzip@2026'")) === true);
  await js("document.getElementById('extractClipboardButton').click()");
  await sleep(300);
  check(
    "clipboard panel opens",
    (await js("!document.getElementById('extractClipboardPanel').hidden && document.getElementById('extractClipboardButton').getAttribute('aria-expanded') === 'true' && document.querySelectorAll('#extractClipboardItems .clipboard-item').length >= 3")) === true
  );
  await js("document.querySelector('[data-clipboard-value=\"rar-password-8899\"]').click()");
  await sleep(250);
  check(
    "clipboard fills password",
    (await js("document.getElementById('extractPassword').value === 'rar-password-8899' && document.getElementById('lastClipboardPreview').textContent === 'rar-password-8899' && document.getElementById('extractClipboardPanel').hidden && document.getElementById('extractClipboardButton').getAttribute('aria-expanded') === 'false'")) === true
  );
  await js("document.querySelector('[data-dest-mode=\"custom\"]').click()");
  await sleep(400);
  check("folder picker opens", (await js("document.getElementById('folderPicker').classList.contains('open')")) === true);
  check("folder picker starts at archive folder", (await js("document.getElementById('folderPickerPath').textContent.includes('Download')")) === true);
  await js("document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}))");
  await sleep(350);
  check(
    "escape goes up one folder",
    (await js("document.getElementById('folderPicker').classList.contains('open') && document.getElementById('folderPickerTitle').textContent.trim() === '内部存储'")) === true
  );
  await js("document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}))");
  await sleep(350);
  check(
    "escape closes picker at root, keeps sheet",
    (await js("!document.getElementById('folderPicker').classList.contains('open') && !document.getElementById('sheetLayer').hidden")) === true
  );
  await js("document.querySelector('[data-dest-mode=\"custom\"]').click()");
  await sleep(400);
  check("folder picker reopens", (await js("document.getElementById('folderPicker').classList.contains('open')")) === true);
  await js("document.querySelector('[data-picker-volume=\"sd\"]').click()");
  await sleep(200);
  check("folder picker reaches SD card", (await js("document.getElementById('folderPickerTitle').textContent.trim() === 'SD 卡'")) === true);
  await js("document.querySelector('[data-picker-volume=\"internal\"]').click()");
  await sleep(200);
  await js("document.querySelector('[data-picker-dir=\"download\"]').click()");
  await sleep(200);
  await js("document.querySelector('[data-picker-dir=\"materials\"]').click()");
  await sleep(200);
  check("folder picker navigates arbitrary folders", (await js("document.getElementById('folderPickerPath').textContent.includes('素材')")) === true);
  await js("document.getElementById('folderPickerSelect').click()");
  await sleep(400);
  check(
    "custom folder is saved",
    (await js("!document.getElementById('folderPicker').classList.contains('open') && document.querySelector('[data-dest-mode=\"custom\"] small').textContent.includes('素材')")) === true
  );
  await js("document.querySelector('[data-dest-mode=\"new-folder\"]').click()");
  await sleep(200);
  await js("document.querySelector('[data-sheet-action=\"start-extract\"]').click()");
  await sleep(600);
  check("task drawer appears", (await js("document.getElementById('taskDrawer').hidden")) === false);
  const taskTarget = await js("document.getElementById('currentTaskRight').textContent.trim()");
  check("new folder destination used", taskTarget.includes("Download") && taskTarget.includes("sample"), taskTarget);
  const s1 = await js("document.getElementById('currentTaskState').textContent.trim()");
  await js("document.getElementById('currentTaskToggle').click()");
  await sleep(300);
  const s2 = await js("document.getElementById('currentTaskState').textContent.trim()");
  check("pause/resume works", s1 !== s2, `${s1} -> ${s2}`);
  await js("document.getElementById('currentTaskToggle').click()");
  await sleep(5200);
  const s3 = await js("document.getElementById('currentTaskState').textContent.trim()");
  check("extraction completes", s3.includes("完成"), s3);
  await js("document.getElementById('drawerClose')?.click()");
  await sleep(300);

  // Encrypted archive
  for (let i = 0; i < 6; i++) {
    const p = await js("document.getElementById('folderTitle').textContent.trim()");
    if (p === "Download") break;
    await js("document.getElementById('topBackButton').click()");
    await sleep(350);
  }
  const extractedNames = await js("[...document.querySelectorAll('#fileList .file-name')].map(e=>e.textContent.trim())");
  check("extracted folder materialized", extractedNames.includes("sample"), extractedNames.join(","));
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

  // Copy uses the full folder picker
  for (let i = 0; i < 4; i++) {
    const p = await js("document.getElementById('folderTitle').textContent.trim()");
    if (p === "Download") break;
    await js("document.getElementById('topBackButton').click()");
    await sleep(350);
  }
  await longPressRow("readmeTxt");
  await js("document.getElementById('defaultCopy').click()");
  await sleep(400);
  check(
    "copy opens full folder picker",
    (await js("document.getElementById('folderPicker').classList.contains('open') && document.getElementById('folderPickerSelect').textContent.trim() === '复制到这里'")) === true
  );
  await js("document.querySelector('[data-picker-volume=\"internal\"]').click()");
  await sleep(200);
  await js("document.querySelector('[data-picker-dir=\"documents\"]').click()");
  await sleep(250);
  await js("document.getElementById('folderPickerSelect').click()");
  await sleep(500);
  check("copy closes picker and selection", (await js("!document.getElementById('folderPicker').classList.contains('open') && document.getElementById('selectionAppbar').hidden")) === true);
  await js("document.getElementById('storageSwitchButton').click()");
  await sleep(200);
  await js("document.querySelector('[data-location=\"documents\"]').click()");
  await sleep(450);
  const documentNames = await js("[...document.querySelectorAll('#fileList .file-name')].map(e=>e.textContent.trim())");
  check("copied file appears in Documents", documentNames.includes("下载说明 副本.txt"), documentNames.join(","));
  await js("document.getElementById('gestureHomeButton').click()");
  await sleep(400);

  // Settings + theme
  for (let i = 0; i < 8; i++) {
    const p = await js("document.getElementById('folderTitle').textContent.trim()");
    if (p === "内部存储") break;
    await js("document.getElementById('topBackButton').click()");
    await sleep(350);
  }
  await js("document.getElementById('moreButton').click()");
  await sleep(250);
  await js("document.querySelector('[data-more-action=\"settings\"]').click()");
  await sleep(450);
  check("settings opens", (await js("document.getElementById('settingsView').classList.contains('open')")) === true);
  await js("document.getElementById('themeSwitch').click()");
  await sleep(300);
  check("dark mode toggles", (await js("document.documentElement.dataset.theme")) === "dark");

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
