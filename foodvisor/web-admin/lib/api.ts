export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:4000/api";

export type FieldType = "text" | "number" | "textarea" | "select" | "date" | "tags" | "boolean";

export type ResourceField = {
  name: string;
  label: string;
  type: FieldType;
  options?: string[];
  required?: boolean;
};

export type ResourceColumn = {
  key: string;
  label: string;
  kind?: "image" | "boolean" | "date";
};

type ApiEnvelope<T> = {
  success?: boolean;
  data?: T;
  error?: {
    code?: string;
    message?: string;
    details?: unknown;
  };
  meta?: Record<string, unknown>;
};

function isEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  return Boolean(value && typeof value === "object" && "success" in value);
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    },
    cache: "no-store"
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error?.message ?? body.message ?? `Request failed: ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const body = await response.json();
  if (isEnvelope<T>(body)) {
    if (body.success === false) {
      throw new Error(body.error?.message ?? `Request failed: ${response.status}`);
    }
    return body.data as T;
  }

  return body as T;
}
