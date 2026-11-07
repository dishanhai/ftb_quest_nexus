// Author: dishanhai. Hand-authored 16 px AE textures and deterministic animation strips.
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntFunction;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataNode;

public final class GenerateSubmitterTextures {
    private static final int SIZE = 16;
    private static final int FRAMES = 16;
    private static final int EDGE = 0xFF828694;
    private static final int WHITE = 0xFFF5F6F8;
    private static final int PANEL = 0xFFDDE0E6;
    private static final int SHADOW = 0xFFB1B5C0;
    private static final int METAL = 0xFF9298A6;
    private static final int SCREEN = 0xFF343746;
    private static final int SCREEN_EDGE = 0xFF606576;
    private static final int VIOLET = 0xFF9C86BE;
    private static final int VIOLET_DIM = 0xFF706A88;
    private static final int CYAN_DIM = 0xFF657F8B;
    private static final int[] CYAN = {
            0xFF71B5C3, 0xFF80C8D3, 0xFF95DCE4, 0xFFB9F0F2,
            0xFFB9F0F2, 0xFF95DCE4, 0xFF80C8D3, 0xFF71B5C3
    };

    public static void main(String[] args) throws Exception {
        Path assets = Path.of(args[0]);
        String casing = "textures/block/casings/ftbq_ae_submitter_casing";
        String part = "textures/block/machine/part/ftbq_ae_submitter/overlay_";
        write(assets, casing, frame -> side(frame, false));
        write(assets, casing + "_active", frame -> side(frame, true));
        write(assets, part + "front", frame -> front(frame, false));
        write(assets, part + "front_active", frame -> front(frame, true));
        write(assets, part + "front_emissive", GenerateSubmitterTextures::glow);
        write(assets, part + "front_emissive_offline", frame -> image());
        write(assets, part + "back", frame -> back(frame, false));
        write(assets, part + "back_active", frame -> back(frame, true));
        write(assets, part + "top", frame -> top(frame, false));
        write(assets, part + "top_active", frame -> top(frame, true));
        write(assets, part + "bottom", frame -> bottom());
        Path previewFile = Path.of(args[1]);
        Files.createDirectories(previewFile.getParent());
        ImageIO.write(preview(7), "png", previewFile.toFile());
        animation(previewFile.resolveSibling("submitter-preview.gif"));
    }

