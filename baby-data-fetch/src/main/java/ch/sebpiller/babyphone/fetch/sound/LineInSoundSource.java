package ch.sebpiller.babyphone.fetch.sound;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.time.Duration;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class LineInSoundSource implements SoundSource {

    @SneakyThrows
    @Override
    public File captureClip(Duration duration) {
        log.info("Starting audio capture for {} milliseconds", duration.toMillis());

        var outputFile = new File("baby_" + System.currentTimeMillis() + ".au");
        log.debug("Temporary output file created: {}", outputFile.getAbsolutePath());
        //outputFile.deleteOnExit();

        var format = getAudioFormat();
        log.debug("Audio format configured: {}", format);

        try (var line = AudioSystem.getTargetDataLine(format)) {
            line.open(format);
            line.start();
            log.info("Audio capture started");

            var ais = new AudioInputStream(line);

            var t = new Thread(() -> {
                try {
                    Thread.sleep(duration.toMillis());
                } catch (InterruptedException e) {
                    log.error("Audio capture thread interrupted", e);
                    throw new RuntimeException(e);
                }
                line.stop();
                log.info("Audio capture stopped");
            });
            t.start();

            var fileType = AudioFileFormat.Type.AU;
            AudioSystem.write(ais, fileType, outputFile);
            log.info("Audio data successfully written to file {}", outputFile.getAbsolutePath());
            t.join();
        } catch (LineUnavailableException ex) {
            log.error("Unable to access system line-in for audio capture", ex);
            throw new IOException("Unable to access system line-in for audio capture", ex);
        } catch (InterruptedException e) {
            log.error("Thread interrupted while capturing audio", e);
            throw new RuntimeException(e);
        }
        log.info("Audio capture completed, file saved at {}", outputFile.getAbsolutePath());
        return outputFile;
    }

    AudioFormat getAudioFormat() {
        float sampleRate = 22_050;
        var sampleSizeInBits = 32;
        var channels = 1;
        var signed = true;
        var bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
}
