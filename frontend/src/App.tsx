import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ApiError, api, session } from "./api";
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

export default function App() {
  const [token, setToken] = useState(session.accessToken);
  const [user, setUser] = useState<CurrentUser>();
  const [view, setView] = useState<View>("catalogue");
  const [notice, setNotice] = useState<Notice>();
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);

  const signOut = useCallback(() => {
    if (token) void api.logout(token, session.refreshToken());
    session.clear();
    setToken("");
    setUser(undefined);
  }, [token]);

  useEffect(() => {
    if (!token) return;
    api.me(token).then(setUser).catch(signOut);
  }, [token, signOut]);

  const handleError = useCallback(
    (error: unknown) => {
      if (error instanceof ApiError && error.status === 401) signOut();
      setNotice({
        tone: "error",
        message: error instanceof Error ? error.message : "Unexpected error.",
      });
    },
    [signOut],
  );

  const run = useCallback(
    async (operation: () => Promise<void>, success?: string) => {
      if (busyRef.current) return;
      busyRef.current = true;
      setBusy(true);
      setNotice(undefined);
      try {
        await operation();
        if (success) setNotice({ tone: "success", message: success });
      } catch (error) {
        handleError(error);
      } finally {
        busyRef.current = false;
        setBusy(false);
      }
    },
    [handleError],
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
        {view === "catalogue" && <Catalogue token={token} run={run} onError={handleError} busy={busy} />}
        {view === "loans" && <Loans token={token} ownUserId={user.id} onError={handleError} />}
        {view === "reservations" && <Reservations token={token} run={run} onError={handleError} busy={busy} />}
        {view === "operations" && <LoanDesk token={token} run={run} onError={handleError} busy={busy} />}
        {view === "dashboard" && <Dashboard token={token} run={run} onError={handleError} />}
      </main>
      {busy && <div className="progress" aria-label="Operation in progress" />}
    </div>
  );
}

export function Login({ onLogin }: { onLogin: (token: string) => void }) {
  type AuthMode = "login" | "register" | "forgot" | "reset" | "confirm";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [identityToken, setIdentityToken] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [mode, setMode] = useState<AuthMode>("login");

  function switchMode(next: AuthMode) {
    setMode(next);
    setError("");
    setMessage("");
    setPassword("");
    setIdentityToken("");
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");
    try {
      if (mode === "register") {
        await api.register(email, password);
        switchMode("confirm");
        setMessage("Account created. Enter the token delivered by the notification adapter.");
        return;
      }
      if (mode === "forgot") {
        await api.forgotPassword(email);
        switchMode("reset");
        setMessage("If the account exists, a reset token has been sent.");
        return;
      }
      if (mode === "reset") {
        await api.resetPassword(identityToken, password);
        switchMode("login");
        setMessage("Password updated. You can now sign in.");
        return;
      }
      if (mode === "confirm") {
        await api.confirmEmail(identityToken);
        switchMode("login");
        setMessage("Email confirmed. You can now sign in.");
        return;
      }
      const tokens = await api.login(email, password);
      session.save(tokens);
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
        <form onSubmit={submit} aria-busy={loading}>
          <p className="eyebrow">{copy[0]}</p>
          <h2>{copy[1]}</h2>
          <p className="muted">{copy[2]}</p>
          {["login", "register", "forgot"].includes(mode) && <label>Email<input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoFocus /></label>}
          {["login", "register", "reset"].includes(mode) && <label>{mode === "reset" ? "New password" : "Password"}<input type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} minLength={mode === "reset" ? 12 : mode === "register" ? 8 : undefined} value={password} onChange={(event) => setPassword(event.target.value)} required /></label>}
          {["reset", "confirm"].includes(mode) && <label>One-time token<input value={identityToken} onChange={(event) => setIdentityToken(event.target.value)} required autoFocus /></label>}
          {message && <div className="form-success" role="status">{message}</div>}
          {error && <div className="form-error" role="alert">{error}</div>}
          <button className="primary wide" disabled={loading}>{loading ? "Please wait…" : "Continue"}</button>
          <div className="auth-links">
            {mode !== "login" && <button type="button" onClick={() => switchMode("login")}>Back to sign in</button>}
            {mode === "login" && <><button type="button" onClick={() => switchMode("register")}>Create account</button><button type="button" onClick={() => switchMode("forgot")}>Forgot password</button><button type="button" onClick={() => switchMode("confirm")}>Confirm email</button></>}
          </div>
          <small>Tokens remain in this browser tab and are cleared when you sign out.</small>
        </form>
      </section>
    </div>
  );
}

