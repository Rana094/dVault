import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { ApiError } from "../api";
import { Dialog, ErrorNotice, Field, Loading } from "../components";
import {
  deleteUrl,
  getUrl,
  listUrls,
  saveUrl,
  type ShortUrl,
  type UrlPage,
} from "./api";
import "./short-links.css";

export function ShortLinks() {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<UrlPage>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [signedOut, setSignedOut] = useState(false);
  const [editing, setEditing] = useState<ShortUrl>();
  const [deleting, setDeleting] = useState<ShortUrl>();
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [formError, setFormError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const form = useRef<HTMLFormElement>(null);
  const target = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    setSignedOut(false);
    try {
      setResult(await listUrls(page));
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) setSignedOut(true);
      else
        setError(
          error instanceof Error
            ? error.message
            : "Could not load short links.",
        );
    } finally {
      setLoading(false);
    }
  }, [page]);
  useEffect(() => {
    void load();
  }, [load]);
  useEffect(() => {
    if (editing) target.current?.focus();
  }, [editing]);

  function reset() {
    setEditing(undefined);
    form.current?.reset();
    setFormError("");
    setFieldErrors({});
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const expires = data.get("expiresAt") as string;
    setBusy(true);
    setFormError("");
    setFieldErrors({});
    setMessage("");
    try {
      const saved = await saveUrl(
        {
          targetUrl: data.get("targetUrl") as string,
          alias: data.get("alias") as string,
          expiresAt: expires ? new Date(expires).toISOString() : null,
          active: data.get("active") === "on",
        },
        editing?.id,
      );
      reset();
      setMessage(`Saved /u/${saved.alias}`);
      await load();
    } catch (error) {
      setFormError(
        error instanceof Error ? error.message : "Could not save this link.",
      );
      if (error instanceof ApiError)
        setFieldErrors(error.details?.fieldErrors ?? {});
    } finally {
      setBusy(false);
    }
  }
  async function details(id: number) {
    setBusy(true);
    setFormError("");
    setFieldErrors({});
    try {
      setEditing(await getUrl(id));
    } catch (error) {
      setFormError(
        error instanceof Error ? error.message : "Could not open this link.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function remove() {
    if (!deleting) return;
    setBusy(true);
    setFormError("");
    setMessage("");
    try {
      await deleteUrl(deleting.id);
      setDeleting(undefined);
      reset();
      setMessage("Short link deleted.");
      if (result?.content.length === 1 && page > 0) setPage(page - 1);
      else await load();
    } catch (error) {
      setFormError(
        error instanceof Error ? error.message : "Could not delete this link.",
      );
    } finally {
      setBusy(false);
    }
  }
  function localDate(instant: string | null) {
    if (!instant) return "";
    const date = new Date(instant);
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);
  }

  if (signedOut)
    return (
      <section className="panel">
        <h2>Sign in to manage your short links</h2>
        <p>Your links are private to your account.</p>
        <button onClick={() => void load()}>Check session again</button>
      </section>
    );
  return (
    <div className="short-links">
      <section className="panel" aria-labelledby="url-form-title">
        <h2 id="url-form-title">
          {editing ? "Link details" : "Create short link"}
        </h2>
        <p>
          Choose an alias or leave it blank to generate one. Redirects aren’t
          available yet.
        </p>
        {formError && <ErrorNotice message={formError} />}
        <form
          ref={form}
          key={editing?.id ?? "new"}
          onSubmit={(event) => void submit(event)}
        >
          <div className="field">
            <label htmlFor="url-target">Destination URL</label>
            <input
              ref={target}
              id="url-target"
              name="targetUrl"
              type="url"
              required
              maxLength={2048}
              defaultValue={editing?.targetUrl ?? ""}
              placeholder="https://example.com"
              aria-invalid={!!fieldErrors.targetUrl}
              aria-describedby={
                fieldErrors.targetUrl ? "url-target-error" : undefined
              }
            />
            {fieldErrors.targetUrl && (
              <span id="url-target-error">{fieldErrors.targetUrl}</span>
            )}
          </div>
          <Field
            id="url-alias"
            name="alias"
            label="Alias (optional)"
            maxLength={32}
            defaultValue={editing?.alias ?? ""}
            error={fieldErrors.alias}
          />
          <Field
            id="url-expiry"
            name="expiresAt"
            label="Expires at (optional, local time)"
            type="datetime-local"
            defaultValue={localDate(editing?.expiresAt ?? null)}
            error={fieldErrors.expiresAt}
          />
          <label className="url-active">
            <input
              name="active"
              type="checkbox"
              defaultChecked={editing?.active ?? true}
            />{" "}
            Active
          </label>
          <div className="url-actions">
            <button type="submit" disabled={busy}>
              {busy ? "Please wait…" : editing ? "Save changes" : "Create link"}
            </button>
            {editing && (
              <button type="button" disabled={busy} onClick={reset}>
                Cancel edit
              </button>
            )}
          </div>
        </form>
      </section>
      {message && <p role="status">{message}</p>}
      <section className="panel" aria-labelledby="url-list-title">
        <h2 id="url-list-title">Your short links</h2>
        {loading ? (
          <Loading>Loading your links…</Loading>
        ) : error ? (
          <ErrorNotice message={error} retry={() => void load()} />
        ) : result?.content.length ? (
          <>
            <ul className="url-list">
              {result.content.map((url) => (
                <li key={url.id}>
                  <div>
                    <strong>/u/{url.alias}</strong>
                    <a href={url.targetUrl} target="_blank" rel="noreferrer">
                      {url.targetUrl}
                    </a>
                    <small>
                      {url.active ? "Active" : "Inactive"}
                      {url.expiresAt
                        ? ` · Expires ${new Date(url.expiresAt).toLocaleString()}`
                        : " · No expiry"}
                    </small>
                  </div>
                  <div className="url-actions">
                    <button
                      disabled={busy}
                      onClick={() => void details(url.id)}
                      aria-label={`Details for ${url.alias}`}
                    >
                      Details
                    </button>
                    <button
                      disabled={busy}
                      onClick={() => setDeleting(url)}
                      aria-label={`Delete ${url.alias}`}
                    >
                      Delete
                    </button>
                  </div>
                </li>
              ))}
            </ul>
            <div className="url-pagination">
              <button
                disabled={page === 0 || busy}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </button>
              <span>
                Page {page + 1} of {result.totalPages}
              </span>
              <button
                disabled={page + 1 >= result.totalPages || busy}
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </div>
          </>
        ) : (
          <p>No short links yet. Create your first one above.</p>
        )}
      </section>
      <Dialog
        open={!!deleting}
        title="Delete short link?"
        onClose={() => {
          setDeleting(undefined);
        }}
      >
        <p>Delete /u/{deleting?.alias}? This cannot be undone.</p>
        <button disabled={busy} onClick={() => void remove()}>
          Confirm delete
        </button>
      </Dialog>
    </div>
  );
}
