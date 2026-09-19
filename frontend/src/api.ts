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

type ProblemBody = {
  detail?: string;
  code?: string;
  correlationId?: string;
  errors?: Record<string, string>;
};

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public code?: string,
    public correlationId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

const fallbackMessage = (status: number) => {
  if (status === 401) return "Your session is invalid or has expired. Please sign in again.";
  if (status === 403) return "You do not have permission to perform this operation.";
  if (status === 404) return "The requested operation is not available.";
  if (status === 409) return "The operation conflicts with the current resource state.";
  if (status === 429) return "Too many requests. Please wait briefly and try again.";
  if (status >= 500) return "The service is temporarily unavailable. Please try again.";
  return `The operation could not be completed (HTTP ${status}).`;
};

const storage = {
  get(key: string) {
    try {
      return sessionStorage.getItem(key) ?? "";
    } catch {
      return "";
    }
  },
  set(tokens: AuthTokens) {
    try {
      sessionStorage.setItem("accessToken", tokens.accessToken);
      sessionStorage.setItem("refreshToken", tokens.refreshToken);
    } catch {
      // A disabled storage backend must not make authentication itself fail.
    }
  },
  clear() {
    try {
      sessionStorage.removeItem("accessToken");
      sessionStorage.removeItem("refreshToken");
    } catch {
      // There is no session state to clean when storage is unavailable.
    }
  },
};

async function apiError(response: Response): Promise<ApiError> {
  const text = await response.text();
  let problem: ProblemBody = {};
  if (text) {
    try {
      problem = JSON.parse(text) as ProblemBody;
    } catch {
      // Reverse proxies can return HTML or plain text. Use a status-aware safe message.
    }
  }
  const validationMessage = problem.errors && Object.values(problem.errors)[0];
  return new ApiError(
    validationMessage ?? problem.detail ?? fallbackMessage(response.status),
    response.status,
    problem.code,
    problem.correlationId ?? response.headers.get("X-Correlation-ID") ?? undefined,
  );
}

async function fetchApi(path: string, token: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(`${API}${path}`, {
      ...init,
      credentials: "same-origin",
      headers: {
        Accept: "application/json, application/problem+json",
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    });
  } catch {
    throw new ApiError(
      "Unable to reach the API. Check that the local services are running and try again.",
      0,
      "NETWORK_ERROR",
    );
  }
}

let refreshInFlight: Promise<AuthTokens> | undefined;

async function refreshSession(): Promise<AuthTokens> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const refreshToken = storage.get("refreshToken");
      if (!refreshToken) throw new ApiError("Your session has expired.", 401, "SESSION_EXPIRED");
      const response = await fetchApi("/auth/refresh", "", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
      });
      if (!response.ok) throw await apiError(response);
      const tokens = (await response.json()) as AuthTokens;
      storage.set(tokens);
      return tokens;
    })().finally(() => {
      refreshInFlight = undefined;
    });
  }
  return refreshInFlight;
}

async function request<T>(
  path: string,
  token: string,
  init: RequestInit = {},
  canRefresh = true,
): Promise<T> {
  const effectiveToken = token ? storage.get("accessToken") || token : "";
  const response = await fetchApi(path, effectiveToken, init);
  if (response.status === 401 && effectiveToken && canRefresh && !path.startsWith("/auth/")) {
    try {
      const tokens = await refreshSession();
      return request<T>(path, tokens.accessToken, init, false);
    } catch (error) {
      storage.clear();
      throw error;
    }
  }
  if (!response.ok) throw await apiError(response);
  if (response.status === 204) return undefined as T;
  const body = await response.text();
  return body ? (JSON.parse(body) as T) : (undefined as T);
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
  forgotPassword: (email: string) =>
    request<void>("/auth/password/forgot", "", {
      method: "POST",
      body: JSON.stringify({ email }),
    }),
  resetPassword: (token: string, newPassword: string) =>
    request<void>("/auth/password/reset", "", {
      method: "POST",
      body: JSON.stringify({ token, newPassword }),
    }),
  confirmEmail: (token: string) =>
    request<void>("/auth/email/confirm", "", {
      method: "POST",
      body: JSON.stringify({ token }),
    }),
  logout: (token: string, refreshToken: string) =>
    Promise.allSettled([
      request<void>("/auth/revoke", token, { method: "POST" }),
      request<void>("/auth/logout", "", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
      }),
    ]),
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

export const session = {
  accessToken: () => storage.get("accessToken"),
  refreshToken: () => storage.get("refreshToken"),
  save: (tokens: AuthTokens) => storage.set(tokens),
  clear: () => storage.clear(),
};
