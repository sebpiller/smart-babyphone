package ch.sebpiller.babyphone.detection.sound;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tensorflow.Graph;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.op.Ops;
import org.tensorflow.proto.ConfigProto;
import org.tensorflow.types.TFloat32;

import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

@Lazy
@Slf4j
@Service
public class YamnetSoundAnalyzer implements SoundAnalyzer {

    public static final String MODELS = "/home/seb/models";
    public static final String HIGH_RES = MODELS + "/yamnet-tensorflow2-yamnet-v1";

    public static final String MODEL_PATH = HIGH_RES;
    private final SavedModelBundle model = SavedModelBundle.load(Objects.requireNonNull(Path.of(MODEL_PATH)).toString(), "serve");

    private final Graph graph = new Graph();
    private Ops ops;
    private Session session;

    @PostConstruct
    private void soundAnalyzer() {
        session = new Session(graph, ConfigProto.newBuilder().setAllowSoftPlacement(true).build());
        ops = Ops.create(graph);
    }


    @SneakyThrows
    @Override
    public DetectionResult detectObjectsOn(File sound, Predicate<Detected> includeInResult) {
        byte[] bytes = new FileInputStream(sound).readAllBytes();
        var ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
        var sr = ais.getFormat().getSampleRate();
        var channels = ais.getFormat().getChannels();
        var frameSize = ais.getFormat().getFrameSize();
        var frameLength = ais.getFrameLength();


        float[] decodedSound = decodeSoundData(bytes);
        var x = preprocessSoundData(decodedSound);
        return DetectionResult.builder().build();
        //return new YamnetModel(Path.of(MODEL_PATH)).predict(x);

//        try {
//            // Prepare the input tensor from sound data
//            TFloat32 inputTensor = preprocessSoundData(sound);
//
//            // Run the model
//            DetectionResult result;
//            try (var output = session.runner()
//                    .feed("input", inputTensor)
//                    .fetch("output")
//                    .run()) {
//
//                // Process the output tensor into a DetectionResult
//                result = processModelOutput(output.get(0));
//            }
//            log.info("Detection complete. Result: {}", result);
//            return result;
//
//        } catch (Exception e) {
//            log.error("Failed to detect objects from sound data", e);
//            throw new RuntimeException(e);
//        }
    }

    private float[] decodeSoundData(byte[] sound) {
        // Assuming 16-bit PCM encoding for decoding raw sound data
        float[] decodedSound = new float[sound.length / 2];
        for (int i = 0; i < sound.length; i += 2) {
            // Combine two bytes into a single 16-bit sample
            int sample = (sound[i] & 0xFF) | (sound[i + 1] << 8);
            // Normalize to the range [-1, 1]
            decodedSound[i / 2] = sample / 32768.0f;
        }
        return decodedSound;
    }

    private TFloat32 preprocessSoundData(float[] sound) {
        return TFloat32.vectorOf(sound);
    }

    private DetectionResult processModelOutput(Tensor output) {
        var result = model.function("serving_default").call(output);

        // Implementation for converting the TensorFlow output to a DetectionResult.
        // Placeholder: Replace this with actual postprocessing logic.
        return DetectionResult.builder().build();
    }
}

