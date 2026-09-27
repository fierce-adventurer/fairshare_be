package com.fairshare.shared.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/fairshare}") String rawUrl,
            @Value("${spring.datasource.username:fairshare}") String username,
            @Value("${spring.datasource.password:fairshare_dev}") String password,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName
    ) {
        String jdbcUrl = rawUrl;
        String finalUsername = username;
        String finalPassword = password;

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            jdbcUrl = "jdbc:postgresql://localhost:5432/fairshare";
        } else if (jdbcUrl.startsWith("postgres://") || jdbcUrl.startsWith("postgresql://")) {
            // Standard PostgreSQL URI format: postgresql://user:pass@host:port/dbname?sslmode=require
            try {
                String httpUrl = jdbcUrl.replaceFirst("^postgres(ql)?://", "http://");
                URI uri = new URI(httpUrl);

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String dbName = (path != null && path.startsWith("/")) ? path.substring(1) : (path != null ? path : "");
                String query = uri.getQuery();

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                }

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    if (parts.length > 0 && !parts[0].isBlank()) {
                        finalUsername = parts[0];
                    }
                    if (parts.length > 1 && !parts[1].isBlank()) {
                        finalPassword = parts[1];
                    }
                }
                log.info("Successfully converted cloud PostgreSQL URL to JDBC: host={}, port={}, db={}", host, port, dbName);
            } catch (Exception e) {
                log.warn("Could not parse cloud PostgreSQL URI components; prepending jdbc:: {}", e.getMessage());
                if (!jdbcUrl.startsWith("jdbc:")) {
                    jdbcUrl = "jdbc:" + jdbcUrl;
                }
            }
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        if (finalUsername != null && !finalUsername.isBlank()) {
            hikariConfig.setUsername(finalUsername);
        }
        if (finalPassword != null && !finalPassword.isBlank()) {
            hikariConfig.setPassword(finalPassword);
        }

        if (jdbcUrl.startsWith("jdbc:h2:")) {
            hikariConfig.setDriverClassName("org.h2.Driver");
        } else if (driverClassName != null && !driverClassName.isBlank()) {
            hikariConfig.setDriverClassName(driverClassName);
        }

        // Connection pool defaults suitable for low RAM and container memory limits
        int maxPool = 5;
        try {
            String poolEnv = System.getenv("DB_POOL_MAX_SIZE");
            if (poolEnv != null && !poolEnv.isBlank()) {
                maxPool = Integer.parseInt(poolEnv.trim());
            }
        } catch (Exception ignored) {}
        hikariConfig.setMaximumPoolSize(maxPool);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(20000);
        hikariConfig.setIdleTimeout(300000);
        hikariConfig.setMaxLifetime(1200000);

        return new HikariDataSource(hikariConfig);
    }
}
