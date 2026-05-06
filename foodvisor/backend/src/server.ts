import cors from "cors";
import "dotenv/config";
import express from "express";
import morgan from "morgan";
import path from "path";
import { connectDatabase } from "./db.js";
import { migrateFoodDictionary } from "./migrations/foodDictionaryMigration.js";
import { apiRouter } from "./routes/index.js";
import { openApiDocument, swaggerHtml } from "./swagger.js";

const app = express();
const port = Number(process.env.PORT ?? 4000);
const corsOrigins = (process.env.CORS_ORIGIN ?? "http://localhost:3000,http://localhost:3001")
  .split(",")
  .map((origin) => origin.trim())
  .filter(Boolean);

app.use(cors({
  origin(origin, callback) {
    if (!origin || corsOrigins.includes("*") || corsOrigins.includes(origin)) {
      callback(null, true);
      return;
    }
    callback(new Error(`CORS origin not allowed: ${origin}`));
  }
}));
app.use(express.json({ limit: "5mb" }));
app.use(morgan("dev"));
app.use("/images", express.static(path.join(process.cwd(), "public", "images")));
app.get("/api-docs.json", (_req, res) => res.json(openApiDocument));
app.get("/api-docs", (_req, res) => res.type("html").send(swaggerHtml("Foodvisor API Docs", "/api-docs.json")));
app.use("/api", apiRouter);

app.use((err: Error, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  res.status(400).json({ message: err.message });
});

await connectDatabase();
const migrationResult = await migrateFoodDictionary({ skipFoods: true });

app.listen(port, () => {
  console.log(`Foodvisor API listening on http://localhost:${port}`);
  console.log(`Foodvisor API docs available at http://localhost:${port}/api-docs`);
  console.log(`Food dictionary migration checked ${migrationResult.foodCount} foods and ${migrationResult.profileCount} daily value profiles.`);
});
