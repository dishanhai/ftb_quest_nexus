// Author: dishanhai. Headless layout preview, using the checker's language resources.
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class CheckerUiPreview {
    private static final String GUI = "gui.ftb_quest_nexus.checker.";
    private static final Color TEXT = new Color(225, 235, 247);
    private static final Color MUTED = new Color(143, 168, 192);
    private static final Color COMPLETE = new Color(140, 214, 177);
    private final Map<String, String> language = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 9) throw new IllegalArgumentException("project output caption goal1 goal2 goal3 goal4 goal5 goal6");
        CheckerUiPreview preview = new CheckerUiPreview();
        String json = Files.readString(Path.of(args[0], "src/main/resources/assets/ftb_quest_nexus/lang/zh_cn.json"),
                StandardCharsets.UTF_8);
        Pattern pairs = Pattern.compile("\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
        var matcher = pairs.matcher(json);
        while (matcher.find()) preview.language.put(matcher.group(1), matcher.group(2).replace("\\\\n", "\\n"));
        preview.render(Path.of(args[1]), args);
    }

    private String translated(String key, Object... arguments) {
        String value = language.getOrDefault(key, key).replaceAll("§.", "");
        return value.formatted(arguments);
    }

    private String gui(String key, Object... arguments) {
        return translated(GUI + key, arguments);
    }

    private void render(Path output, String[] args) throws Exception {
        BufferedImage image = new BufferedImage(1080, 790, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(new GradientPaint(0, 0, new Color(13, 20, 31), 1080, 790, new Color(28, 36, 51)));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.translate(30, 30);
        graphics.scale(3, 3);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(new Font("Microsoft YaHei", Font.PLAIN, 9));
        graphics.setColor(new Color(14, 23, 38, 220));
        graphics.fillRect(0, 0, 340, 230);
        graphics.setColor(new Color(105, 142, 172));
        graphics.drawRect(0, 0, 339, 229);
        text(graphics, 10, 8, 320, translated("block.ftb_quest_nexus.ftbq_ae_checker"), TEXT);
        text(graphics, 10, 24, 320, gui("online"), COMPLETE);
        text(graphics, 10, 38, 320, gui("status", gui("waiting")), TEXT);
        text(graphics, 10, 52, 320, gui("player", "6e41c846-650f-4b22-9918-b393c31cd5b7"), MUTED);
        text(graphics, 10, 66, 320, gui("team", "30b66326-1d25-448c-a52b-3e39e890b91d"), MUTED);
        text(graphics, 10, 82, 320, gui("summary", 4, 2, 2), TEXT);
        text(graphics, 10, 94, 180, gui("goals"), MUTED);
        text(graphics, 204, 94, 126, gui("stock"), MUTED);
        graphics.setColor(new Color(9, 18, 32, 180));
        graphics.fillRect(8, 106, 324, 84);
        graphics.setColor(new Color(64, 88, 112));
        graphics.drawRect(8, 106, 323, 83);
        long[] available = {320, 3000, 4096, 1000, 96, 8};
        long[] required = {512, 5000, 1024, 1000, 128, 16};
        for (int i = 0; i < 6; i++) {
            boolean fluid = i == 1 || i == 3;
            Color color = available[i] >= required[i] ? COMPLETE : TEXT;
            text(graphics, 14, 106 + i * 14 + 1, 184,
                    gui(fluid ? "fluid_goal" : "item_goal", args[i + 3]), color);
            text(graphics, 204, 106 + i * 14 + 1, 122,
                    gui(fluid ? "fluid_amount" : "item_amount", available[i], required[i]), color);
        }
        int buttonWidth = (340 - 26) / 4;
        button(graphics, 10, 194, buttonWidth, gui("bind"), true);
        button(graphics, 14 + buttonWidth, 194, buttonWidth, gui("pause"), true);
        button(graphics, 18 + buttonWidth * 2, 194, buttonWidth, gui("previous"), false);
        button(graphics, 22 + buttonWidth * 3, 194, buttonWidth, gui("next"), true);
        String page = gui("page", 1, 3);
        text(graphics, (340 - graphics.getFontMetrics().stringWidth(page)) / 2, 218, 320, page, MUTED);
        graphics.dispose();
        Graphics2D caption = image.createGraphics();
        caption.setFont(new Font("Microsoft YaHei", Font.PLAIN, 20));
        caption.setColor(new Color(163, 183, 205));
        caption.drawString(args[2], 32, 762);
        caption.dispose();
        Files.createDirectories(output.toAbsolutePath().getParent());
        ImageIO.write(image, "png", output.toFile());
    }

    private void button(Graphics2D graphics, int x, int y, int width, String label, boolean active) {
        graphics.setColor(new Color(30, 41, 59));
        graphics.fillRoundRect(x, y, width, 20, 8, 8);
        graphics.setColor(new Color(active ? 70 : 43, active ? 90 : 56, active ? 111 : 73));
        graphics.drawRoundRect(x, y, width, 20, 8, 8);
        int labelWidth = graphics.getFontMetrics().stringWidth(label);
        text(graphics, x + (width - labelWidth) / 2, y + 3, width, label, active ? TEXT : MUTED.darker());
    }

    private void text(Graphics2D graphics, int x, int y, int width, String value, Color color) {
        var metrics = graphics.getFontMetrics();
        String displayed = value;
        while (!displayed.isEmpty() && metrics.stringWidth(displayed) > width) {
            displayed = displayed.substring(0, displayed.length() - 1);
        }
        graphics.setColor(color);
        graphics.drawString(displayed, x, y + 10);
    }
}