export function Catalogue({ token, run, onError, busy }: { token: string; run: Runner; onError: ErrorHandler; busy: boolean }) {
  const [resources, setResources] = useState<Resource[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const page = await api.resources(token);
      setResources(page.content);
    } catch (error) {
      onError(error);
    } finally {
      setLoading(false);
    }
  }, [token, onError]);
  useEffect(() => { void load(); }, [load]);
  const filtered = useMemo(() => resources.filter((resource) => `${resource.name} ${resource.category} ${resource.type}`.toLowerCase().includes(query.toLowerCase())), [resources, query]);

  return (
    <section>
      <PageHeader eyebrow="Discover" title="Resource catalogue" detail={`${resources.length} resource types in the shared inventory`} />
      <div className="toolbar"><input className="search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search equipment, spaces or books…" /></div>
      {loading ? <Skeleton /> : filtered.length === 0 ? <EmptyState message={resources.length ? "No resources match your search." : "No resources have been added yet."} /> : <div className="card-grid">
        {filtered.map((resource) => {
          const available = resource.items.filter((item) => item.status === "AVAILABLE");
          return <article className="resource-card" key={resource.id}>
            <div className="resource-top"><span className="type-mark">{resource.type.slice(0, 2)}</span><Status value={available.length ? "AVAILABLE" : "WAITLIST"} /></div>
            <div><span className="category">{resource.category}</span><h3>{resource.name}</h3><p>{resource.description || "Managed shared resource"}</p></div>
            <div className="inventory-line"><strong>{available.length}</strong><span>available of {resource.items.length}</span></div>
            <div className="card-actions">
              {available[0] && resource.loanable ? <button className="primary" disabled={busy} onClick={() => run(async () => { await api.requestLoan(token, available[0].id); await load(); }, "Loan request created.")}>Request item</button> : resource.loanable ? <button className="primary" disabled={busy} onClick={() => run(() => api.reserve(token, resource.id).then(() => undefined), "Added to the reservation queue.")}>Join queue</button> : <button className="secondary" disabled>Reference only</button>}
              <span className="identifier">{resource.identifier}</span>
            </div>
          </article>;
        })}
      </div>}
    </section>
  );
}

export function Loans({ token, ownUserId, onError }: { token: string; ownUserId: number; onError: ErrorHandler }) {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { api.loans(token).then((items) => setLoans(items.filter((loan) => loan.borrowerId === ownUserId))).catch(onError).finally(() => setLoading(false)); }, [token, ownUserId, onError]);
  return <section><PageHeader eyebrow="Your activity" title="My loans" detail="Track every request from review to return." />{loading ? <Skeleton /> : <DataTable headers={["Asset", "Status", "Requested", "Due"]}>{loans.length ? loans.map((loan) => <tr key={loan.id}><td><strong>{loan.assetTag}</strong></td><td><Status value={loan.status} /></td><td>{formatDate(loan.requestedAt)}</td><td>{formatDate(loan.dueAt)}</td></tr>) : <EmptyRow columns={4} message="You do not have any loans yet." />}</DataTable>}</section>;
}

export function Reservations({ token, run, onError, busy }: { token: string; run: Runner; onError: ErrorHandler; busy: boolean }) {
  const [items, setItems] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api.reservations(token));
    } catch (error) {
      onError(error);
    } finally {
      setLoading(false);
    }
  }, [token, onError]);
  useEffect(() => { void load(); }, [load]);
  return <section><PageHeader eyebrow="Fair access" title="Reservation queue" detail="FIFO positions and ready-to-collect windows." />{loading ? <Skeleton /> : <DataTable headers={["Resource", "Status", "Position", "Created", ""]}>{items.length ? items.map((item) => <tr key={item.id}><td><strong>{item.resourceName}</strong></td><td><Status value={item.status} /></td><td>{item.queuePosition || "—"}</td><td>{formatDate(item.createdAt)}</td><td>{["WAITING", "READY"].includes(item.status) && <button className="text-button" disabled={busy} onClick={() => run(async () => { await api.cancelReservation(token, item.id); await load(); }, "Reservation cancelled.")}>Cancel</button>}</td></tr>) : <EmptyRow columns={5} message="You do not have any reservations." />}</DataTable>}</section>;
}

