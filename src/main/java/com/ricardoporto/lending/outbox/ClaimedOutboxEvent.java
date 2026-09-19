package com.ricardoporto.lending.outbox;

import java.util.UUID;

public record ClaimedOutboxEvent(OutboxEvent event, UUID claimToken) {}
