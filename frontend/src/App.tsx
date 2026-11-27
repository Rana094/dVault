import { useEffect, useState } from "react";
import { request } from "./api";
import { Dialog, ErrorNotice, Loading } from "./components";
const sections = ["Overview", "My vault", "Shares", "Short links"] as const;
export function App() {
  const [section, setSection] = useState<string>("Overview");
  const [health, setHealth] = useState<"loading" | "UP" | "error">("loading");
  const [about, setAbout] = useState(false);
  async function checkHealth() {
    setHealth("loading");
    try {
      const result = await request<{ status: string }>("/api/health");
      setHealth(result.status === "UP" ? "UP" : "error");
    } catch {
      setHealth("error");
    }
  }
  useEffect(() => {
    void checkHealth();
  }, []);
  return (
    <div className="layout">
      <a className="skip" href="#content">
        Skip to content
      </a>
      <aside>
        <a className="brand" href="/">
          dVault<span>Your private workspace</span>
        </a>
        <nav aria-label="Main navigation">
          {sections.map((name) => (
            <button
              key={name}
              aria-current={section === name ? "page" : undefined}
              onClick={() => setSection(name)}
            >
              {name}
            </button>
          ))}
        </nav>
        <button className="about" onClick={() => setAbout(true)}>
          About dVault
        </button>
      </aside>
      <main id="content">
        <header>
          <span>PERSONAL WORKSPACE</span>
          <p>Storage. Sharing. Simpler.</p>
        </header>
        <section className="hero">
          <p className="eyebrow">A place for what matters</p>
          <h1>{section === "Overview" ? "Welcome to dVault" : section}</h1>
          <p>Keep your files organized and share them with confidence.</p>
        </section>
        <section className="panel" aria-labelledby="status-title">
          <h2 id="status-title">Workspace status</h2>
          {health === "loading" ? (
            <Loading>Connecting to your workspace…</Loading>
          ) : health === "error" ? (
            <ErrorNotice
              message="We could not reach the server."
              retry={() => void checkHealth()}
            />
          ) : (
            <p className="connected">● Server connected</p>
          )}
          <p>
            The workspace foundation is ready. Accounts, file storage, sharing
            and short links will become available as their modules are released.
          </p>
        </section>
        <footer>Private by design. Built one step at a time.</footer>
      </main>
      <Dialog open={about} title="About dVault" onClose={() => setAbout(false)}>
        <p>A private workspace for files, secure sharing and short links.</p>
      </Dialog>
    </div>
  );
}
