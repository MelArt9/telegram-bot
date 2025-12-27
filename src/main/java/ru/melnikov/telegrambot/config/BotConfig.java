package ru.melnikov.telegrambot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
public class BotConfig {
    /**
     * Bot username registered via BotFather.
     */
    private String username;

    /**
     * Bot token issued by BotFather.
     */
    private String token;

    /**
     * Optional admin chat id for diagnostics.
     */
    private Long adminChatId;
}
