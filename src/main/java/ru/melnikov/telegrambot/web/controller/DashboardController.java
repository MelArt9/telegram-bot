package ru.melnikov.telegrambot.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    //
    @GetMapping
    public String dashboard() {
        return "dashboard";
    }
}
