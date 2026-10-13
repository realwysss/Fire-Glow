package hits.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LookTest {
    @Test
    void staticLook() {
        Config c = new Config();
        c.anim = Anim.STATIC;
        c.soul.strength = 2f;
        Look l = Look.of(c, 12.3);
        assertEquals(0x3F / 255f * 2f * Look.GAIN, l.soul()[0], 1e-4f);
        assertEquals(0xD8 / 255f * 2f * Look.GAIN, l.soul()[1], 1e-4f);
        assertEquals(1f * 2f * Look.GAIN, l.soul()[2], 1e-4f);
        assertEquals(c.soul.falloff, l.soul()[3]);
        assertEquals(1f * Look.GAIN, l.fire()[0], 1e-4f);
        assertEquals(0f, l.shimmer());
    }

    @Test
    void offIsDark() {
        Config c = new Config();
        c.soul.on = false;
        Look l = Look.of(c, 0);
        assertEquals(0f, l.soul()[0] + l.soul()[1] + l.soul()[2]);
        assertFalse(l.dark());
        c.fire.on = false;
        assertTrue(Look.of(c, 0).dark());
    }

    @Test
    void animRange() {
        Config c = new Config();
        for (Anim a : Anim.values()) {
            c.anim = a;
            for (double t = 0; t < 30; t += 0.037) {
                Look l = Look.of(c, t);
                for (float[] v : new float[][]{l.fire(), l.soul()}) {
                    for (float x : v) assertTrue(Float.isFinite(x) && x >= 0f, a + " " + t);
                    float sum = v[0] + v[1] + v[2];
                    assertTrue(sum <= 3.001f * c.fire.strength * Look.GAIN, a + " too bright at " + t);
                    assertTrue(sum >= 0.2f, a + " went dark at " + t);
                }
            }
        }
    }

    @Test
    void noise() {
        float prev = Look.noise(0f);
        for (float x = 0; x < 50; x += 0.01f) {
            float n = Look.noise(x);
            assertTrue(n >= 0f && n <= 1f);
            assertTrue(Math.abs(n - prev) < 0.2f);
            prev = n;
        }
    }

    @Test
    void recolor() {
        Config c = new Config();
        c.recolor = false;
        Look l = Look.of(c, 0);
        assertEquals(0f, l.fireTint()[3]);
        assertEquals(0f, l.soulTint()[3]);
        c.recolor = true;
        c.anim = Anim.STATIC;
        l = Look.of(c, 0);
        assertEquals(1f, l.fireTint()[3]);
        assertEquals(1f, l.soulTint()[3]);
        assertEquals(1f, l.fireTint()[0], 1e-4f);
        c.anim = Anim.RAINBOW;
        float[] a = Look.of(c, 0.5).fireTint();
        float[] b = Look.of(c, 3.0).fireTint();
        assertTrue(Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]) > 0.05f);
        c.fire.strength = 0f;
        c.soul.on = false;
        l = Look.of(c, 0);
        assertEquals(0f, l.fireTint()[3]);
        assertTrue(l.dark());
    }

    @Test
    void tintOnly() {
        Config c = new Config();
        c.recolor = true;
        c.fire.strength = 0.01f;
        c.soul.on = false;
        assertFalse(Look.of(c, 0).dark());
    }
}
