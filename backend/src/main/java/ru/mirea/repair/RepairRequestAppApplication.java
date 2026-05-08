package ru.mirea.repair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class RepairRequestAppApplication {
    public static void main(String[] args) {
        configureDatabaseUrlForCloudHosting();
        SpringApplication.run(RepairRequestAppApplication.class, args);
    }

    private static void configureDatabaseUrlForCloudHosting() {
        String springUrl = System.getenv("SPRING_DATASOURCE_URL");
        String databaseUrl = System.getenv("DATABASE_URL");
        if ((springUrl == null || springUrl.isBlank()) && databaseUrl != null && !databaseUrl.isBlank()) {
            URI uri = URI.create(databaseUrl.replace("postgres://", "postgresql://"));
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] credentials = userInfo.split(":", 2);
                String username = URLDecoder.decode(credentials[0], StandardCharsets.UTF_8);
                String password = URLDecoder.decode(credentials[1], StandardCharsets.UTF_8);
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
                System.setProperty("spring.datasource.url", jdbcUrl);
                System.setProperty("spring.datasource.username", username);
                System.setProperty("spring.datasource.password", password);
            }
        }
    }
}
