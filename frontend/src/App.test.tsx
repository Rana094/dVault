import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { App } from "./App";
import { Field } from "./components";
const fetchMock = vi.fn();
beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
});
afterEach(cleanup);
it("renders the responsive shell and real health response", async () => {
  fetchMock.mockResolvedValue(new Response('{"status":"UP"}'));
  render(<App />);
  expect(
    screen.getByRole("navigation", { name: "Main navigation" }),
  ).toBeInTheDocument();
  expect(await screen.findByText("● Server connected")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "My vault" }));
  expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
    "My vault",
  );
});
it("shows a retryable error and recovers", async () => {
  fetchMock
    .mockRejectedValueOnce(new Error("offline"))
    .mockResolvedValueOnce(new Response('{"status":"UP"}'));
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "We could not reach the server.",
  );
  fireEvent.click(screen.getByRole("button", { name: "Try again" }));
  expect(await screen.findByText("● Server connected")).toBeInTheDocument();
});
it("associates field labels and validation messages", () => {
  render(<Field id="email" label="Email" error="Enter a valid email" />);
  expect(screen.getByLabelText("Email")).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  expect(screen.getByLabelText("Email")).toHaveAccessibleDescription(
    "Enter a valid email",
  );
});
