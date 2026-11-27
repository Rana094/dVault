import { afterEach, beforeEach, expect, it, vi } from "vitest";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { ShortLinks } from "./ShortLinks";
import { ApiError } from "../api";
import { deleteUrl, getUrl, listUrls, saveUrl } from "./api";
vi.mock("./api", () => ({
  listUrls: vi.fn(),
  getUrl: vi.fn(),
  saveUrl: vi.fn(),
  deleteUrl: vi.fn(),
}));
const url = {
  id: 1,
  alias: "my-link",
  targetUrl: "https://example.com",
  expiresAt: null,
  active: true,
};
const page = {
  content: [url],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
};
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(listUrls).mockResolvedValue(page);
  Object.defineProperty(HTMLDialogElement.prototype, "showModal", {
    configurable: true,
    value: function (this: HTMLDialogElement) {
      this.open = true;
    },
  });
  Object.defineProperty(HTMLDialogElement.prototype, "close", {
    configurable: true,
    value: function (this: HTMLDialogElement) {
      this.open = false;
    },
  });
});
afterEach(cleanup);
it("creates a link and reports the saved alias", async () => {
  vi.mocked(saveUrl).mockResolvedValue(url);
  render(<ShortLinks />);
  await screen.findByText("/u/my-link");
  fireEvent.change(screen.getByLabelText("Destination URL"), {
    target: { value: "https://example.com" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Create link" }));
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Saved /u/my-link",
  );
  expect(saveUrl).toHaveBeenCalledWith(
    {
      targetUrl: "https://example.com",
      alias: "",
      expiresAt: null,
      active: true,
    },
    undefined,
  );
});
it("loads details then saves an owner edit", async () => {
  vi.mocked(getUrl).mockResolvedValue(url);
  vi.mocked(saveUrl).mockResolvedValue({
    ...url,
    targetUrl: "https://example.org",
  });
  render(<ShortLinks />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Details for my-link" }),
  );
  await screen.findByRole("heading", { name: "Link details" });
  fireEvent.change(screen.getByLabelText("Destination URL"), {
    target: { value: "https://example.org" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  await screen.findByText("Saved /u/my-link");
  expect(getUrl).toHaveBeenCalledWith(1);
  expect(saveUrl).toHaveBeenCalledWith(
    expect.objectContaining({ targetUrl: "https://example.org" }),
    1,
  );
});
it("confirms deletion and refreshes the collection", async () => {
  vi.mocked(deleteUrl).mockResolvedValue(undefined);
  render(<ShortLinks />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Delete my-link" }),
  );
  const dialog = screen.getByRole("dialog", { name: "Delete short link?" });
  fireEvent.click(
    within(dialog).getByRole("button", { name: "Confirm delete" }),
  );
  expect(await screen.findByText("Short link deleted.")).toBeInTheDocument();
  expect(deleteUrl).toHaveBeenCalledWith(1);
  expect(listUrls).toHaveBeenCalledTimes(2);
});
it("shows alias conflicts without discarding entered values", async () => {
  vi.mocked(saveUrl).mockRejectedValue(
    new ApiError(409, "That alias is already in use"),
  );
  render(<ShortLinks />);
  await screen.findByText("/u/my-link");
  fireEvent.change(screen.getByLabelText("Destination URL"), {
    target: { value: "https://example.com" },
  });
  fireEvent.change(screen.getByLabelText("Alias (optional)"), {
    target: { value: "my-link" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Create link" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "That alias is already in use",
  );
  expect(screen.getByLabelText("Alias (optional)")).toHaveValue("my-link");
});
it("shows session-required and empty states", async () => {
  vi.mocked(listUrls).mockRejectedValueOnce(
    new ApiError(401, "Authentication required"),
  );
  render(<ShortLinks />);
  await screen.findByRole("heading", {
    name: "Sign in to manage your short links",
  });
  vi.mocked(listUrls).mockResolvedValueOnce({
    ...page,
    content: [],
    totalElements: 0,
    totalPages: 0,
  });
  fireEvent.click(screen.getByRole("button", { name: "Check session again" }));
  expect(
    await screen.findByText("No short links yet. Create your first one above."),
  ).toBeInTheDocument();
});
it("requests the next page without changing ownership parameters", async () => {
  vi.mocked(listUrls).mockResolvedValue({
    ...page,
    totalPages: 2,
    totalElements: 11,
  });
  render(<ShortLinks />);
  fireEvent.click(await screen.findByRole("button", { name: "Next" }));
  await screen.findByText("Page 2 of 2");
  expect(listUrls).toHaveBeenLastCalledWith(1);
});
