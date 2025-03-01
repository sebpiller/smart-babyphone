package ch.sebpiller.babyphone.fetch.rtsp;

import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Lazy
@Slf4j
@Service
@RequiredArgsConstructor
public class RtspSoundSource implements SoundSource {
    private long start = 0;

    @Override
    public byte[] captureClip() {
        if (start == 0) {
            start = System.currentTimeMillis();
            return new byte[0];
        }

        //
        try (var x = getClass().getResourceAsStream("/samples/sounds/miaow_16k.wav")) {
            return x.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
