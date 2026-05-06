import { Router } from "express";
import type { Model } from "mongoose";

export function createCrudRouter(model: Model<any>, searchFields: string[] = []) {
  const router = Router();

  router.get("/", async (req, res, next) => {
    try {
      const page = Math.max(Number(req.query.page ?? 1), 1);
      const limit = Math.min(Math.max(Number(req.query.limit ?? 20), 1), 500);
      const q = String(req.query.q ?? "").trim();
      const filter = q
        ? {
            $or: searchFields.map((field) => ({
              [field]: { $regex: q, $options: "i" }
            }))
          }
        : {};

      const [items, total] = await Promise.all([
        model.find(filter).sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit).lean(),
        model.countDocuments(filter)
      ]);

      res.json({ items, total, page, limit });
    } catch (error) {
      next(error);
    }
  });

  router.get("/:id", async (req, res, next) => {
    try {
      const item = await model.findById(req.params.id).lean();
      if (!item) {
        res.status(404).json({ message: "Resource not found" });
        return;
      }
      res.json(item);
    } catch (error) {
      next(error);
    }
  });

  router.post("/", async (req, res, next) => {
    try {
      const item = await model.create({ doctor_verified: false, ...req.body });
      res.status(201).json(item);
    } catch (error) {
      next(error);
    }
  });

  router.put("/:id", async (req, res, next) => {
    try {
      const item = await model.findByIdAndUpdate(req.params.id, req.body, {
        new: true,
        runValidators: true
      });
      if (!item) {
        res.status(404).json({ message: "Resource not found" });
        return;
      }
      res.json(item);
    } catch (error) {
      next(error);
    }
  });

  router.delete("/:id", async (req, res, next) => {
    try {
      const item = await model.findByIdAndDelete(req.params.id);
      if (!item) {
        res.status(404).json({ message: "Resource not found" });
        return;
      }
      res.status(204).send();
    } catch (error) {
      next(error);
    }
  });

  return router;
}
