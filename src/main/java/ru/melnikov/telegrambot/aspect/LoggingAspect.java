package ru.melnikov.telegrambot.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.melnikov.telegrambot.bot.context.CommandContext;
import ru.melnikov.telegrambot.service.CommandLogService;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private final CommandLogService commandLogService;

    /**
     * Точка среза для всех методов CommandService
     */
    @Pointcut("execution(* ru.melnikov.telegrambot.bot.CommandService.*(..))")
    public void commandServiceMethods() {}

    /**
     * Точка среза для всех методов контроллеров
     */
    @Pointcut("execution(* ru.melnikov.telegrambot.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * Логирование команд Telegram бота
     */
    @Around("commandServiceMethods() && args(context,..)")
    public Object logCommandExecution(ProceedingJoinPoint joinPoint, CommandContext context) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logCommand(context, methodName, exception, executionTime);
        }

        return result;
    }

    /**
     * Логирование HTTP запросов
     */
    @Around("controllerMethods()")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();

        long startTime = System.currentTimeMillis();
        log.info("🔄 Контроллер {}.{} начал выполнение", className, methodName);

        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            log.info("✅ Контроллер {}.{} выполнен успешно", className, methodName);
        } catch (Exception e) {
            exception = e;
            log.error("❌ Контроллер {}.{} завершился с ошибкой: {}",
                    className, methodName, e.getMessage(), e);
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("⏱️  Контроллер {}.{} выполнен за {} мс",
                    className, methodName, executionTime);
        }

        return result;
    }

    private void logCommand(CommandContext context, String methodName, Exception exception, long executionTime) {
        try {
            User user = context.getUser();
            Long userId = user != null ? user.getId() : null;
            String username = user != null ?
                    (user.getUserName() != null ? user.getUserName() : user.getFirstName()) :
                    "Unknown";

            String args = String.join(" ", context.getArgs());

            if (exception == null) {
                commandLogService.logSuccess(
                        userId,
                        username,
                        context.getChatId(),
                        methodName,
                        args,
                        executionTime
                );

                log.info("✅ Команда /{} выполнена пользователем {} ({} мс)",
                        methodName.toLowerCase(), username, executionTime);
            } else {
                commandLogService.logError(
                        userId,
                        username,
                        context.getChatId(),
                        methodName,
                        args,
                        exception.getMessage(),
                        executionTime
                );

                log.error("❌ Ошибка в команде /{} от пользователя {}: {} ({} мс)",
                        methodName.toLowerCase(), username, exception.getMessage(), executionTime);
            }
        } catch (Exception e) {
            log.warn("⚠️ Не удалось записать лог команды: {}", e.getMessage());
        }
    }
}