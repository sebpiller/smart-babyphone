package ch.sebpiller.babyphone.detection.sound;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import ch.sebpiller.babyphone.detection.sound.torefacto.ImageUtils;
import ch.sebpiller.babyphone.detection.sound.torefacto.MelSpectrogram;
import ch.sebpiller.babyphone.detection.sound.torefacto.TensorUtils;
import ch.sebpiller.babyphone.detection.sound.torefacto.consts.MelSpectrogramDimension;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.proto.GraphDef;
import org.tensorflow.types.TFloat32;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Predicate;

@Slf4j
@Service
public class Cifar10AudioClassifier implements Closeable, SoundAnalyzer {

    public static final String[] labels = new String[]{
            "blues", "classical", "country", "disco", "hiphop", "jazz", "metal", "pop", "reggae", "rock"

    };

    private final Graph graph = new Graph();

    @PostConstruct
    void loadModel() throws IOException {
        graph.importGraphDef(GraphDef.parseFrom(getClass().getResourceAsStream("/cifar10.pb")));
    }


    @Override
    public void close() {
        graph.close();
    }

    @Override
    public DetectionResult detectObjectsOn(File sound, Predicate<Detected> includeInResult) {
        var image = MelSpectrogram.extractSpectrogramFromAudioFile(sound);

        if (image != null) {
            return analyzeHistogram(image);
        }

        return null;
    }

    DetectionResult analyzeHistogram(BufferedImage image) {
        var dr = DetectionResult.builder();
        var predicted = runModelOnImage(image, MelSpectrogramDimension.Width, MelSpectrogramDimension.Height);

        var d = new ArrayList<Detected>();
        for (var x = 0; x < predicted.length; x++) {
            d.add(new Detected(labels[x], predicted[x], 0, 0, 1, 1));
        }

        dr.detected(d);
        return dr.build();
    }

    private float[] runModelOnImage(BufferedImage image, int imgWidth, int imgHeight) {
        image = ImageUtils.resizeImage(image, imgWidth, imgHeight);
        var imageTensor = TensorUtils.getImageTensor(image, imgWidth, imgHeight);

        try (var sess = new Session(graph); var result = sess.runner()
                .feed("conv2d_1_input:0", imageTensor)
                //.feed("dropout_1/keras_learning_phase:0", Tensor.create(false))
                .fetch("output_node0:0").run();) {

            var res = (TFloat32) result.get(0);
            var rshape = res.shape();
            if (rshape.numDimensions() != 2 || rshape.asArray()[0] != 1) {
                throw new IllegalStateException("Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape " + rshape);
            }

            var array = result.get(0).shape().asArray();
            var ress = new float[array.length];
            var i = 0;

            for (var l : array) {
                ress[i++] = l;
            }

            return ress;
        }
    }
}