export function LoanDesk({ token, run, onError, busy }: { token: string; run: Runner; onError: ErrorHandler; busy: boolean }) {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      setLoans(await api.loans(token));
    } catch (error) {
      onError(error);
    } finally {
      setLoading(false);
    }
  }, [token, onError]);
  useEffect(() => { void load(); }, [load]);
  return <section><PageHeader eyebrow="Staff workspace" title="Loan desk" detail="The next valid action is derived from the loan state machine." />{loading ? <Skeleton /> : <DataTable headers={["Borrower", "Asset", "Status", "Requested", "Action"]}>{loans.length ? loans.map((loan) => { const action = staffAction(loan.status); return <tr key={loan.id}><td>{loan.borrowerEmail}</td><td><strong>{loan.assetTag}</strong></td><td><Status value={loan.status} /></td><td>{formatDate(loan.requestedAt)}</td><td>{action && <button className="secondary" disabled={busy} onClick={() => run(async () => { await api.transitionLoan(token, loan.id, action.action); await load(); }, `Loan ${action.action} completed.`)}>{action.label}</button>}</td></tr>; }) : <EmptyRow columns={5} message="There are no loans to process." />}</DataTable>}</section>;
}

export function Dashboard({ token, run, onError }: { token: string; run: Runner; onError: ErrorHandler }) {
  const [summary, setSummary] = useState<DashboardSummary>();
  const [utilization, setUtilization] = useState<Utilization[]>([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { Promise.all([api.dashboard(token), api.utilization(token)]).then(([nextSummary, nextUtilization]) => { setSummary(nextSummary); setUtilization(nextUtilization); }).catch(onError).finally(() => setLoading(false)); }, [token, onError]);
  async function download() {
    const response = await fetch(api.exportUrl, { headers: { Authorization: `Bearer ${token}` } });
    if (!response.ok) throw new Error("CSV export failed.");
    const url = URL.createObjectURL(await response.blob());
    const anchor = document.createElement("a"); anchor.href = url; anchor.download = "resource-utilization.csv"; anchor.click(); URL.revokeObjectURL(url);
  }
  if (loading) return <Skeleton />;
  if (!summary) return <EmptyState message="Operational data is currently unavailable." />;
  return <section><PageHeader eyebrow="Live operations" title="Operational overview" detail={`Generated ${formatDate(summary.generatedAt)}`} action={<button className="secondary" onClick={() => run(download)}>Export CSV</button>} /><div className="metrics"><Metric label="Active loans" value={summary.activeLoans} /><Metric label="Overdue" value={summary.overdueLoans} alert /><Metric label="Unavailable items" value={summary.unavailableItems} /><Metric label="Avg. queue wait" value={`${Math.round(summary.averageReservationWaitSeconds / 3600)}h`} /></div><div className="panel"><div className="panel-heading"><div><span className="category">Inventory health</span><h3>Utilization by resource</h3></div><span className="muted">Current snapshot</span></div>{utilization.length ? utilization.slice(0, 8).map((row) => <div className="util-row" key={row.resourceId}><div><strong>{row.resourceName}</strong><span>{row.onLoanItems} of {row.totalItems} on loan</span></div><div className="bar"><i style={{ width: `${Math.min(100, row.currentUtilizationPercent)}%` }} /></div><b>{Math.round(row.currentUtilizationPercent)}%</b></div>) : <EmptyState message="No utilization data is available yet." />}</div></section>;
}

type Runner = (operation: () => Promise<void>, success?: string) => Promise<void>;
type ErrorHandler = (error: unknown) => void;
const Brand = () => <div className="brand"><span className="brand-symbol">CG</span><span>Common Ground<small>Resource operations</small></span></div>;
const Splash = ({ label }: { label: string }) => <div className="splash"><Brand /><span>{label}…</span></div>;
function NavButton({ active, icon, children, onClick }: React.PropsWithChildren<{ active: boolean; icon: string; onClick: () => void }>) { return <button className={`nav-button ${active ? "active" : ""}`} onClick={onClick}><i>{icon}</i>{children}</button>; }
function PageHeader({ eyebrow, title, detail, action }: { eyebrow: string; title: string; detail: string; action?: React.ReactNode }) { return <header className="page-header"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p className="muted">{detail}</p></div>{action}</header>; }
const Status = ({ value }: { value: string }) => <span className={`status status-${value.toLowerCase()}`}><i />{readable(value)}</span>;
function DataTable({ headers, children }: React.PropsWithChildren<{ headers: string[] }>) { return <div className="table-wrap"><table><thead><tr>{headers.map((header, index) => <th key={`${header}-${index}`}>{header}</th>)}</tr></thead><tbody>{children}</tbody></table></div>; }
const EmptyRow = ({ columns, message }: { columns: number; message: string }) => <tr><td className="empty-cell" colSpan={columns}>{message}</td></tr>;
const EmptyState = ({ message }: { message: string }) => <div className="empty-state">{message}</div>;
const Metric = ({ label, value, alert = false }: { label: string; value: string | number; alert?: boolean }) => <article className={`metric ${alert ? "alert" : ""}`}><span>{label}</span><strong>{value}</strong><small>Live operational value</small></article>;
const Skeleton = () => <div className="skeleton"><i /><i /><i /></div>;
