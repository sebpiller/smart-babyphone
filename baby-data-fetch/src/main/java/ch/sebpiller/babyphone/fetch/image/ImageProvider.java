package ch.sebpiller.babyphone.fetch.image;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

@FunctionalInterface
public interface ImageProvider extends Supplier<BufferedImage> {
    BufferedImage get();

}
