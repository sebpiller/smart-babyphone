package ch.sebpiller.babyphone.toolkit.tensorflow;

import lombok.experimental.UtilityClass;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.types.TFloat32;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

@UtilityClass
public class TensorUtils {
    public static TFloat32 getImageTensor(BufferedImage image, int imgWidth, int imgHeight) {
        var channels = 1;
        var fb = FloatBuffer.allocate(imgWidth * imgHeight * channels);

        for (var row = 0; row < imgHeight; row++) {
            for (var column = 0; column < imgWidth; column++) {
                var pixel = image.getRGB(column, row);

                float red = (pixel >> 16) & 0xff;

                float green = (pixel >> 8) & 0xff;
                float blue = pixel & 0xff;
                fb.put(red);
            }
        }

        return TFloat32.tensorOf(NdArrays.wrap(Shape.of(1L, imgHeight, imgWidth, channels), DataBuffers.of(fb.array())));
    }
}
