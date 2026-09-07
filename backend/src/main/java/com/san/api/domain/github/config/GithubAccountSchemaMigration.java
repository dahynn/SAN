package com.san.api.domain.github.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * github_accounts.github_user_id에 남아 있는 기존 전역 unique 제약을 제거합니다.
 * 여러 서비스 사용자가 같은 GitHub 계정을 연동할 수 있고, user_id는 계속 고유해야 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GithubAccountSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> constraintNames = jdbcTemplate.queryForList("""
                SELECT con.conname
                FROM pg_constraint con
                JOIN pg_class rel ON rel.oid = con.conrelid
                WHERE con.contype = 'u'
                  AND rel.oid = 'github_accounts'::regclass
                  AND con.conkey = ARRAY[
                      (
                          SELECT att.attnum
                          FROM pg_attribute att
                          WHERE att.attrelid = rel.oid
                            AND att.attname = 'github_user_id'
                            AND NOT att.attisdropped
                      )
                  ]::smallint[]
                """, String.class);

        for (String constraintName : constraintNames) {
            jdbcTemplate.execute("ALTER TABLE github_accounts DROP CONSTRAINT " + quoteIdentifier(constraintName));
            log.info("[GitHub] dropped legacy unique constraint on github_user_id: {}", constraintName);
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
