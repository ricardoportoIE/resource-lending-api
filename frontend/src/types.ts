export type Role = "STUDENT" | "STAFF" | "ADMIN";
export type LoanStatus =
  | "REQUESTED"
  | "APPROVED"
  | "ACTIVE"
  | "OVERDUE"
  | "RETURNED"
  | "REJECTED"
  | "CANCELLED";

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface CurrentUser {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: Role[];
}

export interface ResourceItem {
  id: string;
  assetTag: string;
  status: "AVAILABLE" | "RESERVED" | "LOANED" | "MAINTENANCE" | "RETIRED";
  version: number;
  updatedAt: string;
}

export interface Resource {
  id: string;
  name: string;
  description: string;
  type: string;
  category: string;
  identifier: string;
  loanable: boolean;
  items: ResourceItem[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export interface Loan {
  id: string;
  borrowerId: number;
  borrowerEmail: string;
  resourceItemId: string;
  assetTag: string;
  status: LoanStatus;
  requestedAt: string;
  dueAt?: string;
}

export interface Reservation {
  id: string;
  resourceId: string;
  resourceName: string;
  status: "WAITING" | "READY" | "FULFILLED" | "CANCELLED" | "EXPIRED";
  queuePosition: number;
  createdAt: string;
  expiresAt?: string;
}

export interface DashboardSummary {
  activeLoans: number;
  overdueLoans: number;
  returnedLoans: number;
  unavailableItems: number;
  maintenanceItems: number;
  averageReservationWaitSeconds: number;
  reservationWaitSampleSize: number;
  generatedAt: string;
}

export interface Utilization {
  resourceId: string;
  resourceName: string;
  totalItems: number;
  onLoanItems: number;
  unavailableItems: number;
  currentUtilizationPercent: number;
}