    private static BufferedImage image() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static void rect(BufferedImage image, int x, int y, int width, int height, int color) {
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) {
                image.setRGB(column, row, color);
            }
        }
    }

    private static BufferedImage shell() {
        BufferedImage image = image();
        rect(image, 0, 0, 16, 16, EDGE);
        rect(image, 1, 1, 14, 14, PANEL);
        rect(image, 1, 0, 14, 1, WHITE);
        rect(image, 0, 1, 1, 14, WHITE);
        rect(image, 1, 1, 13, 1, WHITE);
        rect(image, 1, 2, 1, 10, WHITE);
        rect(image, 14, 1, 1, 14, SHADOW);
        rect(image, 1, 12, 13, 2, SHADOW);
        rect(image, 1, 14, 13, 1, METAL);
        return image;
    }

    private static int indicator(int frame, boolean active) {
        return active ? CYAN[frame / 2] : CYAN_DIM;
    }

    private static BufferedImage front(int frame, boolean active) {
        BufferedImage image = shell();
        rect(image, 3, 3, 10, 7, METAL);
        rect(image, 4, 4, 8, 5, SCREEN);
        rect(image, 4, 4, 8, 1, SCREEN_EDGE);
        rect(image, 5, 5, 2, 3, active ? PANEL : METAL);
        image.setRGB(5, 7, active ? VIOLET : VIOLET_DIM);
        int color = indicator(frame, active);
        rect(image, 8, 6, 3, 1, color);
        image.setRGB(9, 5, color);
        image.setRGB(9, 7, color);
        rect(image, 8, 8, 3, 1, color);
        if (active) image.setRGB(8 + frame / 4, 6, WHITE);
        rect(image, 4, 12, 5, 1, SCREEN_EDGE);
        image.setRGB(11, 11, color);
        return image;
    }

    private static BufferedImage side(int frame, boolean active) {
        BufferedImage image = shell();
        rect(image, 3, 4, 10, 6, SHADOW);
        rect(image, 4, 5, 8, 4, PANEL);
        rect(image, 5, 6, 1, 2, METAL);
        rect(image, 7, 6, 1, 2, METAL);
        rect(image, 9, 6, 1, 2, METAL);
        rect(image, 3, 12, 7, 1, SCREEN_EDGE);
        image.setRGB(11, 12, indicator(frame, active));
        image.setRGB(12, 3, VIOLET_DIM);
        return image;
    }

    private static BufferedImage back(int frame, boolean active) {
        BufferedImage image = shell();
        rect(image, 4, 3, 8, 8, METAL);
        rect(image, 5, 4, 6, 6, SCREEN);
        rect(image, 6, 5, 4, 4, active ? VIOLET : VIOLET_DIM);
        rect(image, 7, 6, 2, 2, SHADOW);
        rect(image, 6, 11, 4, 1, SCREEN_EDGE);
        image.setRGB(active ? 7 + frame / 4 : 7, 11, indicator(frame, active));
        return image;
    }

    private static BufferedImage top(int frame, boolean active) {
        BufferedImage image = shell();
        rect(image, 1, 12, 13, 2, PANEL);
        rect(image, 3, 3, 10, 10, SHADOW);
        rect(image, 4, 4, 8, 8, WHITE);
        rect(image, 5, 5, 6, 6, METAL);
        rect(image, 6, 6, 4, 4, VIOLET_DIM);
        rect(image, 7, 7, 2, 2, active ? VIOLET : SHADOW);
        image.setRGB(11, 4, indicator(frame, active));
        return image;
    }

    private static BufferedImage bottom() {
        BufferedImage image = shell();
        rect(image, 3, 3, 10, 8, SHADOW);
        rect(image, 5, 5, 6, 4, SCREEN_EDGE);
        rect(image, 6, 6, 4, 2, METAL);
        image.setRGB(3, 3, SCREEN_EDGE);
        image.setRGB(12, 3, SCREEN_EDGE);
        image.setRGB(3, 10, SCREEN_EDGE);
        image.setRGB(12, 10, SCREEN_EDGE);
        return image;
    }

    private static BufferedImage glow(int frame) {
        BufferedImage source = front(frame, true);
        BufferedImage image = image();
        int color = indicator(frame, true);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (source.getRGB(x, y) == color) image.setRGB(x, y, color);
            }
        }
        return image;
    }

    private static void write(Path assets, String name, IntFunction<BufferedImage> frame) throws Exception {
        BufferedImage strip = new BufferedImage(SIZE, SIZE * FRAMES, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = strip.createGraphics();
        for (int index = 0; index < FRAMES; index++) graphics.drawImage(frame.apply(index), 0, index * SIZE, null);
        graphics.dispose();
        Path file = assets.resolve(name + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(strip, "png", file.toFile());
        Files.writeString(assets.resolve(name + ".png.mcmeta"), """
                {
                  "animation": {
                    "frametime": 4,
                    "interpolate": false,
                    "width": 16,
                    "height": 16
                  }
                }
                """);
    }

    private static BufferedImage preview(int frame) {
        BufferedImage preview = new BufferedImage(720, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = preview.createGraphics();
        graphics.setColor(new Color(0x171A24));
        graphics.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int state = 0; state < 2; state++) {
            boolean active = state == 1;
            int offset = state * 360;
            graphics.drawImage(top(frame, active), new AffineTransform(6, 3, -6, 3, 180 + offset, 16), null);
            graphics.drawImage(front(frame, active), new AffineTransform(6, 3, 0, 7, 84 + offset, 64), null);
            graphics.drawImage(side(frame, active), new AffineTransform(-6, 3, 0, 7, 276 + offset, 64), null);
            BufferedImage[] faces = {front(frame, active), side(frame, active), back(frame, active), top(frame, active), bottom()};
            for (int face = 0; face < faces.length; face++) {
                graphics.drawImage(faces[face], 40 + face * 132, 268 + state * 120, 112, 112, null);
            }
        }
        graphics.dispose();
        return preview;
    }

    private static void animation(Path file) throws Exception {
        var writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (var output = ImageIO.createImageOutputStream(file.toFile())) {
            writer.setOutput(output);
            writer.prepareWriteSequence(null);
            for (int frame = 0; frame < FRAMES; frame++) {
                var type = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
                var metadata = writer.getDefaultImageMetadata(type, writer.getDefaultWriteParam());
                var root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
                var control = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("delayTime", "20");
                if (frame == 0) {
                    var extensions = new IIOMetadataNode("ApplicationExtensions");
                    var loop = new IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID", "NETSCAPE");
                    loop.setAttribute("authenticationCode", "2.0");
                    loop.setUserObject(new byte[]{1, 0, 0});
                    extensions.appendChild(loop);
                    root.appendChild(extensions);
                }
                metadata.setFromTree("javax_imageio_gif_image_1.0", root);
                writer.writeToSequence(new IIOImage(preview(frame), null, metadata), null);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }
}
