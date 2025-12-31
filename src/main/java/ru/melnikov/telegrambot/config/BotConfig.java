package ru.melnikov.telegrambot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
public class BotConfig {
    private String token;
    private String username;
    private boolean enabled = true;

    // Настройки прокси (опционально)
    private Proxy proxy = new Proxy();

    @Data
    public static class Proxy {
        private boolean enabled = false;
        private String host;
        private Integer port;
        private String type = "HTTP"; // HTTP, SOCKS4, SOCKS5
        private String username;
        private String password;
    }

    public boolean isProxyEnabled() {
        return proxy != null && proxy.isEnabled() &&
                proxy.getHost() != null && proxy.getPort() != null;
    }
}