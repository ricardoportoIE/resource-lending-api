import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiError, api } from "./api";
import type {
  CurrentUser,
  DashboardSummary,
  Loan,
  Reservation,
  Resource,
  Utilization,
} from "./types";
import { formatDate, readable, staffAction } from "./utils";

type View = "catalogue" | "loans" | "reservations" | "operations" | "dashboard";
type Notice = { tone: "success" | "error"; message: string };

const storedToken = () => sessionStorage.getItem("accessToken") ?? "";

export default function App() {
  const [token, setToken] = useState(storedToken);
  const [user, setUser] = useState<CurrentUser>();
  const [view, setView] = useState<View>("catalogue");
  const [notice, setNotice] = useState<Notice>();
  const [busy, setBusy] = useState(false);

  const signOut = useCallback(() => {
    if (token) void api.logout(token, sessionStorage.getItem("refreshToken") ?? "");
    sessionStorage.clear();
    setToken("");
    setUser(undefined);
  }, [token]);

  useEffect(() => {
    if (!token) return;
    api.me(token).then(setUser).catch(signOut);
  }, [token, signOut]);

  const run = useCallback(
    async (operation: () => Promise<void>, success?: string) => {
      setBusy(true);
      setNotice(undefined);
      try {
        await operation();
        if (success) setNotice({ tone: "success", message: success });
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) signOut();
        setNotice({
          tone: "error",
          message: error instanceof Error ? error.message : "Unexpected error.",
        });
      } finally {
        setBusy(false);
      }
    },
    [signOut],
  );

  if (!token) return <Login onLogin={(next) => setToken(next)} />;
  if (!user) return <Splash label="Preparing your workspace" />;

  const isStaff = user.roles.some((role) => role === "STAFF" || role === "ADMIN");

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Brand />
        <nav aria-label="Primary navigation">
          <NavButton active={view === "catalogue"} onClick={() => setView("catalogue")} icon="⌘">
            Catalogue
          </NavButton>
          <NavButton active={view === "loans"} onClick={() => setView("loans")} icon="↗">
            My loans
          </NavButton>
          <NavButton active={view === "reservations"} onClick={() => setView("reservations")} icon="◷">
            Reservations
          </NavButton>
          {isStaff && (
            <>
              <span className="nav-caption">Operations</span>
              <NavButton active={view === "operations"} onClick={() => setView("operations")} icon="✓">
                Loan desk
              </NavButton>
              <NavButton active={view === "dashboard"} onClick={() => setView("dashboard")} icon="⌁">
                Dashboard
              </NavButton>
            </>
          )}
        </nav>
        <div className="profile-block">
          <div className="avatar">{user.email.slice(0, 2).toUpperCase()}</div>
          <div>
            <strong>{user.firstName || user.email.split("@")[0]}</strong>
            <span>{user.roles.join(" · ")}</span>
          </div>
          <button className="icon-button" onClick={signOut} aria-label="Sign out">↪</button>
        </div>
      </aside>

      <main>
        <div className="mobile-brand"><Brand /></div>
        {notice && <div className={`notice ${notice.tone}`}>{notice.message}</div>}
        {view === "catalogue" && <Catalogue token={token} run={run} />}
        {view === "loans" && <Loans token={token} ownUserId={user.id} />}
        {view === "reservations" && <Reservations token={token} run={run} />}
        {view === "operations" && <LoanDesk token={token} run={run} busy={busy} />}
        {view === "dashboard" && <Dashboard token={token} run={run} />}
      </main>
      {busy && <div className="progress" aria-label="Operation in progress" />}
    </div>
  );
}

