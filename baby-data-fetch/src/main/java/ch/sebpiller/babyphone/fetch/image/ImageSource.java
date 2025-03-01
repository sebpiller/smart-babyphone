package ch.sebpiller.babyphone.fetch.image;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

@FunctionalInterface
public interface ImageSource extends Supplier<BufferedImage> {
    BufferedImage get();

}
