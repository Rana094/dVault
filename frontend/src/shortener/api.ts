import { request } from "../api";
export type ShortUrl = {
  id: number;
  alias: string;
  targetUrl: string;
  expiresAt: string | null;
  active: boolean;
};
export type UrlInput = Omit<ShortUrl, "id">;
export type UrlPage = {
  content: ShortUrl[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
export const listUrls = (page: number) =>
  request<UrlPage>(`/api/urls?page=${page}&size=10`);
export const getUrl = (id: number) => request<ShortUrl>(`/api/urls/${id}`);
export const saveUrl = (input: UrlInput, id?: number) =>
  request<ShortUrl>(id ? `/api/urls/${id}` : "/api/urls", {
    method: id ? "PUT" : "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
export const deleteUrl = (id: number) =>
  request<void>(`/api/urls/${id}`, { method: "DELETE" });
