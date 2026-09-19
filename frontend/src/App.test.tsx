import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axe from "axe-core";
import { beforeEach, describe, expect, it, vi } from "vitest";
import App, { Catalogue, Dashboard, LoanDesk, Loans, Login, Reservations } from "./App";
import { ApiError } from "./api";
import type { AuthTokens, CurrentUser, Loan, Reservation, Resource } from "./types";

const mocks = vi.hoisted(() => ({
  register: vi.fn(), login: vi.fn(), forgotPassword: vi.fn(), resetPassword: vi.fn(),
  confirmEmail: vi.fn(), logout: vi.fn(), me: vi.fn(), resources: vi.fn(), loans: vi.fn(),
  requestLoan: vi.fn(), transitionLoan: vi.fn(), reservations: vi.fn(), reserve: vi.fn(),
  cancelReservation: vi.fn(), dashboard: vi.fn(), utilization: vi.fn(),
  accessToken: vi.fn(), refreshToken: vi.fn(), save: vi.fn(), clear: vi.fn(),
}));

vi.mock("./api", async () => {
  const actual = await vi.importActual<typeof import("./api")>("./api");
  return {
    ...actual,
    api: {
      register: mocks.register, login: mocks.login, forgotPassword: mocks.forgotPassword,
      resetPassword: mocks.resetPassword, confirmEmail: mocks.confirmEmail, logout: mocks.logout,
      me: mocks.me, resources: mocks.resources, loans: mocks.loans, requestLoan: mocks.requestLoan,
      transitionLoan: mocks.transitionLoan, reservations: mocks.reservations, reserve: mocks.reserve,
      cancelReservation: mocks.cancelReservation, dashboard: mocks.dashboard,
      utilization: mocks.utilization, exportUrl: "/api/v1/reports/export.csv?report=RESOURCE_UTILIZATION",
    },
    session: {
      accessToken: mocks.accessToken, refreshToken: mocks.refreshToken,
      save: mocks.save, clear: mocks.clear,
    },
  };
});

const tokens: AuthTokens = {
  accessToken: "access-token", refreshToken: "refresh-token", tokenType: "Bearer", expiresIn: 900,
};
const student: CurrentUser = {
  id: 7, email: "student@example.com", firstName: "Ada", roles: ["STUDENT"],
};
const staff: CurrentUser = { ...student, email: "staff@example.com", roles: ["STAFF"] };
const resource = (overrides: Partial<Resource> = {}): Resource => ({
  id: "resource-1", name: "Clean Architecture", description: "A shared book", type: "BOOK",
  category: "Engineering", identifier: "BOOK-001", loanable: true,
  items: [{ id: "item-1", assetTag: "BOOK-001-A", status: "AVAILABLE", version: 0, updatedAt: "2026-09-19T08:00:00Z" }],
  ...overrides,
});
const loan = (overrides: Partial<Loan> = {}): Loan => ({
  id: "loan-1", borrowerId: 7, borrowerEmail: "student@example.com", resourceItemId: "item-1",
  assetTag: "BOOK-001-A", status: "REQUESTED", requestedAt: "2026-09-19T08:00:00Z", ...overrides,
});
const reservation = (overrides: Partial<Reservation> = {}): Reservation => ({
  id: "reservation-1", resourceId: "resource-1", resourceName: "Clean Architecture",
  status: "WAITING", queuePosition: 1, createdAt: "2026-09-19T08:00:00Z", ...overrides,
});
const run = async (operation: () => Promise<void>) => operation();

beforeEach(() => {
  vi.resetAllMocks();
  mocks.accessToken.mockReturnValue("");
  mocks.refreshToken.mockReturnValue("refresh-token");
  mocks.resources.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0 });
  mocks.loans.mockResolvedValue([]);
  mocks.reservations.mockResolvedValue([]);
  mocks.dashboard.mockResolvedValue({
    activeLoans: 2, overdueLoans: 1, returnedLoans: 8, unavailableItems: 3, maintenanceItems: 1,
    averageReservationWaitSeconds: 7200, reservationWaitSampleSize: 4, generatedAt: "2026-09-19T08:00:00Z",
  });
  mocks.utilization.mockResolvedValue([]);
});

