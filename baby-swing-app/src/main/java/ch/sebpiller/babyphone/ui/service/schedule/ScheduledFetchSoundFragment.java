package ch.sebpiller.babyphone.ui.service.schedule;


import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import ch.sebpiller.babyphone.toolkit.SoundUtils;
import ch.sebpiller.babyphone.ui.swing.MainController;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import ch.sebpiller.spi.toolkit.aop.Mdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
//@Lazy
@Slf4j
@Service
@AutoLog
@Mdc
public class ScheduledFetchSoundFragment {

    private final MainController mainController;
    private final SoundSource soundSource;

    @Scheduled(initialDelay = 10, fixedRate = 5, timeUnit = TimeUnit.SECONDS, scheduler = "taskScheduler")
    public void fetchSoundFragment() {
        var raw = soundSource.captureClip(Duration.ofSeconds(5), SoundUtils.getAudioFormat());
        mainController.receiveRawSound("source", raw, SoundUtils.getAudioFormat());
    }
}
