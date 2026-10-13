package hits.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ColorsTest {
    @Test
    void hex() {
        for (int c : new int[]{0x000000, 0xFFFFFF, 0xFF7A1F, 0x3FD8FF, 0x123456, 0xABCDEF}) {
            assertEquals(c, Colors.parse(Colors.hex(c), -1));
        }
    }

    @Test
    void parse() {
        assertEquals(0xFFAA00, Colors.parse("#fa0", -1));
        assertEquals(0xFFAA00, Colors.parse("  FFAA00 ", -1));
        assertEquals(-1, Colors.parse("#12345", -1));
        assertEquals(-1, Colors.parse("zzzzzz", -1));
        assertEquals(-1, Colors.parse(null, -1));
        assertEquals(-1, Colors.parse("", -1));
    }

    @Test
    void hsv() {
        for (int c = 0; c < 0xFFFFFF; c += 0x010F37) {
            float[] v = Colors.hsv(c);
            int back = Colors.fromHsv(v[0], v[1], v[2]);
            assertTrue(Math.abs(Colors.r(c) - Colors.r(back)) <= 1, Colors.hex(c));
            assertTrue(Math.abs(Colors.g(c) - Colors.g(back)) <= 1, Colors.hex(c));
            assertTrue(Math.abs(Colors.b(c) - Colors.b(back)) <= 1, Colors.hex(c));
        }
    }

    @Test
    void hueWrap() {
        int red = 0xFF0000;
        assertEquals(red, Colors.shift(red, 1f));
        assertEquals(red, Colors.shift(red, -3f));
        float[] v = Colors.hsv(Colors.shift(red, 1f / 3f));
        assertEquals(1f / 3f, v[0], 0.01f);
    }

    @Test
    void mix() {
        assertEquals(0xFF000000, Colors.mix(0xFF000000, 0xFFFFFFFF, -1f));
        assertEquals(0xFFFFFFFF, Colors.mix(0xFF000000, 0xFFFFFFFF, 2f));
        assertEquals(0x80FFFFFF, Colors.alpha(0xFFFFFFFF, 128 / 255f) & 0xFFFFFFFF);
        assertEquals(0x00123456, Colors.alpha(0xFF123456, 0f));
        assertEquals(0.0f, Colors.luma(0), 1e-6f);
        assertEquals(1.0f, Colors.luma(0xFFFFFF), 1e-3f);
    }
}
