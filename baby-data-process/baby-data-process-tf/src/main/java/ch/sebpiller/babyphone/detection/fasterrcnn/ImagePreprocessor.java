package ch.sebpiller.babyphone.detection.fasterrcnn;

import lombok.experimental.UtilityClass;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.types.TFloat32;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

@UtilityClass
class ImagePreprocessor {

    public static TFloat32 preprocess(List<BufferedImage> sourceImages, int imageHeight, int imageWidth, int imageChannels) {
        Shape imageShape = Shape.of(sourceImages.size(), imageHeight, imageWidth, imageChannels);

        return TFloat32.tensorOf(imageShape, tensor -> {

            // Copy all images to the tensor
            for (int imageIdx = 0; imageIdx < sourceImages.size(); imageIdx++) {
                BufferedImage sourceImage = sourceImages.get(imageIdx);

                // Scale the image to required dimensions if needed
                BufferedImage image = (sourceImage.getWidth() != imageWidth || sourceImage.getHeight() != imageHeight) ?
                        scaleImage(sourceImage, imageWidth, imageHeight) : sourceImage;

                // Converts the image to floats and normalize by subtracting mean values
                int i = 0;
                for (long h = 0; h < imageHeight; h++) {
                    for (long w = 0; w < imageWidth; w++) {
                        // "caffe"-style normalization
                        tensor.setFloat(getElemFloat(image, i++) - 103.939f, imageIdx, h, w, 0);
                        tensor.setFloat(getElemFloat(image, i++) - 116.779f, imageIdx, h, w, 1);
                        tensor.setFloat(getElemFloat(image, i++) - 123.68f, imageIdx, h, w, 2);
                    }
                }
            }
        });
    }

    private static BufferedImage scaleImage(BufferedImage sourceImage, int imageWidth, int imageHeight) {
        BufferedImage scaledImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
        var graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );
        graphics.drawImage(sourceImage, 0, 0, imageWidth, imageHeight, null);
        graphics.dispose();
        return scaledImage;
    }

    private static float getElemFloat(BufferedImage image, int index) {
        return image.getData().getDataBuffer().getElemFloat(index);
    }
}