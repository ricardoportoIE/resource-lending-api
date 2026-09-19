import type {
  AuthTokens,
  CurrentUser,
  DashboardSummary,
  Loan,
  Page,
  Reservation,
  Resource,
  Utilization,
} from "./types";

const API = "/api/v1";

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public code?: string,
  ) {
    super(message);
  }
}

async function request<T>(path: string, token: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API}${path}`, {
    ...init,
    headers: {
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new ApiError(problem.detail ?? "The operation could not be completed.", response.status, problem.code);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

const idempotencyKey = () => crypto.randomUUID();

export const api = {
  register: (email: string, password: string) =>
    request<void>("/auth/register", "", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),
  login: (email: string, password: string) =>
    request<AuthTokens>("/auth/login", "", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),
  me: (token: string) => request<CurrentUser>("/users/me", token),
  resources: (token: string) => request<Page<Resource>>("/resources?size=100&sort=name,asc", token),
  loans: (token: string) => request<Loan[]>("/loans", token),
  requestLoan: (token: string, resourceItemId: string) =>
    request<Loan>("/loans", token, {
      method: "POST",
      headers: { "Idempotency-Key": idempotencyKey() },
      body: JSON.stringify({ resourceItemId }),
    }),
  transitionLoan: (token: string, loanId: string, transition: string) =>
    request<Loan>(`/loans/${loanId}/${transition}`, token, {
      method: "POST",
      headers: { "Idempotency-Key": idempotencyKey() },
    }),
  reservations: (token: string) => request<Reservation[]>("/reservations", token),
  reserve: (token: string, resourceId: string) =>
    request<Reservation>(`/resources/${resourceId}/reservations`, token, {
      method: "POST",
      headers: { "Idempotency-Key": idempotencyKey() },
    }),
  cancelReservation: (token: string, reservationId: string) =>
    request<void>(`/reservations/${reservationId}`, token, { method: "DELETE" }),
  dashboard: (token: string) => request<DashboardSummary>("/reports/dashboard", token),
  utilization: (token: string) => request<Utilization[]>("/reports/resource-utilization", token),
  exportUrl: `${API}/reports/export.csv?report=RESOURCE_UTILIZATION`,
};
