# ADR-003: Separate catalogue resources from lendable items

- Status: Accepted
- Date: 2026-09-19

## Context

The legacy `Exemplar` hierarchy combines bibliographic description and the single object being loaned. That model cannot represent several copies of the same book or several devices of the same model without duplicating catalogue data.

## Decision

`Resource` represents the shared catalogue description and classification. `ResourceItem` represents one independently tracked lendable unit with a unique asset tag, availability status and optimistic-lock version. A resource may own any number of items.

The API uses UUID identifiers, typed resource and item statuses, paginated catalogue queries, and filters for type, category and availability. The superseded exemplar endpoints were retired after the loan domain migration; their historical tables are preserved in PostgreSQL schema `legacy`.

## Consequences

Availability and concurrency can be enforced per physical item, while catalogue metadata is stored once. The extra entity and join add some query complexity, but they remove ambiguity and provide the correct boundary for maintenance, loss, reservations and concurrent lending.
