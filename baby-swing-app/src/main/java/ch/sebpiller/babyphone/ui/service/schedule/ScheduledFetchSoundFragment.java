package ch.sebpiller.babyphone.ui.service.schedule;


import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import ch.sebpiller.babyphone.toolkit.SoundUtils;
import ch.sebpiller.babyphone.ui.swing.MainController;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class ScheduledFetchSoundFragment {

    private final MainController mainController;
    private final SoundSource soundSource;

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    public void fetchSoundFragment() {
        var raw = soundSource.captureClip(Duration.ofSeconds(3), SoundUtils.getAudioFormat());
        mainController.receiveRawSound("source", raw, SoundUtils.getAudioFormat());
    }
}
