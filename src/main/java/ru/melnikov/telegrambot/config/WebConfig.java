package ru.melnikov.telegrambot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(LocalDate.class, new org.springframework.format.datetime.standard.TemporalAccessorPrinter(DATE_FORMATTER), new org.springframework.format.datetime.standard.TemporalAccessorParser(DATE_FORMATTER));
        registry.addFormatterForFieldType(LocalDateTime.class, new org.springframework.format.datetime.standard.TemporalAccessorPrinter(DATE_TIME_FORMATTER), new org.springframework.format.datetime.standard.TemporalAccessorParser(DATE_TIME_FORMATTER));
    }
}