function Login({ onLogin }: { onLogin: (token: string) => void }) {
  type AuthMode = "login" | "register" | "forgot" | "reset" | "confirm";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [identityToken, setIdentityToken] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [mode, setMode] = useState<AuthMode>("login");

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");
    try {
      if (mode === "register") {
        await api.register(email, password);
        setMode("confirm");
        setMessage("Account created. Enter the token delivered by the notification adapter.");
        return;
      }
      if (mode === "forgot") {
        await api.forgotPassword(email);
        setMode("reset");
        setMessage("If the account exists, a reset token has been sent.");
        return;
      }
      if (mode === "reset") {
        await api.resetPassword(identityToken, password);
        setMode("login");
        setMessage("Password updated. You can now sign in.");
        return;
      }
      if (mode === "confirm") {
        await api.confirmEmail(identityToken);
        setMode("login");
        setMessage("Email confirmed. You can now sign in.");
        return;
      }
      const tokens = await api.login(email, password);
      sessionStorage.setItem("accessToken", tokens.accessToken);
      sessionStorage.setItem("refreshToken", tokens.refreshToken);
      onLogin(tokens.accessToken);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to complete the request.");
    } finally {
      setLoading(false);
    }
  }

  const copy = {
    login: ["Welcome back", "Sign in to operations", "Use an account provisioned through the Resource Lending API."],
    register: ["Get started", "Create your account", "New accounts receive student access after email confirmation."],
    forgot: ["Account recovery", "Request a reset", "The response is identical whether or not the account exists."],
    reset: ["Account recovery", "Choose a new password", "Paste the one-time token delivered to your notification channel."],
    confirm: ["Verify identity", "Confirm your email", "Paste the one-time token delivered to your notification channel."],
  }[mode];

  return (
    <div className="login-layout">
      <section className="login-story">
        <Brand />
        <div className="story-copy">
          <p className="eyebrow">Shared assets, clearly managed</p>
          <h1>Make every resource count.</h1>
          <p>A calm operational space for equipment, rooms and the people waiting to use them.</p>
        </div>
        <div className="story-metric"><strong>One inventory.</strong> From request to return.</div>
      </section>
      <section className="login-panel">
        <form onSubmit={submit}>
          <p className="eyebrow">{copy[0]}</p>
          <h2>{copy[1]}</h2>
          <p className="muted">{copy[2]}</p>
          {["login", "register", "forgot"].includes(mode) && <label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoFocus /></label>}
          {["login", "register", "reset"].includes(mode) && <label>{mode === "reset" ? "New password" : "Password"}<input type="password" minLength={mode === "reset" ? 12 : 8} value={password} onChange={(event) => setPassword(event.target.value)} required /></label>}
          {["reset", "confirm"].includes(mode) && <label>One-time token<input value={identityToken} onChange={(event) => setIdentityToken(event.target.value)} required autoFocus /></label>}
          {message && <div className="form-success">{message}</div>}
          {error && <div className="form-error">{error}</div>}
          <button className="primary wide" disabled={loading}>{loading ? "Please wait…" : "Continue"}</button>
          <div className="auth-links">
            {mode !== "login" && <button type="button" onClick={() => setMode("login")}>Back to sign in</button>}
            {mode === "login" && <><button type="button" onClick={() => setMode("register")}>Create account</button><button type="button" onClick={() => setMode("forgot")}>Forgot password</button><button type="button" onClick={() => setMode("confirm")}>Confirm email</button></>}
          </div>
          <small>Tokens remain in this browser tab and are cleared when you sign out.</small>
        </form>
      </section>
    </div>
  );
}

