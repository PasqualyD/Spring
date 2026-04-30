package com.example.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    /**
     * Activated when Railway injects DATABASE_URL in postgresql://user:pass@host:port/db format.
     * Converts it to JDBC format and extracts credentials, overriding Spring Boot's auto-configured
     * DataSource (which expects jdbc: prefix and separate username/password properties).
     */
    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSource() {
        String rawUrl = System.getenv("DATABASE_URL");

        // Convert postgresql:// to jdbc:postgresql://
        String jdbcUrl = rawUrl.replace("postgresql://", "jdbc:postgresql://");

        HikariConfig config = new HikariConfig();

        if (jdbcUrl.contains("@")) {
            // Format: jdbc:postgresql://user:pass@host:port/db
            String withoutScheme = jdbcUrl.replace("jdbc:postgresql://", "");
            String credentials = withoutScheme.substring(0, withoutScheme.indexOf("@"));
            String hostAndDb = withoutScheme.substring(withoutScheme.indexOf("@") + 1);
            String username = credentials.split(":")[0];
            String password = credentials.split(":")[1];
            config.setJdbcUrl("jdbc:postgresql://" + hostAndDb);
            config.setUsername(username);
            config.setPassword(password);
        } else {
            config.setJdbcUrl(jdbcUrl);
        }

        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }
}
