import { refreshCsrf, request } from "./api";

export type Credentials = { email: string; password: string };
export type SessionUser = {
  id: number;
  email: string;
  roles: ("USER" | "ADMIN")[];
};

export function register(credentials: Credentials): Promise<SessionUser> {
  return request("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });
}

export async function login(credentials: Credentials): Promise<SessionUser> {
  const user = await request<SessionUser>("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });
  await refreshCsrf();
  return user;
}

export async function logout(): Promise<void> {
  await request<void>("/api/auth/logout", { method: "POST" });
  await refreshCsrf();
}

export function currentSession(): Promise<SessionUser> {
  return request("/api/auth/session");
}
