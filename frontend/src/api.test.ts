import { beforeEach, expect, it, vi } from "vitest";
import { ApiError, refreshCsrf, request } from "./api";
const fetchMock = vi.fn();
beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
});
it("attaches session CSRF token to a mutation and handles 204", async () => {
  fetchMock.mockResolvedValueOnce(
    new Response(
      JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "first" }),
    ),
  );
  await refreshCsrf();
  fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
  expect(await request("/api/files/1", { method: "DELETE" })).toBeUndefined();
  const [path, options] = fetchMock.mock.calls[1];
  expect(path).toBe("/api/files/1");
  expect(options.headers.get("X-CSRF-TOKEN")).toBe("first");
  expect(options.credentials).toBe("same-origin");
});
it("refreshes CSRF after the session changes", async () => {
  fetchMock.mockResolvedValueOnce(
    new Response(
      JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "new-session" }),
    ),
  );
  await refreshCsrf();
  fetchMock.mockResolvedValueOnce(new Response("{}"));
  await request("/api/example", { method: "POST" });
  expect(fetchMock.mock.calls[1][1].headers.get("X-CSRF-TOKEN")).toBe(
    "new-session",
  );
});
it("preserves structured API errors", async () => {
  fetchMock.mockResolvedValueOnce(
    new Response(
      JSON.stringify({
        status: 409,
        code: "CONFLICT",
        message: "Folder is not empty",
      }),
      { status: 409 },
    ),
  );
  await expect(request("/api/folders/1")).rejects.toMatchObject({
    status: 409,
    message: "Folder is not empty",
    details: { code: "CONFLICT" },
  });
});
it("handles non-JSON server failures", async () => {
  fetchMock.mockResolvedValueOnce(new Response("Unavailable", { status: 503 }));
  await expect(request("/api/health")).rejects.toBeInstanceOf(ApiError);
});
it("rejects external and non-API paths before sending", async () => {
  await expect(request("https://example.com")).rejects.toThrow("same-origin");
  await expect(request("//example.com/api/")).rejects.toThrow("same-origin");
  expect(fetchMock).not.toHaveBeenCalled();
});
