import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, api, session } from "./api";

const json = (body: unknown, status = 200, headers: Record<string, string> = {}) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...headers },
  });

describe("API client", () => {
  let values: Map<string, string>;

  beforeEach(() => {
    values = new Map();
    vi.stubGlobal("sessionStorage", {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => values.set(key, value),
      removeItem: (key: string) => values.delete(key),
    });
  });

  afterEach(() => vi.unstubAllGlobals());

  it("returns authentication tokens from a successful login", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        json({ accessToken: "access", refreshToken: "refresh", tokenType: "Bearer", expiresIn: 900 }),
      ),
    );

    await expect(api.login("student@example.com", "password")).resolves.toMatchObject({
      accessToken: "access",
      refreshToken: "refresh",
    });
  });

  it("surfaces a field validation message and correlation id", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        json(
          {
            detail: "Request validation failed.",
            code: "VALIDATION_ERROR",
            correlationId: "correlation-123",
            errors: { password: "Password must contain at least 8 characters" },
          },
          400,
        ),
      ),
    );

    const error = await api.register("student@example.com", "short").catch((caught) => caught);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      message: "Password must contain at least 8 characters",
      status: 400,
      code: "VALIDATION_ERROR",
      correlationId: "correlation-123",
    });
  });

  it("uses a useful status message when a proxy returns non-JSON content", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("Bad gateway", { status: 502 })),
    );

    await expect(api.login("student@example.com", "password")).rejects.toMatchObject({
      message: "The service is temporarily unavailable. Please try again.",
      status: 502,
    });
  });

  it("identifies connectivity failures", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

    await expect(api.login("student@example.com", "password")).rejects.toMatchObject({
      message: "Unable to reach the API. Check that the local services are running and try again.",
      code: "NETWORK_ERROR",
    });
  });

  it("rotates the session once and retries an expired authenticated request", async () => {
    session.save({
      accessToken: "expired-access",
      refreshToken: "valid-refresh",
      tokenType: "Bearer",
      expiresIn: 900,
    });
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(json({ detail: "Expired" }, 401))
      .mockResolvedValueOnce(
        json({ accessToken: "new-access", refreshToken: "new-refresh", tokenType: "Bearer", expiresIn: 900 }),
      )
      .mockResolvedValueOnce(json({ content: [], totalElements: 0, totalPages: 0, number: 0 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(api.resources("expired-access")).resolves.toMatchObject({ content: [] });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1][0]).toBe("/api/v1/auth/refresh");
    expect((fetchMock.mock.calls[2][1] as RequestInit).headers).toMatchObject({
      Authorization: "Bearer new-access",
    });
    expect(session.accessToken()).toBe("new-access");
    expect(session.refreshToken()).toBe("new-refresh");
  });

  it("does not attach authorization or JSON content headers to public bodyless requests", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await api.confirmEmail("one-time-token");

    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.credentials).toBe("same-origin");
    expect(init.headers).not.toHaveProperty("Authorization");
    expect(init.headers).toMatchObject({ "Content-Type": "application/json" });
  });

  it("attaches bearer tokens only to authenticated API calls", async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ id: 1, email: "student@example.com", roles: ["STUDENT"] }));
    vi.stubGlobal("fetch", fetchMock);

    await api.me("access-token");

    expect((fetchMock.mock.calls[0][1] as RequestInit).headers).toMatchObject({
      Authorization: "Bearer access-token",
      Accept: "application/json, application/problem+json",
    });
    expect((fetchMock.mock.calls[0][1] as RequestInit).headers).not.toHaveProperty("Content-Type");
  });

  it("clears local credentials when refresh is rejected", async () => {
    session.save({ accessToken: "expired", refreshToken: "revoked", tokenType: "Bearer", expiresIn: 1 });
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockResolvedValueOnce(json({ detail: "Expired" }, 401))
        .mockResolvedValueOnce(json({ detail: "Refresh token is invalid", code: "INVALID_REFRESH_TOKEN" }, 401)),
    );

    await expect(api.resources("expired")).rejects.toMatchObject({ status: 401 });
    expect(session.accessToken()).toBe("");
    expect(session.refreshToken()).toBe("");
  });

  it("shares one refresh request across concurrent expired requests", async () => {
    session.save({ accessToken: "expired", refreshToken: "valid-refresh", tokenType: "Bearer", expiresIn: 1 });
    let refreshCalls = 0;
    const fetchMock = vi.fn(async (url: string, init: RequestInit) => {
      const authorization = (init.headers as Record<string, string>).Authorization;
      if (url.endsWith("/auth/refresh")) {
        refreshCalls += 1;
        await Promise.resolve();
        return json({ accessToken: "fresh", refreshToken: "rotated", tokenType: "Bearer", expiresIn: 900 });
      }
      if (authorization === "Bearer expired") return json({ detail: "Expired" }, 401);
      return url.includes("/resources")
        ? json({ content: [], totalElements: 0, totalPages: 0, number: 0 })
        : json([]);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(Promise.all([api.resources("expired"), api.loans("expired")])).resolves.toHaveLength(2);
    expect(refreshCalls).toBe(1);
    expect(session.accessToken()).toBe("fresh");
  });

  it("does not retry authentication endpoints after a 401", async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ detail: "Invalid credentials" }, 401));
    vi.stubGlobal("fetch", fetchMock);

    await expect(api.login("student@example.com", "wrong")).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it("generates a distinct idempotency key for each duplicate-sensitive command", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(json({ id: "created" }, 201))));

    await api.requestLoan("access", "item-1");
    await api.reserve("access", "resource-1");

    const fetchMock = vi.mocked(fetch);
    const first = (fetchMock.mock.calls[0][1] as RequestInit).headers as Record<string, string>;
    const second = (fetchMock.mock.calls[1][1] as RequestInit).headers as Record<string, string>;
    expect(first["Idempotency-Key"]).toMatch(/^[0-9a-f-]{36}$/);
    expect(second["Idempotency-Key"]).toMatch(/^[0-9a-f-]{36}$/);
    expect(first["Idempotency-Key"]).not.toBe(second["Idempotency-Key"]);
  });

  it("attempts both server-side revocations even when one fails", async () => {
    const fetchMock = vi
      .fn()
      .mockRejectedValueOnce(new TypeError("offline"))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(api.logout("access", "refresh")).resolves.toHaveLength(2);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
      "/api/v1/auth/revoke",
      "/api/v1/auth/logout",
    ]);
  });

  it("keeps session helpers safe when browser storage is unavailable", () => {
    vi.stubGlobal("sessionStorage", {
      getItem: () => { throw new DOMException("Blocked"); },
      setItem: () => { throw new DOMException("Blocked"); },
      removeItem: () => { throw new DOMException("Blocked"); },
    });

    expect(() => session.save({ accessToken: "a", refreshToken: "r", tokenType: "Bearer", expiresIn: 1 })).not.toThrow();
    expect(session.accessToken()).toBe("");
    expect(() => session.clear()).not.toThrow();
  });
});
