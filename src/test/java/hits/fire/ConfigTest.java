package hits.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
    @TempDir
    Path dir;

    @Test
    void defaults() {
        Config c = new Config();
        assertTrue(c.fire.on);
        assertTrue(c.soul.on);
        assertEquals(Config.FIRE, c.fire.color);
        assertEquals(Config.SOUL, c.soul.color);
        assertEquals(3f, c.fire.radius);
        assertEquals(1f, c.soul.strength);
        assertEquals(Anim.FLICKER, c.anim);
        assertTrue(c.blocks && c.camps && c.mobs);
        assertFalse(c.torches);
        assertFalse(c.overlay);
        assertTrue(c.recolor);
        assertFalse(c.hand);
    }

    @Test
    void saveLoad() {
        Path file = dir.resolve("glow.json");
        Config.load(file);
        assertTrue(Files.exists(file));
        Config.get().soul.color = 0x3FA9FF;
        Config.get().anim = Anim.SHIMMER;
        Config.get().camps = false;
        Config.get().recolor = false;
        Config.get().hand = true;
        Config.save();
        Config.set(new Config());
        Config.load(file);
        assertEquals(0x3FA9FF, Config.get().soul.color);
        assertEquals(Anim.SHIMMER, Config.get().anim);
        assertFalse(Config.get().camps);
        assertFalse(Config.get().recolor);
        assertTrue(Config.get().hand);
    }

    @Test
    void corrupt() throws IOException {
        Path file = dir.resolve("bad.json");
        Files.writeString(file, "{ this is not json ]");
        Config.load(file);
        assertTrue(new Config().same(Config.get()));
    }

    @Test
    void clamps() throws IOException {
        Path file = dir.resolve("wild.json");
        Files.writeString(file, "{\"fire\":{\"on\":true,\"color\":-1,\"radius\":99,\"strength\":-4,\"falloff\":0},\"soul\":null,\"speed\":1000,\"inner\":-3,\"anim\":\"NOPE\"}");
        Config.load(file);
        Config c = Config.get();
        assertEquals(0xFFFFFF, c.fire.color);
        assertEquals(10f, c.fire.radius);
        assertEquals(0f, c.fire.strength);
        assertEquals(0.25f, c.fire.falloff);
        assertEquals(4f, c.speed);
        assertEquals(0f, c.inner);
        assertNotNull(c.anim);
        assertNotNull(c.soul);
        assertEquals(Config.SOUL, c.soul.color);
    }

    @Test
    void copy() {
        Config a = new Config();
        Config b = a.copy();
        b.fire.color = 0x123456;
        assertEquals(Config.FIRE, a.fire.color);
        assertFalse(a.same(b));
    }

    @Test
    void live() {
        Config c = new Config();
        assertTrue(c.live());
        c.fire.on = false;
        c.soul.strength = 0f;
        assertFalse(c.live());
        c.soul.strength = 1f;
        assertTrue(c.live());
        c.enabled = false;
        assertFalse(c.live());
    }

    @Test
    void any() {
        Config c = new Config();
        assertTrue(c.any());
        c.blocks = c.camps = c.torches = c.mobs = c.overlay = false;
        assertFalse(c.any());
        c.overlay = true;
        assertTrue(c.any());
    }
}
