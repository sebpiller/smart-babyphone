package ch.sebpiller.babyphone.fetch.rtsp;

import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;


@Lazy
@Slf4j
@Service
@RequiredArgsConstructor
public class RtspSoundSource implements SoundSource {
    private long start = 0;

    @SneakyThrows
    @Override
    public File captureClip(Duration duration) {
        if (start == 0) {
            start = System.currentTimeMillis();
            return null;
        }

        return new File(getClass().getResource("/samples/sounds/miaow_16k.wav").toURI());
    }
}
