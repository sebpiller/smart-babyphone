package ch.sebpiller.babyphone.rest.controller;

import ch.sebpiller.babyphone.sound.api.SoundApi;
import ch.sebpiller.babyphone.sound.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SoundController implements SoundApi {

    @Override
    public ResponseEntity<List<User>> analyzePost() {
        return null;
    }
}