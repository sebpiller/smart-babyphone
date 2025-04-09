package ch.sebpiller.babyphone.ui;

import ch.sebpiller.babyphone.ui.config.BabyphoneConfiguration;
import ch.sebpiller.babyphone.ui.swing.MainController;
import ch.sebpiller.babyphone.ui.swing.MainFrame;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

import javax.swing.*;

@SpringBootApplication
//@ComponentScan(basePackageClasses = BabyPhoneSwingUI.class)
@Import(BabyphoneConfiguration.class)
public class BabyPhoneSwingUI {

    static {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                     UnsupportedLookAndFeelException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public static void main(String[] args) {
        var context2 = new SpringApplicationBuilder(BabyPhoneSwingUI.class)
                .headless(false)
                .build(args);

        context2.setBanner((environment, sourceClass, out) -> {
            out.println("Starting BabyPhone Swing UI");
            out.println("  Version: ");
            out.println("    " + environment.getProperty("version"));
            out.println("  Build: ");
            out.println("    " + environment.getProperty("build"));
            out.println("  Build time: ");
            out.println("    " + environment.getProperty("build.time"));
            out.println("  Build JDK: ");
            out.println("    " + environment.getProperty("build.jdk"));
            out.println("  Build JDK version: ");
            out.println("    " + environment.getProperty("build.jdk.version"));
            out.println("  Build JDK vendor: ");
        });

        var context = context2.run(args);
        var appFrame = context.getBean(MainFrame.class);
        var ctrl = context.getBean(MainController.class);

        ctrl.setDetecteds(appFrame::reannotateImage);
        ctrl.setDetectedSounds(appFrame::receiveSound);
        ctrl.setFps(appFrame::setFps);
        SwingUtilities.invokeLater(() -> appFrame.setVisible(true));
    }

}