describe("authentication journeys", () => {
  it("stores a successful login without imposing the registration password length", async () => {
    mocks.login.mockResolvedValue(tokens);
    const onLogin = vi.fn();
    render(<Login onLogin={onLogin} />);

    await userEvent.type(screen.getByLabelText("Email"), "student@example.com");
    const password = screen.getByLabelText("Password");
    expect(password).not.toHaveAttribute("minlength");
    await userEvent.type(password, "123");
    await userEvent.click(screen.getByRole("button", { name: "Continue" }));

    await waitFor(() => expect(mocks.login).toHaveBeenCalledWith("student@example.com", "123"));
    expect(mocks.save).toHaveBeenCalledWith(tokens);
    expect(onLogin).toHaveBeenCalledWith("access-token");
  });

  it("renders safe API errors and does not expose correlation details in the page", async () => {
    mocks.login.mockRejectedValue(new ApiError("Invalid e-mail or password.", 401, "INVALID_CREDENTIALS", "secret-correlation"));
    render(<Login onLogin={vi.fn()} />);
    await userEvent.type(screen.getByLabelText("Email"), "student@example.com");
    await userEvent.type(screen.getByLabelText("Password"), "wrong");
    await userEvent.click(screen.getByRole("button", { name: "Continue" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid e-mail or password.");
    expect(document.body).not.toHaveTextContent("secret-correlation");
  });

  it("creates an account and continues to one-time token confirmation", async () => {
    mocks.register.mockResolvedValue(undefined);
    render(<Login onLogin={vi.fn()} />);
    await userEvent.click(screen.getByRole("button", { name: "Create account" }));
    await userEvent.type(screen.getByLabelText("Email"), "new@example.com");
    const password = screen.getByLabelText("Password");
    expect(password).toHaveAttribute("minlength", "8");
    await userEvent.type(password, "StrongPass!2026");
    await userEvent.click(screen.getByRole("button", { name: "Continue" }));
    expect(await screen.findByLabelText("One-time token")).toBeVisible();
    expect(mocks.register).toHaveBeenCalledWith("new@example.com", "StrongPass!2026");
    expect(screen.getByRole("status")).toHaveTextContent("Account created");
  });

  it("supports enumeration-safe password recovery and reset", async () => {
    mocks.forgotPassword.mockResolvedValue(undefined);
    mocks.resetPassword.mockResolvedValue(undefined);
    render(<Login onLogin={vi.fn()} />);
    await userEvent.click(screen.getByRole("button", { name: "Forgot password" }));
    await userEvent.type(screen.getByLabelText("Email"), "unknown@example.com");
    await userEvent.click(screen.getByRole("button", { name: "Continue" }));
    expect(await screen.findByText(/If the account exists/)).toBeVisible();
    await userEvent.type(screen.getByLabelText("One-time token"), "one-time-token");
    await userEvent.type(screen.getByLabelText("New password"), "Replacement!2026");
    await userEvent.click(screen.getByRole("button", { name: "Continue" }));
    await waitFor(() => expect(mocks.resetPassword).toHaveBeenCalledWith("one-time-token", "Replacement!2026"));
    expect(screen.getByText("Sign in to operations")).toBeVisible();
  });

  it("has no automatically detectable accessibility violations on the login surface", async () => {
    const { container } = render(<Login onLogin={vi.fn()} />);
    const results = await axe.run(container, { rules: { "color-contrast": { enabled: false } } });
    expect(results.violations).toEqual([]);
  });
});

describe("role-aware application shell", () => {
  it("hides privileged navigation from students", async () => {
    mocks.accessToken.mockReturnValue("access-token");
    mocks.me.mockResolvedValue(student);
    render(<App />);
    expect(await screen.findByText("Resource catalogue")).toBeVisible();
    expect(screen.queryByRole("button", { name: /Loan desk/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Dashboard/ })).not.toBeInTheDocument();
  });

  it("shows staff tools and clears both client and server sessions on sign out", async () => {
    mocks.accessToken.mockReturnValue("access-token");
    mocks.me.mockResolvedValue(staff);
    mocks.logout.mockResolvedValue([]);
    render(<App />);
    expect(await screen.findByRole("button", { name: /Loan desk/ })).toBeVisible();
    await userEvent.click(screen.getByRole("button", { name: "Sign out" }));
    expect(mocks.logout).toHaveBeenCalledWith("access-token", "refresh-token");
    expect(mocks.clear).toHaveBeenCalledOnce();
    expect(screen.getByText("Sign in to operations")).toBeVisible();
  });

  it("clears a stale local session when identity lookup is rejected", async () => {
    mocks.accessToken.mockReturnValue("expired-token");
    mocks.me.mockRejectedValue(new ApiError("Expired", 401));
    render(<App />);
    await waitFor(() => expect(mocks.clear).toHaveBeenCalled());
    expect(await screen.findByText("Sign in to operations")).toBeVisible();
  });
});

describe("operational components", () => {
  it("filters catalogue results and requests the first available item", async () => {
    mocks.resources.mockResolvedValue({ content: [resource(), resource({ id: "resource-2", name: "Meeting room", identifier: "ROOM-1" })], totalElements: 2, totalPages: 1, number: 0 });
    mocks.requestLoan.mockResolvedValue(loan());
    render(<Catalogue token="token" run={run} onError={vi.fn()} busy={false} />);
    expect(await screen.findByText("Clean Architecture")).toBeVisible();
    await userEvent.type(screen.getByPlaceholderText(/Search equipment/), "meeting");
    expect(screen.queryByText("Clean Architecture")).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Request item" }));
    expect(mocks.requestLoan).toHaveBeenCalledWith("token", "item-1");
  });

  it("offers a reservation only for unavailable loanable resources", async () => {
    mocks.resources.mockResolvedValue({ content: [resource({ items: [] })], totalElements: 1, totalPages: 1, number: 0 });
    mocks.reserve.mockResolvedValue(reservation());
    render(<Catalogue token="token" run={run} onError={vi.fn()} busy={false} />);
    await userEvent.click(await screen.findByRole("button", { name: "Join queue" }));
    expect(mocks.reserve).toHaveBeenCalledWith("token", "resource-1");
  });

  it("shows only the signed-in user's loans", async () => {
    mocks.loans.mockResolvedValue([loan(), loan({ id: "loan-2", borrowerId: 99, assetTag: "HIDDEN" })]);
    render(<Loans token="token" ownUserId={7} onError={vi.fn()} />);
    expect(await screen.findByText("BOOK-001-A")).toBeVisible();
    expect(screen.queryByText("HIDDEN")).not.toBeInTheDocument();
  });

  it("cancels actionable reservations and refreshes the queue", async () => {
    mocks.reservations.mockResolvedValue([reservation()]);
    mocks.cancelReservation.mockResolvedValue(undefined);
    render(<Reservations token="token" run={run} onError={vi.fn()} busy={false} />);
    await userEvent.click(await screen.findByRole("button", { name: "Cancel" }));
    expect(mocks.cancelReservation).toHaveBeenCalledWith("token", "reservation-1");
    expect(mocks.reservations).toHaveBeenCalledTimes(2);
  });

  it("derives and executes the valid staff transition", async () => {
    mocks.loans.mockResolvedValue([loan()]);
    mocks.transitionLoan.mockResolvedValue(loan({ status: "APPROVED" }));
    render(<LoanDesk token="token" run={run} onError={vi.fn()} busy={false} />);
    await userEvent.click(await screen.findByRole("button", { name: "Approve" }));
    expect(mocks.transitionLoan).toHaveBeenCalledWith("token", "loan-1", "approve");
  });

  it("renders dashboard metrics and caps utilization bars at 100 percent", async () => {
    mocks.utilization.mockResolvedValue([{ resourceId: "resource-1", resourceName: "Camera", totalItems: 2, onLoanItems: 2, unavailableItems: 0, currentUtilizationPercent: 125 }]);
    const { container } = render(<Dashboard token="token" run={run} onError={vi.fn()} />);
    expect(await screen.findByText("Operational overview")).toBeVisible();
    expect(screen.getByText("2h")).toBeVisible();
    expect(container.querySelector(".bar i")).toHaveStyle({ width: "100%" });
  });

  it("forwards load failures to the shared error handler", async () => {
    const error = new Error("offline");
    const onError = vi.fn();
    mocks.resources.mockRejectedValue(error);
    await act(async () => render(<Catalogue token="token" run={run} onError={onError} busy={false} />));
    await waitFor(() => expect(onError).toHaveBeenCalledWith(error));
  });

  it("does not run disabled commands", async () => {
    mocks.resources.mockResolvedValue({ content: [resource()], totalElements: 1, totalPages: 1, number: 0 });
    render(<Catalogue token="token" run={run} onError={vi.fn()} busy />);
    const button = await screen.findByRole("button", { name: "Request item" });
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(mocks.requestLoan).not.toHaveBeenCalled();
  });
});
