// Author: dishanhai. Native 16 x 16 pixel textures for the ME FTB quest checker.
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

public final class GenerateCheckerTextures {
    private static final int SIZE = 16;
    private static final int FRAMES = 8;
    private static final int EDGE = 0xFF818593;
    private static final int WHITE = 0xFFF4F5F8;
    private static final int PANEL = 0xFFDCE0E7;
    private static final int SHADOW = 0xFFB1B5C1;
    private static final int METAL = 0xFF9399A8;
    private static final int RIM = 0xFF9952D0;
    private static final int STRIPE = 0xFFAC57DD;
    private static final int BEZEL = 0xFF6E329C;
    private static final int SCREEN = 0xFF482062;
    private static final int MARK = 0xFFEEDBFF;
    private static final int VIOLET_DIM = 0xFF9660BE;
    private static final int ACCENT_DIM = 0xFF75408F;
    private static final int[] ACCENT = {
            0xFFA45BD4, 0xFFB36BE4, 0xFFC47DF0, 0xFFD28FFC,
            0xFFD28FFC, 0xFFC47DF0, 0xFFB36BE4, 0xFFA45BD4
    };
    private static final int[] VIOLET = {
            0xFF9558AF, 0xFFA563BD, 0xFFB26FC8, 0xFFBE7DD2,
            0xFFBE7DD2, 0xFFB26FC8, 0xFFA563BD, 0xFF9558AF
    };
    private static final String PART = "textures/block/machine/part/ftbq_ae_checker/overlay_";

    public static void main(String[] args) throws Exception {
        Path assets = Path.of(args[0]);
        write(assets, "textures/block/casings/ftbq_ae_checker_casing", frame -> side(frame, false));
        write(assets, "textures/block/casings/ftbq_ae_checker_casing_active", frame -> side(frame, true));
        write(assets, PART + "front", frame -> front(frame, false));
        write(assets, PART + "front_active", frame -> front(frame, true));
        write(assets, PART + "back", frame -> back(frame, false));
        write(assets, PART + "back_active", frame -> back(frame, true));
        write(assets, PART + "top", frame -> top(frame, false));
        write(assets, PART + "top_active", frame -> top(frame, true));
        write(assets, PART + "bottom", frame -> bottom());
        write(assets, PART + "front_emissive", GenerateCheckerTextures::glow);
        write(assets, PART + "front_emissive_offline", frame -> image());
        writeModels(assets);
        Path previews = Path.of(args[1]);
        Files.createDirectories(previews);
        ImageIO.write(preview(3), "png", previews.resolve("checker-preview.png").toFile());
        ImageIO.write(frameSequence(), "png", previews.resolve("checker-frame-sequence.png").toFile());
        animation(previews.resolve("checker-preview.gif"));
    }

