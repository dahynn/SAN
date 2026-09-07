package com.san.api.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchemaIndexInitializerTest {

    @Test
    void createsAuditLogSearchIndexes() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SchemaIndexInitializer initializer = new SchemaIndexInitializer(jdbcTemplate);

        initializer.run(new DefaultApplicationArguments());

        verify(jdbcTemplate).execute(contains("idx_outbox_events_status_next_attempt"));
        verify(jdbcTemplate).execute(contains("idx_outbox_events_event_type_created_at"));
        verify(jdbcTemplate).execute(contains("idx_outbox_events_aggregate"));
        verify(jdbcTemplate).execute(contains("idx_audit_log_events_event_domain_time"));
        verify(jdbcTemplate).execute(contains("idx_audit_log_events_outcome_time"));
        verify(jdbcTemplate).execute(contains("idx_audit_log_events_failure_reason_time"));
        verify(jdbcTemplate).execute(contains("idx_audit_log_events_target_time"));
        verify(jdbcTemplate).execute(contains("idx_audit_log_events_occurred_at"));
        verify(jdbcTemplate).execute(contains("integrity_hash"));
        verify(jdbcTemplate).execute(contains("actor_user_id uuid"));
        verify(jdbcTemplate).execute(contains("request_metadata jsonb"));
        verify(jdbcTemplate).execute(contains("started_at timestamp"));
        verify(jdbcTemplate).execute(contains("completed_at timestamp"));
    }
}
