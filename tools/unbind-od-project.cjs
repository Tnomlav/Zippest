const Database = require("C:\\Open Design\\resources\\app\\node_modules\\better-sqlite3");

const [dbPath, projectId] = process.argv.slice(2);
if (!dbPath || !projectId) {
  console.error("usage: unbind-od-project.cjs <app.sqlite> <project-id>");
  process.exit(2);
}

const db = new Database(dbPath);
const before = db
  .prepare("SELECT * FROM workspace_projects WHERE project_id = ?")
  .all(projectId);
const result = db
  .prepare("DELETE FROM workspace_projects WHERE project_id = ?")
  .run(projectId);

console.log(JSON.stringify({ projectId, deleted: result.changes, before }, null, 1));
db.close();
