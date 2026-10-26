const Database = require("C:\\Open Design\\resources\\app\\node_modules\\better-sqlite3");
const db = new Database(process.argv[2], { readonly: true, fileMustExist: true });
for (const table of ["tabs", "tabs_state"]) {
  console.log("== " + table);
  console.log(JSON.stringify(db.prepare(`SELECT * FROM "${table}"`).all(), null, 1));
}