    private static BufferedImage image() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static void rect(BufferedImage image, int x, int y, int width, int height, int color) {
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) image.setRGB(column, row, color);
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
        return active ? ACCENT[frame] : ACCENT_DIM;
    }

    private static void checkMark(BufferedImage image, int x, int y) {
        // Two-pixel strokes keep the check visible after the cube is scaled to a GUI slot.
        rect(image, x, y + 2, 2, 2, MARK);
        rect(image, x + 1, y + 3, 2, 2, MARK);
        rect(image, x + 2, y + 4, 2, 2, MARK);
        rect(image, x + 3, y + 3, 2, 2, MARK);
        rect(image, x + 4, y + 2, 2, 2, MARK);
        rect(image, x + 5, y + 1, 2, 2, MARK);
        rect(image, x + 6, y, 2, 2, MARK);
    }

    private static BufferedImage front(int frame, boolean active) {
        BufferedImage image = shell();
        rect(image, 3, 3, 10, 9, RIM);
        rect(image, 4, 4, 8, 7, SCREEN);
        checkMark(image, 4, 4);
        rect(image, 5, 11, 5, 1, active ? ACCENT[frame] : SCREEN);
        rect(image, 3, 12, 10, 2, BEZEL);
        return image;
    }

    private static BufferedImage side(int frame, boolean active) {
        BufferedImage image = shell();
        rect(image, 3, 3, 10, 8, BEZEL);
        rect(image, 4, 4, 8, 2, STRIPE);
        rect(image, 4, 8, 8, 2, STRIPE);
        rect(image, 3, 12, 10, 2, BEZEL);
        image.setRGB(11, 12, active ? ACCENT[frame] : STRIPE);
        return image;
    }

    private static BufferedImage back(int frame, boolean active) {
        BufferedImage image = shell();
        rect(image, 4, 3, 8, 8, RIM);
        rect(image, 5, 4, 6, 6, SCREEN);
        rect(image, 6, 5, 4, 4, VIOLET_DIM);
        rect(image, 7, 6, 2, 2, active ? VIOLET[frame] : SHADOW);
        rect(image, 3, 12, 10, 2, BEZEL);
        image.setRGB(7, 11, indicator(frame, active));
        return image;
    }

    private static BufferedImage top(int frame, boolean active) {
        BufferedImage image = shell();
        rect(image, 1, 12, 13, 2, PANEL);
        rect(image, 3, 3, 10, 10, RIM);
        rect(image, 4, 4, 8, 8, SCREEN);
        checkMark(image, 4, 5);
        rect(image, 2, 2, 2, 2, STRIPE);
        rect(image, 12, 12, 2, 2, STRIPE);
        image.setRGB(11, 11, active ? ACCENT[frame] : STRIPE);
        return image;
    }

    private static BufferedImage bottom() {
        BufferedImage image = shell();
        rect(image, 3, 3, 10, 8, BEZEL);
        rect(image, 5, 5, 6, 4, SCREEN);
        rect(image, 6, 6, 4, 2, VIOLET_DIM);
        image.setRGB(3, 3, RIM);
        image.setRGB(12, 3, RIM);
        image.setRGB(3, 10, RIM);
        image.setRGB(12, 10, RIM);
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

    private static void writeModels(Path assets) throws Exception {
        Path models = assets.resolve("models/block");
        Files.createDirectories(models);
        Files.writeString(models.resolve("ftbq_ae_checker.json"), """
                {
                  "__comment": "Author: dishanhai. Six native 16 px faces with state-specific indicator lighting.",
                  "parent": "minecraft:block/block",
                  "render_type": "minecraft:cutout",
                  "textures": {
                    "particle": "#side",
                    "side": "ftb_quest_nexus:block/casings/ftbq_ae_checker_casing",
                    "front": "ftb_quest_nexus:block/machine/part/ftbq_ae_checker/overlay_front",
                    "back": "ftb_quest_nexus:block/machine/part/ftbq_ae_checker/overlay_back",
                    "top": "ftb_quest_nexus:block/machine/part/ftbq_ae_checker/overlay_top",
                    "bottom": "ftb_quest_nexus:block/machine/part/ftbq_ae_checker/overlay_bottom",
                    "glow": "ftb_quest_nexus:block/machine/part/ftbq_ae_checker/overlay_front_emissive_offline"
                  },
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [16, 16, 16],
                      "faces": {
                        "north": {"texture": "#front", "cullface": "north"},
                        "south": {"texture": "#back", "cullface": "south"},
                        "east": {"texture": "#side", "uv": [16, 0, 0, 16], "cullface": "east"},
                        "west": {"texture": "#side", "cullface": "west"},
                        "up": {"texture": "#top", "cullface": "up"},
                        "down": {"texture": "#bottom", "cullface": "down"}
                      }
                    },
                    {
                      "from": [0, 0, -0.001],
                      "to": [16, 16, -0.001],
                      "shade": false,
                      "faces": {
                        "north": {
                          "texture": "#glow",
                          "uv": [0, 0, 16, 16],
                          "cullface": "north",
                          "forge_data": {"block_light": 15, "sky_light": 15, "ambient_occlusion": false}
                        }
                      }
                    }
                  ]
                }
                """);
        Files.writeString(models.resolve("ftbq_ae_checker_active.json"), """
                {
                  "__comment": "Author: dishanhai. Online state uses the same six-face geometry.",
                  "parent": "ftb_quest_nexus:block/ftbq_ae_checker",
                  "textures": {
                    "side": "ftb_quest_nexus:block/casings/ftbq_ae_checker_casing_active",
                    "front": "ftb_quest_nexus:block/machine/part/ftbq_ae_checker/overlay_front_active",
                    "back": "ftb_quest_nexus:block/machine/part/ftbq_ae_checker/overlay_back_active",
                    "top": "ftb_quest_nexus:block/machine/part/ftbq_ae_checker/overlay_top_active",
                    "glow": "ftb_quest_nexus:block/machine/part/ftbq_ae_checker/overlay_front_emissive"
                  }
                }
                """);
        Path items = assets.resolve("models/item");
        Files.createDirectories(items);
        Files.writeString(items.resolve("ftbq_ae_checker.json"), """
                {
                  "__comment": "Author: dishanhai.",
                  "parent": "ftb_quest_nexus:block/ftbq_ae_checker"
                }
                """);
        Path states = assets.resolve("blockstates");
        Files.createDirectories(states);
        StringBuilder variants = new StringBuilder("{\n  \"variants\": {\n");
        String[] directions = {"north", "south", "east", "west", "up", "down"};
        int[] xRotation = {0, 0, 0, 0, 270, 90};
        int[] yRotation = {0, 180, 90, 270, 0, 0};
        for (int direction = 0; direction < directions.length; direction++) {
            for (int online = 0; online < 2; online++) {
                variants.append("    \"facing=").append(directions[direction]).append(",online=")
                        .append(online == 1).append("\": {\"model\": \"ftb_quest_nexus:block/ftbq_ae_checker")
                        .append(online == 1 ? "_active" : "").append("\"");
                if (xRotation[direction] != 0) variants.append(", \"x\": ").append(xRotation[direction]);
                if (yRotation[direction] != 0) variants.append(", \"y\": ").append(yRotation[direction]);
                variants.append("}").append(direction == 5 && online == 1 ? "\n" : ",\n");
            }
        }
        variants.append("  }\n}\n");
        Files.writeString(states.resolve("ftbq_ae_checker.json"), variants);
    }

    private static BufferedImage[] faces(int frame, boolean active) {
        return new BufferedImage[]{front(frame, active), side(frame, active), side(frame, active),
                back(frame, active), top(frame, active), bottom()};
    }

    private static Graphics2D previewGraphics(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(0x171A24));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        return graphics;
    }

    private static BufferedImage preview(int frame) {
        BufferedImage preview = new BufferedImage(864, 560, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = previewGraphics(preview);
        for (int state = 0; state < 2; state++) {
            boolean active = state == 1;
            int offset = state * 424;
            graphics.drawImage(top(frame, active), new AffineTransform(6, 3, -6, 3, 220 + offset, 22), null);
            graphics.drawImage(front(frame, active), new AffineTransform(6, 3, 0, 7, 124 + offset, 70), null);
            graphics.drawImage(side(frame, active), new AffineTransform(-6, 3, 0, 7, 316 + offset, 70), null);
            BufferedImage[] faces = faces(frame, active);
            for (int face = 0; face < faces.length; face++) {
                graphics.drawImage(faces[face], 32 + face * 136, 278 + state * 128, 112, 112, null);
            }
        }
        graphics.dispose();
        return preview;
    }

    private static BufferedImage frameSequence() {
        BufferedImage preview = new BufferedImage(768, 576, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = previewGraphics(preview);
        for (int frame = 0; frame < FRAMES; frame++) {
            BufferedImage[] faces = faces(frame, true);
            for (int face = 0; face < faces.length; face++) {
                graphics.drawImage(faces[face], frame * 96, face * 96, 96, 96, null);
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
