import type { LoanStatus } from "./types";

export const formatDate = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("en-GB", { dateStyle: "medium", timeStyle: "short" }).format(
        new Date(value),
      )
    : "—";

export const readable = (value: string) =>
  value
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (letter) => letter.toUpperCase());

export function staffAction(status: LoanStatus): { action: string; label: string } | undefined {
  if (status === "REQUESTED") return { action: "approve", label: "Approve" };
  if (status === "APPROVED") return { action: "collect", label: "Confirm collection" };
  if (status === "ACTIVE" || status === "OVERDUE") return { action: "return", label: "Mark returned" };
  return undefined;
}
