// Файл: /src/main/java/ru/melnikov/telegrambot/controller/TestWeekController.java
package ru.melnikov.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.melnikov.telegrambot.service.WeekTypeService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestWeekController {

    private final WeekTypeService weekTypeService; // Используем сервис вместо конфига

    @GetMapping("/api/test/week-type")
    public Map<String, Object> testWeekType(
            @RequestParam(required = false) String date) {

        Map<String, Object> response = new HashMap<>();

        try {
            LocalDate testDate = date != null ?
                    LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")) :
                    LocalDate.now();

            // Получаем информацию о конфигурации через сервис
            String configurationInfo = weekTypeService.getConfigurationInfo();

            // Результаты расчета через сервис
            String weekType = weekTypeService.getWeekTypeForDate(testDate);

            response.put("result", Map.of(
                    "date", testDate.toString(),
                    "weekType", weekType,
                    "weekTypeName", weekTypeService.getWeekTypeDisplayName(weekType),
                    "isEven", weekTypeService.isEvenWeek(testDate),
                    "isOdd", weekTypeService.isOddWeek(testDate),
                    "emoji", weekTypeService.getWeekTypeEmoji(weekType),
                    "formatted", String.format("%s %s",
                            weekTypeService.getWeekTypeEmoji(weekType),
                            weekTypeService.getWeekTypeDisplayName(weekType))
            ));

            response.put("configurationInfo", configurationInfo);
            response.put("success", true);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("success", false);
            log.error("Ошибка тестирования недели: ", e);
        }

        return response;
    }
}