import type { NextFunction, Request, Response } from "express";
import { AppError } from "../utils/errors.js";
import logger from "../utils/logger.js";
import { sendError } from "../utils/response.js";

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Unexpected error";
}

export function errorHandler(error: unknown, req: Request, res: Response, _next: NextFunction) {
  const appError = error instanceof AppError ? error : null;
  const statusCode = appError?.statusCode || 500;
  const code = appError?.code || "INTERNAL_ERROR";
  const message = appError?.message || errorMessage(error);

  logger.api.error(req.method, req.path, message, {
    statusCode,
    code,
    stack: error instanceof Error ? error.stack : undefined
  });

  sendError(res, code, message, statusCode, appError?.details);
}
