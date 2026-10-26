// Read-only inspection of the local Open Design SQLite database.
const Database = require("C:\\Open Design\\resources\\app\\node_modules\\better-sqlite3");

const dbPath = process.argv[2];
if (!dbPath) {
  console.error("usage: inspect-od-db.cjs <app.sqlite>");
  process.exit(2);
}

const db = new Database(dbPath, { readonly: true, fileMustExist: true });
const tables = db
  .prepare("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name")
  .all()
  .map((row) => row.name);

console.log("TABLES: " + tables.join(", "));

for (const table of tables) {
  if (!/project|workspace|^tabs/i.test(table)) continue;
  console.log("\n== " + table);
  try {
    const cols = db.prepare(`PRAGMA table_info("${table}")`).all().map((c) => c.name);
    console.log("COLUMNS: " + cols.join(", "));
    const rows = db.prepare(`SELECT * FROM "${table}" LIMIT 20`).all();
    console.log("ROWS: " + rows.length);
    console.log(JSON.stringify(rows, null, 1).slice(0, 6000));
  } catch (err) {
    console.log("ERR: " + err.message);
  }
}
