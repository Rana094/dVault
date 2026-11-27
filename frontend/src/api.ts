export type ApiErrorBody = {
  status: number;
  code: string;
  message: string;
  path: string;
  timestamp: string;
  fieldErrors?: Record<string, string>;
};
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public details?: ApiErrorBody,
  ) {
    super(message);
  }
}
type CsrfToken = { headerName: string; token: string };
let csrf: CsrfToken | undefined;
// Authentication flows must refresh this token after successful login/logout.
export async function refreshCsrf(): Promise<void> {
  csrf = undefined;
  const response = await fetch("/api/auth/csrf", {
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok)
    throw new ApiError(
      response.status,
      "Could not establish a secure session.",
    );
  csrf = (await response.json()) as CsrfToken;
}
export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  if (!path.startsWith("/api/") || path.includes("://"))
    throw new Error("API requests must use a same-origin /api/ path.");
  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    if (!csrf) await refreshCsrf();
    headers.set(csrf!.headerName, csrf!.token);
  }
  const response = await fetch(path, {
    ...options,
    method,
    headers,
    credentials: "same-origin",
  });
  if (!response.ok) {
    const details = (await response.json().catch(() => undefined)) as
      | ApiErrorBody
      | undefined;
    throw new ApiError(
      response.status,
      details?.message ?? "The request could not be completed.",
      details,
    );
  }
  return response.status === 204
    ? (undefined as T)
    : (response.json() as Promise<T>);
}
