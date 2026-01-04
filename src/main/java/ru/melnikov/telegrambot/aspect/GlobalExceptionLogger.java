package ru.melnikov.telegrambot.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class GlobalExceptionLogger {

    /**
     * Логирование всех непойманных исключений
     */
    @Pointcut("execution(* ru.melnikov.telegrambot..*.*(..))")
    public void allMethods() {}

    @AfterThrowing(pointcut = "allMethods()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.error("""
                🚨 НЕОБРАБОТАННОЕ ИСКЛЮЧЕНИЕ
                Класс: {}
                Метод: {}
                Тип исключения: {}
                Сообщение: {}
                Stack trace:""",
                className, methodName, ex.getClass().getName(), ex.getMessage());

        log.error("Stack trace:", ex);
    }
}