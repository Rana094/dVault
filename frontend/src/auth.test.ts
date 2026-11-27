import { beforeEach, expect, it, vi } from "vitest";
import { currentSession, login, logout, register } from "./auth";
import { refreshCsrf, request } from "./api";
vi.mock("./api", () => ({ refreshCsrf: vi.fn(), request: vi.fn() }));
beforeEach(() => vi.clearAllMocks());
const credentials = {
  email: "alice@example.com",
  password: "test-only-password",
};
it("refreshes CSRF after successful session login", async () => {
  const user = { id: 1, email: credentials.email, roles: ["USER"] };
  vi.mocked(request).mockResolvedValue(user);
  expect(await login(credentials)).toEqual(user);
  expect(request).toHaveBeenCalledWith(
    "/api/auth/login",
    expect.objectContaining({
      method: "POST",
      body: JSON.stringify(credentials),
    }),
  );
  expect(refreshCsrf).toHaveBeenCalledOnce();
  expect(vi.mocked(request).mock.invocationCallOrder[0]).toBeLessThan(
    vi.mocked(refreshCsrf).mock.invocationCallOrder[0],
  );
});
it("refreshes CSRF after logout and does not store authentication tokens", async () => {
  vi.mocked(request).mockResolvedValue(undefined);
  await logout();
  expect(request).toHaveBeenCalledWith("/api/auth/logout", { method: "POST" });
  expect(refreshCsrf).toHaveBeenCalledOnce();
});
it("does not refresh CSRF or retry when credentials are refused", async () => {
  vi.mocked(request).mockRejectedValueOnce(
    new Error("Invalid email or password"),
  );
  await expect(login(credentials)).rejects.toThrow("Invalid email or password");
  expect(refreshCsrf).not.toHaveBeenCalled();
  expect(request).toHaveBeenCalledTimes(1);
});
it("registers without auto-login and exposes session lookup", async () => {
  vi.mocked(request).mockResolvedValue({ id: 1 });
  await register(credentials);
  expect(request).toHaveBeenCalledWith(
    "/api/auth/register",
    expect.objectContaining({ method: "POST" }),
  );
  expect(refreshCsrf).not.toHaveBeenCalled();
  await currentSession();
  expect(request).toHaveBeenLastCalledWith("/api/auth/session");
});
