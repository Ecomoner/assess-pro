package com.frist.assesspro.config.metrics;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                return Health.up()
                        .withDetail("dev_postgres", "assess_pro_db")
                        .withDetail("status", "connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("dev_postgres", "assess_pro_db")
                        .withDetail("error", "Unexpected result")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("dev_postgres", "assess_pro_db")
                    .build();
        }
    }
}
