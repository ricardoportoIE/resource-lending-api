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
});
