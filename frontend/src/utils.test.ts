import { describe, expect, it } from "vitest";
import { readable, staffAction } from "./utils";

describe("operational presentation", () => {
  it("maps workflow states only to valid primary staff actions", () => {
    expect(staffAction("REQUESTED")).toEqual({ action: "approve", label: "Approve" });
    expect(staffAction("APPROVED")?.action).toBe("collect");
    expect(staffAction("OVERDUE")?.action).toBe("return");
    expect(staffAction("RETURNED")).toBeUndefined();
  });

  it("renders API enum labels for people", () => {
    expect(readable("IN_MAINTENANCE")).toBe("In maintenance");
  });
});
