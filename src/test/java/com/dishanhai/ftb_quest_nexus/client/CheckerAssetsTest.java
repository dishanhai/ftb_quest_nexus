package com.dishanhai.ftb_quest_nexus.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

/** Author: dishanhai. Actual PNG frame sizes, six-face models and placement-state contracts. */
class CheckerAssetsTest {
    private static final String ROOT = "assets/ftb_quest_nexus/";
    private static final String PART = "block/machine/part/ftbq_ae_checker/overlay_";
    private static final int FRAMES = 8;

    @Test
    void allTexturesHaveEightNativeSixteenPixelFrames() throws IOException {
        Set<String> textures = new HashSet<>();
        for (String model : new String[]{"ftbq_ae_checker", "ftbq_ae_checker_active"}) {
            json("models/block/" + model + ".json").getAsJsonObject("textures").entrySet().forEach(entry -> {
                String texture = entry.getValue().getAsString();
                if (!texture.startsWith("#")) textures.add(texture.substring("ftb_quest_nexus:".length()));
            });
        }
        assertEquals(11, textures.size());
        for (String texture : textures) {
            BufferedImage image = texture(texture);
            assertEquals(16, image.getWidth(), texture);
            assertEquals(16 * FRAMES, image.getHeight(), texture);
            JsonObject animation = json("textures/" + texture + ".png.mcmeta").getAsJsonObject("animation");
            assertEquals(16, animation.get("width").getAsInt(), texture);
            assertEquals(16, animation.get("height").getAsInt(), texture);
            assertEquals(4, animation.get("frametime").getAsInt(), texture);
            assertFalse(animation.get("interpolate").getAsBoolean(), texture);
            for (int frame = 0; frame < FRAMES; frame++) {
                int changed = 0;
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int pixel = image.getRGB(x, y + 16 * frame);
                        assertTrue(pixel >>> 24 == 0 || pixel >>> 24 == 255, texture);
                        if (pixel != image.getRGB(x, y)) changed++;
                    }
                }
                assertTrue(changed <= 8, texture + " frame " + frame);
                if (!texture.contains("active") && !texture.endsWith("front_emissive")) {
                    assertEquals(0, changed, texture);
                }
            }
        }
    }

    @Test
    void everyFaceKeepsTheLightShellAndBroadVioletAccents() throws IOException {
        String[] faces = {"block/casings/ftbq_ae_checker_casing", "block/casings/ftbq_ae_checker_casing_active",
                PART + "front", PART + "front_active", PART + "back", PART + "back_active",
                PART + "top", PART + "top_active", PART + "bottom"};
        for (String face : faces) {
            BufferedImage image = texture(face);
            for (int frame = 0; frame < FRAMES; frame++) {
                int violetPixels = 0;
                int lightShellPixels = 0;
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int pixel = image.getRGB(x, y + frame * 16);
                        int red = pixel >> 16 & 255;
                        int green = pixel >> 8 & 255;
                        int blue = pixel & 255;
                        if (pixel >>> 24 == 255 && red > green + 25 && blue > red + 12) violetPixels++;
                        if (pixel >>> 24 == 255 && red >= 125 && blue >= red && blue - red <= 30) lightShellPixels++;
                    }
                }
                assertTrue(violetPixels >= 64, face + " needs a broad violet accent in frame " + frame);
                assertTrue(lightShellPixels >= 128, face + " needs a light shell in frame " + frame);
            }
        }
    }

    @Test
    void onlineAnimationPreservesTheShellAndEmitsOnlyMatchingIndicators() throws IOException {
        BufferedImage front = texture(PART + "front_active");
        BufferedImage offlineFront = texture(PART + "front");
        BufferedImage glow = texture(PART + "front_emissive");
        BufferedImage offlineGlow = texture(PART + "front_emissive_offline");
        int changedFrames = 0;
        for (int frame = 0; frame < FRAMES; frame++) {
            int lit = 0;
            boolean changed = false;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int stripY = y + frame * 16;
                    assertEquals(0, offlineGlow.getRGB(x, stripY) >>> 24);
                    if (x <= 2 || x >= 13 || y <= 2 || y >= 13) {
                        assertEquals(offlineFront.getRGB(x, stripY), front.getRGB(x, stripY));
                    }
                    if (glow.getRGB(x, stripY) >>> 24 != 0) {
                        assertEquals(front.getRGB(x, stripY), glow.getRGB(x, stripY));
                        lit++;
                    }
                    changed |= front.getRGB(x, stripY) != front.getRGB(x, y);
                }
            }
            assertEquals(5, lit);
            if (changed) changedFrames++;
        }
        assertTrue(changedFrames > 0, "Online strip must contain actual visual animation");
    }

    @Test
    void bothModelsCoverAllSixFacesAndOnlyTheOnlineModelSelectsEmissivePixels() throws IOException {
        JsonObject offline = json("models/block/ftbq_ae_checker.json");
        JsonObject online = json("models/block/ftbq_ae_checker_active.json");
        JsonObject faces = offline.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
        assertEquals(Set.of("north", "south", "east", "west", "up", "down"), faces.keySet());
        for (String face : faces.keySet()) {
            assertEquals(face, faces.getAsJsonObject(face).get("cullface").getAsString());
        }
        assertEquals("#front", faces.getAsJsonObject("north").get("texture").getAsString());
        assertEquals("#back", faces.getAsJsonObject("south").get("texture").getAsString());
        assertEquals("#side", faces.getAsJsonObject("east").get("texture").getAsString());
        assertEquals("#side", faces.getAsJsonObject("west").get("texture").getAsString());
        assertEquals("#top", faces.getAsJsonObject("up").get("texture").getAsString());
        assertEquals("#bottom", faces.getAsJsonObject("down").get("texture").getAsString());
        assertEquals("ftb_quest_nexus:block/ftbq_ae_checker", online.get("parent").getAsString());
        assertFalse(online.has("elements"));
        assertTrue(offline.getAsJsonObject("textures").get("glow").getAsString().endsWith("_offline"));
        assertTrue(online.getAsJsonObject("textures").get("glow").getAsString().endsWith("front_emissive"));
        JsonObject glow = offline.getAsJsonArray("elements").get(1).getAsJsonObject()
                .getAsJsonObject("faces").getAsJsonObject("north");
        JsonObject light = glow.getAsJsonObject(glow.has("forge_data") ? "forge_data" : "neoforge_data");
        assertEquals(15, light.get("block_light").getAsInt());
        assertFalse(light.get("ambient_occlusion").getAsBoolean());
        assertEquals("ftb_quest_nexus:block/ftbq_ae_checker",
                json("models/item/ftbq_ae_checker.json").get("parent").getAsString());
    }

    @Test
    void allTwelveStatesSelectTheMatchingModelAndOrientTheFront() throws IOException {
        JsonObject states = json("blockstates/ftbq_ae_checker.json").getAsJsonObject("variants");
        assertEquals(12, states.size());
        String[] directions = {"north", "south", "east", "west", "up", "down"};
        int[] x = {0, 0, 0, 0, 270, 90};
        int[] y = {0, 180, 90, 270, 0, 0};
        for (int direction = 0; direction < directions.length; direction++) {
            for (boolean online : new boolean[]{false, true}) {
                JsonObject variant = states.getAsJsonObject("facing=" + directions[direction] + ",online=" + online);
                assertNotNull(variant);
                assertEquals("ftb_quest_nexus:block/ftbq_ae_checker" + (online ? "_active" : ""),
                        variant.get("model").getAsString());
                assertEquals(x[direction], variant.has("x") ? variant.get("x").getAsInt() : 0);
                assertEquals(y[direction], variant.has("y") ? variant.get("y").getAsInt() : 0);
            }
        }
    }

    private InputStream resource(String path) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(ROOT + path);
        assertNotNull(stream, path);
        return stream;
    }

    private JsonObject json(String path) throws IOException {
        try (var reader = new InputStreamReader(resource(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private BufferedImage texture(String path) throws IOException {
        try (InputStream stream = resource("textures/" + path + ".png")) {
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path);
            return image;
        }
    }
}
