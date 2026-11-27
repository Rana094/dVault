import { useEffect, useRef, type ReactNode } from "react";
export function Loading({ children = "Loading…" }: { children?: ReactNode }) {
  return (
    <p role="status" aria-live="polite">
      {children}
    </p>
  );
}
export function ErrorNotice({
  message,
  retry,
}: {
  message: string;
  retry?: () => void;
}) {
  return (
    <div role="alert" className="notice">
      <p>{message}</p>
      {retry && <button onClick={retry}>Try again</button>}
    </div>
  );
}
export function Field({
  id,
  label,
  error,
  ...input
}: React.InputHTMLAttributes<HTMLInputElement> & {
  id: string;
  label: string;
  error?: string;
}) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input
        {...input}
        id={id}
        aria-invalid={!!error}
        aria-describedby={error ? `${id}-error` : undefined}
      />
      {error && <span id={`${id}-error`}>{error}</span>}
    </div>
  );
}
export function Dialog({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    if (open && !ref.current?.open) ref.current?.showModal();
    else if (!open && ref.current?.open) ref.current.close();
  }, [open]);
  return (
    <dialog ref={ref} aria-label={title} onCancel={onClose} onClose={onClose}>
      <h2>{title}</h2>
      {children}
      <button onClick={onClose}>Close</button>
    </dialog>
  );
}
