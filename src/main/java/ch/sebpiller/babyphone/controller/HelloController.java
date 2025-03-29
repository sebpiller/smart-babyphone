package ch.sebpiller.babyphone.controller;

import ch.sebpiller.spi.toolkit.aop.AutoLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@AutoLog
@Slf4j
@RequiredArgsConstructor
@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "Bienvenue dans votre application Spring Boot!";
    }

}