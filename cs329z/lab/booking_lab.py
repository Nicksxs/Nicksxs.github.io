"""A read-only, standard-library lab for the CS329Z study notes."""

import argparse
import json
from dataclasses import dataclass
from datetime import date
from typing import Optional


@dataclass(frozen=True)
class Policy:
    ref: str
    campus: str
    equipment: str
    starts: date
    ends: Optional[date]
    audience: str
    clarification: str


POLICIES = (
    Policy("N-07", "north", "audio-mic", date(2026, 1, 1),
           date(2026, 10, 1), "student", "仅限器材室内使用"),
    Policy("N-12", "north", "audio-mic", date(2026, 10, 1),
           None, "student", "校内借用须先确认归还时间"),
    Policy("S-06", "south", "audio-mic", date(2026, 1, 1),
           None, "student", "按南校区流程登记后在本楼使用"),
    Policy("N-90", "north", "mix-table", date(2026, 1, 1),
           None, "staff", "虚构的内部工作流程"),
)


def request(day="2026-10-01", campus="north", equipment="audio-mic",
            action="lookup_policy", **extra):
    return dict(action=action, campus=campus, equipment=equipment, day=day, **extra)


# Each row is a synthetic task and independently specified expected result.
# The second expected field is the ID that must support a successful answer.
CASES = {
    "current": (request(), ("found", "N-12")),
    "history": (request(day="2026-09-30"), ("found", "N-07")),
    "south": (request(campus="south"), ("found", "S-06")),
    "before": (request(day="2025-12-31"), ("no_evidence", None)),
    "staff_only": (request(equipment="mix-table"), ("no_evidence", None)),
    "unknown": (request(equipment="tripod"), ("no_evidence", None)),
    "missing_day": (request(day=None), ("ask_date", None)),
    "invalid_day": (request(day="2026-09-31"), ("invalid_date", None)),
    "write_request": (request(action="reserve"), ("unsupported_action", None)),
    "extra_field": (request(quantity=2), ("invalid_fields", None)),
}


def parse_day(value):
    if not isinstance(value, str):
        return None
    try:
        day = date.fromisoformat(value)
    except ValueError:
        return None
    return day if value == day.isoformat() else None


def result(status, evidence=None, **details):
    return {"status": status, "evidence": evidence, **details}


def public_matches(query):
    # The synthetic caller is always a student. Tool parameters never change it.
    return tuple(
        row for row in POLICIES
        if row.audience == "student" and row.equipment == query.get("equipment")
    )


def baseline(query):
    """Lesson 01: return the first public entry by device, as a dry run."""
    if query.get("day") is None:
        return result("ask_date")
    matches = public_matches(query)
    if not matches:
        return result("no_evidence")
    row = matches[0]
    return result("found", row.ref, clarification=row.clarification)


def retrieval(query):
    """Lesson 03: filter readable evidence by site and effective interval."""
    if query.get("day") is None:
        return result("ask_date")
    day = parse_day(query["day"])
    if day is None:
        # This is an evidence filter, without a distinct bad-request status.
        return result("no_evidence")
    matches = [
        row for row in public_matches(query)
        if row.campus == query.get("campus")
        and row.starts <= day and (row.ends is None or day < row.ends)
    ]
    if not matches:
        return result("no_evidence")
    if len(matches) > 1:
        return result("conflicting_evidence")
    row = matches[0]
    return result("found", row.ref, clarification=row.clarification)


def validate(query):
    if set(query) != {"action", "campus", "equipment", "day"}:
        return "invalid_fields"
    if query["action"] != "lookup_policy":
        return "unsupported_action"
    if query["campus"] not in ("north", "south"):
        return "invalid_fields"
    if not isinstance(query["equipment"], str) or not query["equipment"].strip():
        return "invalid_fields"
    if query["day"] is None:
        return "ask_date"
    if parse_day(query["day"]) is None:
        return "invalid_date"
    return None


def tools(query):
    """Lesson 04: validate a proposal, then call the read-only lookup."""
    trace = [{"event": "proposed", "request": dict(query)}]
    reason = validate(query)
    if reason is not None:
        trace.append({"event": "rejected", "status": reason})
        return result(reason, trace=trace)
    trace.append({"event": "accepted", "tool": "lookup_policy"})
    found = retrieval(query)
    trace.append({
        "event": "observed", "status": found["status"],
        "evidence": found["evidence"],
    })
    return {**found, "trace": trace}


STAGES = {"baseline": baseline, "retrieval": retrieval, "tools": tools}


def evaluate():
    """Lesson 13: reuse the same tasks and report which checks fail."""
    for label, run in STAGES.items():
        failures = []
        for name, (query, expected) in CASES.items():
            answer = run(query)
            actual = answer["status"], answer["evidence"]
            if actual != expected:
                failures.append(name)
        print(f"{label}: {len(CASES) - len(failures)}/{len(CASES)}")
        if failures:
            print("  fails:", ", ".join(failures))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stage", choices=(*STAGES, "evaluate"), nargs="?",
                        default="evaluate")
    parser.add_argument("--case", choices=CASES)
    args = parser.parse_args()
    if args.stage == "evaluate":
        if args.case is not None:
            parser.error("evaluate always uses the full case set")
        evaluate()
    else:
        query, _ = CASES[args.case or "current"]
        print(json.dumps(STAGES[args.stage](query), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