function Catalogue({ token, run }: { token: string; run: Runner }) {
  const [resources, setResources] = useState<Resource[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const load = useCallback(() => api.resources(token).then((page) => setResources(page.content)).finally(() => setLoading(false)), [token]);
  useEffect(() => { void load(); }, [load]);
  const filtered = useMemo(() => resources.filter((resource) => `${resource.name} ${resource.category} ${resource.type}`.toLowerCase().includes(query.toLowerCase())), [resources, query]);

  return (
    <section>
      <PageHeader eyebrow="Discover" title="Resource catalogue" detail={`${resources.length} resource types in the shared inventory`} />
      <div className="toolbar"><input className="search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search equipment, spaces or books…" /></div>
      {loading ? <Skeleton /> : <div className="card-grid">
        {filtered.map((resource) => {
          const available = resource.items.filter((item) => item.status === "AVAILABLE");
          return <article className="resource-card" key={resource.id}>
            <div className="resource-top"><span className="type-mark">{resource.type.slice(0, 2)}</span><Status value={available.length ? "AVAILABLE" : "WAITLIST"} /></div>
            <div><span className="category">{resource.category}</span><h3>{resource.name}</h3><p>{resource.description || "Managed shared resource"}</p></div>
            <div className="inventory-line"><strong>{available.length}</strong><span>available of {resource.items.length}</span></div>
            <div className="card-actions">
              {available[0] && resource.loanable ? <button className="primary" onClick={() => run(async () => { await api.requestLoan(token, available[0].id); await load(); }, "Loan request created.")}>Request item</button> : <button className="primary" onClick={() => run(() => api.reserve(token, resource.id).then(() => undefined), "Added to the reservation queue.")}>Join queue</button>}
              <span className="identifier">{resource.identifier}</span>
            </div>
          </article>;
        })}
      </div>}
    </section>
  );
}

function Loans({ token, ownUserId }: { token: string; ownUserId: number }) {
  const [loans, setLoans] = useState<Loan[]>([]);
  useEffect(() => { api.loans(token).then((items) => setLoans(items.filter((loan) => loan.borrowerId === ownUserId))); }, [token, ownUserId]);
  return <section><PageHeader eyebrow="Your activity" title="My loans" detail="Track every request from review to return." /><DataTable headers={["Asset", "Status", "Requested", "Due"]}>{loans.map((loan) => <tr key={loan.id}><td><strong>{loan.assetTag}</strong></td><td><Status value={loan.status} /></td><td>{formatDate(loan.requestedAt)}</td><td>{formatDate(loan.dueAt)}</td></tr>)}</DataTable></section>;
}

function Reservations({ token, run }: { token: string; run: Runner }) {
  const [items, setItems] = useState<Reservation[]>([]);
  const load = useCallback(() => api.reservations(token).then(setItems), [token]);
  useEffect(() => { void load(); }, [load]);
  return <section><PageHeader eyebrow="Fair access" title="Reservation queue" detail="FIFO positions and ready-to-collect windows." /><DataTable headers={["Resource", "Status", "Position", "Created", ""]}>{items.map((item) => <tr key={item.id}><td><strong>{item.resourceName}</strong></td><td><Status value={item.status} /></td><td>{item.queuePosition || "—"}</td><td>{formatDate(item.createdAt)}</td><td>{["WAITING", "READY"].includes(item.status) && <button className="text-button" onClick={() => run(async () => { await api.cancelReservation(token, item.id); await load(); }, "Reservation cancelled.")}>Cancel</button>}</td></tr>)}</DataTable></section>;
}

function LoanDesk({ token, run, busy }: { token: string; run: Runner; busy: boolean }) {
  const [loans, setLoans] = useState<Loan[]>([]);
  const load = useCallback(() => api.loans(token).then(setLoans), [token]);
  useEffect(() => { void load(); }, [load]);
  return <section><PageHeader eyebrow="Staff workspace" title="Loan desk" detail="The next valid action is derived from the loan state machine." /><DataTable headers={["Borrower", "Asset", "Status", "Requested", "Action"]}>{loans.map((loan) => { const action = staffAction(loan.status); return <tr key={loan.id}><td>{loan.borrowerEmail}</td><td><strong>{loan.assetTag}</strong></td><td><Status value={loan.status} /></td><td>{formatDate(loan.requestedAt)}</td><td>{action && <button className="secondary" disabled={busy} onClick={() => run(async () => { await api.transitionLoan(token, loan.id, action.action); await load(); }, `Loan ${action.action} completed.`)}>{action.label}</button>}</td></tr>; })}</DataTable></section>;
}

function Dashboard({ token, run }: { token: string; run: Runner }) {
  const [summary, setSummary] = useState<DashboardSummary>();
  const [utilization, setUtilization] = useState<Utilization[]>([]);
  useEffect(() => { Promise.all([api.dashboard(token), api.utilization(token)]).then(([nextSummary, nextUtilization]) => { setSummary(nextSummary); setUtilization(nextUtilization); }); }, [token]);
  async function download() {
    const response = await fetch(api.exportUrl, { headers: { Authorization: `Bearer ${token}` } });
    if (!response.ok) throw new Error("CSV export failed.");
    const url = URL.createObjectURL(await response.blob());
    const anchor = document.createElement("a"); anchor.href = url; anchor.download = "resource-utilization.csv"; anchor.click(); URL.revokeObjectURL(url);
  }
  if (!summary) return <Skeleton />;
  return <section><PageHeader eyebrow="Live operations" title="Operational overview" detail={`Generated ${formatDate(summary.generatedAt)}`} action={<button className="secondary" onClick={() => run(download)}>Export CSV</button>} /><div className="metrics"><Metric label="Active loans" value={summary.activeLoans} /><Metric label="Overdue" value={summary.overdueLoans} alert /><Metric label="Unavailable items" value={summary.unavailableItems} /><Metric label="Avg. queue wait" value={`${Math.round(summary.averageReservationWaitSeconds / 3600)}h`} /></div><div className="panel"><div className="panel-heading"><div><span className="category">Inventory health</span><h3>Utilization by resource</h3></div><span className="muted">Current snapshot</span></div>{utilization.slice(0, 8).map((row) => <div className="util-row" key={row.resourceId}><div><strong>{row.resourceName}</strong><span>{row.onLoanItems} of {row.totalItems} on loan</span></div><div className="bar"><i style={{ width: `${Math.min(100, row.currentUtilizationPercent)}%` }} /></div><b>{Math.round(row.currentUtilizationPercent)}%</b></div>)}</div></section>;
}

type Runner = (operation: () => Promise<void>, success?: string) => Promise<void>;
const Brand = () => <div className="brand"><span className="brand-symbol">CG</span><span>Common Ground<small>Resource operations</small></span></div>;
const Splash = ({ label }: { label: string }) => <div className="splash"><Brand /><span>{label}…</span></div>;
function NavButton({ active, icon, children, onClick }: React.PropsWithChildren<{ active: boolean; icon: string; onClick: () => void }>) { return <button className={`nav-button ${active ? "active" : ""}`} onClick={onClick}><i>{icon}</i>{children}</button>; }
function PageHeader({ eyebrow, title, detail, action }: { eyebrow: string; title: string; detail: string; action?: React.ReactNode }) { return <header className="page-header"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p className="muted">{detail}</p></div>{action}</header>; }
const Status = ({ value }: { value: string }) => <span className={`status status-${value.toLowerCase()}`}><i />{readable(value)}</span>;
function DataTable({ headers, children }: React.PropsWithChildren<{ headers: string[] }>) { return <div className="table-wrap"><table><thead><tr>{headers.map((header, index) => <th key={`${header}-${index}`}>{header}</th>)}</tr></thead><tbody>{children}</tbody></table></div>; }
const Metric = ({ label, value, alert = false }: { label: string; value: string | number; alert?: boolean }) => <article className={`metric ${alert ? "alert" : ""}`}><span>{label}</span><strong>{value}</strong><small>Live operational value</small></article>;
const Skeleton = () => <div className="skeleton"><i /><i /><i /></div>;
