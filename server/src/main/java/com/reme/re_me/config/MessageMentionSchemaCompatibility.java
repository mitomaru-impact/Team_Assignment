package com.reme.re_me.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageMentionSchemaCompatibility implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public MessageMentionSchemaCompatibility(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Boolean legacyPartnerIsRequired = jdbcTemplate.query(
                """
                SELECT is_nullable = 'NO'
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'conversation_mutes'
                  AND column_name = 'partner_id'
                """,
                resultSet -> resultSet.next() ? resultSet.getBoolean(1) : null);
        if (Boolean.TRUE.equals(legacyPartnerIsRequired)) {
            jdbcTemplate.execute(
                    "ALTER TABLE conversation_mutes ALTER COLUMN partner_id DROP NOT NULL");
        }
        Boolean legacyPartnerColumnExists = jdbcTemplate.query(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = 'conversation_mutes'
                      AND column_name = 'partner_user_id'
                )
                """,
                resultSet -> resultSet.next() ? resultSet.getBoolean(1) : null);
        if (Boolean.TRUE.equals(legacyPartnerColumnExists)) {
            jdbcTemplate.execute(
                    """
                    UPDATE conversation_mutes
                    SET partner_id = partner_user_id
                    WHERE partner_id IS NULL AND partner_user_id IS NOT NULL
                    """);
        }

        Boolean legacyColumnIsRequired = jdbcTemplate.query(
                """
                SELECT is_nullable = 'NO'
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'message_mentions'
                  AND column_name = 'group_message'
                """,
                resultSet -> resultSet.next() ? resultSet.getBoolean(1) : null);
        if (Boolean.TRUE.equals(legacyColumnIsRequired)) {
            jdbcTemplate.execute(
                    "ALTER TABLE message_mentions ALTER COLUMN group_message DROP NOT NULL");
        }

        Boolean legacyUserIdIsRequired = jdbcTemplate.query(
                """
                SELECT is_nullable = 'NO'
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'message_mentions'
                  AND column_name = 'user_id'
                """,
                resultSet -> resultSet.next() ? resultSet.getBoolean(1) : null);
        if (Boolean.TRUE.equals(legacyUserIdIsRequired)) {
            jdbcTemplate.execute(
                    "ALTER TABLE message_mentions ALTER COLUMN user_id DROP NOT NULL");
        }
    }
}
