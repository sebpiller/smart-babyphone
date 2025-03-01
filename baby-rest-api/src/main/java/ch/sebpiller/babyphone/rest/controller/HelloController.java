package ch.sebpiller.babyphone.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "Bienvenue dans votre application Spring Boot!";
    }

}