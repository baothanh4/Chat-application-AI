package org.example.chatapplication.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbFixRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // Drop the check constraint if it exists
            jdbcTemplate.execute("ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_message_type_check");
            log.info("Successfully dropped chat_messages_message_type_check constraint");
        } catch (Exception e) {
            log.warn("Failed to drop constraint or it doesn't exist: {}", e.getMessage());
        }
        
        try {
            // Just in case it has a different name, try to find and drop it
            jdbcTemplate.execute("DO $$ DECLARE r RECORD; BEGIN FOR r IN (SELECT conname FROM pg_constraint WHERE conrelid = 'chat_messages'::regclass AND contype = 'c') LOOP EXECUTE 'ALTER TABLE chat_messages DROP CONSTRAINT ' || quote_ident(r.conname); END LOOP; END $$;");
            log.info("Successfully dropped all check constraints on chat_messages");
        } catch (Exception e) {
            log.warn("Failed to drop other constraints: {}", e.getMessage());
        }
    }
}
