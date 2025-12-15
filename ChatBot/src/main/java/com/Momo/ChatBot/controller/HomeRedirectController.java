package com.Momo.ChatBot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
public class HomeRedirectController {

    @GetMapping("/")
    public Mono<String> redirectToChatPage() {
        // Redirige la URL raíz a tu archivo estático
        return Mono.just("redirect:/index.html");
    }
}