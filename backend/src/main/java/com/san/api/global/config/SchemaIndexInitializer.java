package com.san.api.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaIndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_async_jobs_active_target_job_type
                    ON async_jobs (target_id, job_type)
                    WHERE status IN ('PENDING', 'PROCESSING')
                """);
        jdbcTemplate.execute("""
                ALTER TABLE async_jobs
                    ADD COLUMN IF NOT EXISTS actor_user_id uuid
                """);
        jdbcTemplate.execute("""
                ALTER TABLE async_jobs
                    ADD COLUMN IF NOT EXISTS trace_id varchar(100)
                """);
        jdbcTemplate.execute("""
                ALTER TABLE async_jobs
                    ADD COLUMN IF NOT EXISTS ip_address varchar(45)
                """);
        jdbcTemplate.execute("""
                ALTER TABLE async_jobs
                    ADD COLUMN IF NOT EXISTS user_agent text
                """);
        jdbcTemplate.execute("""
                ALTER TABLE async_jobs
                    ADD COLUMN IF NOT EXISTS requested_by_type varchar(30)
                """);
        jdbcTemplate.execute("""
                ALTER TABLE async_jobs
                    ADD COLUMN IF NOT EXISTS request_metadata jsonb
                """);
        jdbcTemplate.execute("""
                ALTER TABLE async_jobs
                    ADD COLUMN IF NOT EXISTS started_at timestamp
                """);
        jdbcTemplate.execute("""
                ALTER TABLE async_jobs
                    ADD COLUMN IF NOT EXISTS completed_at timestamp
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_scraps_active_user_source_hash
                    ON scraps (user_id, source_type, content_hash)
                    WHERE is_deleted = false
                      AND content_hash IS NOT NULL
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_outbox_events_status_next_attempt
                    ON outbox_events (status, next_attempt_at, created_at)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_outbox_events_event_type_created_at
                    ON outbox_events (event_type, created_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate
                    ON outbox_events (aggregate_type, aggregate_id)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_event_domain_time
                    ON audit_log_events (event_domain, occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_outcome_time
                    ON audit_log_events (outcome, occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_failure_reason_time
                    ON audit_log_events (failure_reason_code, occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_target_time
                    ON audit_log_events (target_type, target_id, occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_occurred_at
                    ON audit_log_events (occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                ALTER TABLE audit_log_events
                    ADD COLUMN IF NOT EXISTS integrity_hash varchar(64)
                """);
        jdbcTemplate.execute("""
                ALTER TABLE users
                    ADD COLUMN IF NOT EXISTS role varchar(20)
                """);
        jdbcTemplate.execute("""
                UPDATE users
                    SET role = 'USER'
                    WHERE role IS NULL
                """);
        jdbcTemplate.execute("""
                ALTER TABLE users
                    ALTER COLUMN role SET DEFAULT 'USER'
                """);
        jdbcTemplate.execute("""
                ALTER TABLE users
                    ALTER COLUMN role SET NOT NULL
                """);
    }
}
