package ch.sebpiller.babyphone.fetch.sound;

import ch.sebpiller.spi.toolkit.aop.AutoLog;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.time.Duration;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
@AutoLog
public class LineInSoundSource implements SoundSource {

    @SneakyThrows
    @Override
    public byte[] captureClip(Duration duration, AudioFormat format) {
        try (var line = AudioSystem.getTargetDataLine(format)) {
            line.open(format);
            line.start();
            log.info("Audio capture started");

            var ais = new AudioInputStream(line);

            var t = new Thread(() -> {
                try {
                    Thread.sleep(duration.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                line.stop();
                log.info("Audio capture stopped");
            });
            t.start();

            var fileType = AudioFileFormat.Type.AU;
            var out = new ByteArrayOutputStream();
            AudioSystem.write(ais, fileType, out);

            t.join();
            return out.toByteArray();
        } catch (LineUnavailableException ex) {
            throw new IllegalStateException("Unable to access system line-in for audio capture", ex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
