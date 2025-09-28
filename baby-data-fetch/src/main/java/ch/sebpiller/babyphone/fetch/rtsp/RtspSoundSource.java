package ch.sebpiller.babyphone.fetch.rtsp;

import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFormat;
import java.time.Duration;


@Lazy
@Slf4j
@Service
@RequiredArgsConstructor
public class RtspSoundSource implements SoundSource {
    private final RtspStreamProperties streamProperties;

    @SneakyThrows
    @Override
    public byte[] captureClip(Duration duration, AudioFormat format) {
        return getClass().getResourceAsStream("/samples/sounds/miaow_16k.wav").readAllBytes();
    }
}
