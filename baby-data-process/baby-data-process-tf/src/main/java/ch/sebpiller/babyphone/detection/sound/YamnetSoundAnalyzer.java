package ch.sebpiller.babyphone.detection.sound;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import ch.sebpiller.babyphone.toolkit.tensorflow.BaseTensorFlowRunnerFacade;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.ndarray.impl.dense.FloatDenseNdArray;
import org.tensorflow.types.TFloat32;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@Lazy
@Slf4j
@Service
@AutoLog
public class YamnetSoundAnalyzer extends BaseTensorFlowRunnerFacade implements SoundAnalyzer, Closeable, AutoCloseable {

    public static final String MODELS = "/home/seb/models";
    public static final String HIGH_RES = MODELS + "/yamnet-tensorflow2-yamnet-v1";

    public static final String MODEL_PATH = HIGH_RES;
    private final SavedModelBundle model = SavedModelBundle.load(Objects.requireNonNull(Path.of(MODEL_PATH)).toString(), "serve");
    private final Object[] csvData;

    public YamnetSoundAnalyzer() {
        super(null);
        csvData = loadCsvFromClasspath("yamnet_class_map.csv");

    }

    @SneakyThrows
    @Override
    public DetectionResult detectObjectsOn(byte[] sound, AudioFormat format, Predicate<Detected> includeInResult) {
        var decodedSound = decodeSoundData(sound);
        var inputTensor = preprocessSoundData(decodedSound);
        var build = DetectionResult.builder();

        try (var izgi = model.call(Map.of("waveform", inputTensor));) {

            var tensor1 = (FloatDenseNdArray) izgi.get("output_0").orElseThrow();
            var array = tensor1.shape().asArray();

            var xxx = new ArrayList<Detected>();
            for (var i = 0; i < array[1]; i++) {
                var type = csvData[i + 1];

                var sum = 0f;
                for (var j = 0; j < array[0]; j++) {
                    var object = tensor1.getObject(j, i);
                    sum += object;
                }
                var detected = new Detected(type.toString(), sum / array[0], 0, 0, 0, 0);
                if (includeInResult.test(detected)) {
                    xxx.add(detected);
                }
            }

            build.detected(xxx);
        }

        return build.build();
    }

    private float[] decodeSoundData(byte[] sound) {
        // Assuming 16-bit PCM encoding for decoding raw sound data
        var decodedSound = new float[sound.length / 2];
        for (var i = 0; i < sound.length - 1; i += 2) {
            // Combine two bytes into a single 16-bit sample
            var sample = (short) ((sound[i] & 0xFF) | (sound[i + 1] << 8));
            // Normalize to the range [-1, 1]
            decodedSound[i / 2] = sample / 32768.0f;
        }
        return decodedSound;
    }

    private TFloat32 preprocessSoundData(float[] sound) {
        return TFloat32.vectorOf(sound);
    }

    private Object[] loadCsvFromClasspath(String filename) {
        try (var inputStream = getClass().getClassLoader().getResourceAsStream(filename);
             var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            return reader.lines().toArray();
        } catch (Exception e) {
            log.error("Failed to load CSV file: {}", filename, e);
            throw new IllegalStateException("Could not load CSV file", e);
        }
    }
}

