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

/** Author: dishanhai. Pixel size, state lighting and localization contracts for both platforms. */
class SubmitterAssetsTest {
    private static final String ROOT = "assets/ftb_quest_nexus/";
    private static final String PART = "block/machine/part/ftbq_ae_submitter/overlay_";

    @Test
    void everyTextureUsesSixteenPixelFramesAndSmallAnimatedAccents() throws IOException {
        Set<String> textures = new HashSet<>();
        for (String model : new String[]{"ftbq_ae_submitter", "ftbq_ae_submitter_active"}) {
            json("models/block/" + model + ".json").getAsJsonObject("textures").entrySet().forEach(entry -> {
                String value = entry.getValue().getAsString();
                if (!value.startsWith("#")) textures.add(value.substring("ftb_quest_nexus:".length()));
            });
        }
        assertEquals(11, textures.size());
        for (String texture : textures) {
            BufferedImage image = texture(texture);
            assertEquals(16, image.getWidth(), texture);
            assertEquals(16 * 16, image.getHeight(), texture);
            JsonObject animation = json("textures/" + texture + ".png.mcmeta").getAsJsonObject("animation");
            assertEquals(16, animation.get("width").getAsInt(), texture);
            assertEquals(16, animation.get("height").getAsInt(), texture);
            assertEquals(4, animation.get("frametime").getAsInt(), texture);
            assertFalse(animation.get("interpolate").getAsBoolean(), texture);
            for (int frame = 0; frame < 16; frame++) {
                int changed = 0;
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int pixel = image.getRGB(x, y + frame * 16);
                        assertTrue(pixel >>> 24 == 0 || pixel >>> 24 == 255, texture);
                        if (pixel != image.getRGB(x, y)) changed++;
                    }
                }
                assertTrue(changed <= 12, texture + " frame " + frame);
                if (!texture.contains("active") && !texture.endsWith("front_emissive")) {
                    assertEquals(0, changed, texture);
                }
            }
        }
    }

    @Test
    void onlyOnlineIndicatorsEmitLightAndAlignWithTheFrontTexture() throws IOException {
        BufferedImage front = texture(PART + "front_active");
        BufferedImage glow = texture(PART + "front_emissive");
        BufferedImage offline = texture(PART + "front_emissive_offline");
        int lit = 0;
        for (int y = 0; y < glow.getHeight(); y++) {
            for (int x = 0; x < glow.getWidth(); x++) {
                assertEquals(0, offline.getRGB(x, y) >>> 24);
                if (glow.getRGB(x, y) >>> 24 != 0) {
                    assertEquals(front.getRGB(x, y), glow.getRGB(x, y));
                    lit++;
                }
            }
        }
        assertTrue(lit > 0);
        assertTrue(lit <= 12 * 16);
    }

    @Test
    void onlineModelSharesAllSixFacesAndAllTwelvePlacementStatesExist() throws IOException {
        JsonObject base = json("models/block/ftbq_ae_submitter.json");
        JsonObject active = json("models/block/ftbq_ae_submitter_active.json");
        assertEquals("ftb_quest_nexus:block/ftbq_ae_submitter", active.get("parent").getAsString());
        assertFalse(active.has("elements"));
        JsonObject faces = base.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
        assertEquals(Set.of("north", "south", "east", "west", "up", "down"), faces.keySet());
        JsonObject glowFace = base.getAsJsonArray("elements").get(1).getAsJsonObject()
                .getAsJsonObject("faces").getAsJsonObject("north");
        JsonObject light = glowFace.getAsJsonObject(glowFace.has("forge_data") ? "forge_data" : "neoforge_data");
        assertEquals(15, light.get("block_light").getAsInt());
        JsonObject states = json("blockstates/ftbq_ae_submitter.json").getAsJsonObject("variants");
        assertEquals(12, states.size());
        for (String facing : faces.keySet()) {
            for (boolean online : new boolean[]{false, true}) {
                String model = states.getAsJsonObject("facing=" + facing + ",online=" + online).get("model").getAsString();
                assertEquals("ftb_quest_nexus:block/ftbq_ae_submitter" + (online ? "_active" : ""), model);
            }
        }
    }

    @Test
    void bothLanguagesProvideAllItemAndButtonTooltipLines() throws IOException {
        JsonObject chinese = json("lang/zh_cn.json");
        JsonObject english = json("lang/en_us.json");
        assertEquals(chinese.keySet(), english.keySet());
        for (JsonObject language : new JsonObject[]{chinese, english}) {
            for (int line = 1; line <= 8; line++) {
                assertFalse(language.get("tooltip.ftb_quest_nexus.submitter." + line).getAsString().isBlank());
            }
            for (String button : new String[]{"bind", "clear", "toast", "claim"}) {
                String prefix = "gui.ftb_quest_nexus.submitter." + button;
                assertTrue(language.get(prefix + ".tooltip").getAsString().length()
                        > language.get(prefix).getAsString().length());
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

    private BufferedImage texture(String name) throws IOException {
        try (InputStream input = resource("textures/" + name + ".png")) {
            BufferedImage image = ImageIO.read(input);
            assertNotNull(image, name);
            return image;
        }
    }
}
