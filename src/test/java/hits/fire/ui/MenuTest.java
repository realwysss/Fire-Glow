package hits.fire.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hits.fire.Anim;
import hits.fire.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MenuTest {
    private record Mark(String kind, String text, float x, float y, float w, float h, int argb) {
        double cx() {
            return x + w / 2.0;
        }

        double cy() {
            return y + h / 2.0;
        }
    }

    private static final class Rec implements Canvas {
        final List<Mark> marks = new ArrayList<>();
        boolean soul;

        public void fill(float x, float y, float w, float h, float r, int top, int bottom) {
            marks.add(new Mark("fill", null, x, y, w, h, top));
        }

        public void ring(float x, float y, float w, float h, float r, float t, int top, int bottom) {
            marks.add(new Mark("ring", null, x, y, w, h, top));
        }

        public void shadow(float x, float y, float w, float h, float r, float blur, int argb) {
        }

        public void hue(float x, float y, float w, float h, float r, int argb) {
            marks.add(new Mark("hue", null, x, y, w, h, argb));
        }

        public void sv(float x, float y, float w, float h, float r, int argb) {
            marks.add(new Mark("sv", null, x, y, w, h, argb));
        }

        public void text(String s, float x, float y, int argb) {
            marks.add(new Mark("text", s, x, y, width(s), 8, argb));
        }

        public int width(String s) {
            return s.length() * 6;
        }

        public void flame(float x, float y, float size, boolean soul) {
            this.soul = soul;
            marks.add(new Mark("flame", null, x, y, size, size, -1));
        }

        Mark find(String kind, String text) {
            for (Mark m : marks) {
                if (m.kind.equals(kind) && (text == null || text.equals(m.text))) return m;
            }
            throw new AssertionError("missing " + kind + " " + text);
        }
    }

    private Menu menu;
    private Rec rec;
    private AtomicInteger closed;
    private String clip;

    @BeforeEach
    void setUp() {
        Config.set(new Config());
        closed = new AtomicInteger();
        clip = null;
        menu = new Menu(closed::incrementAndGet, () -> clip);
        frame(480, 270);
    }

    private void frame(int w, int h) {
        rec = new Rec();
        menu.draw(rec, w, h, -100, -100);
    }

    private void click(Mark m) {
        assertTrue(menu.click(m.cx(), m.cy(), 0));
        menu.release(m.cx(), m.cy(), 0);
        frame(480, 270);
    }

    private void recolor(String hex) {
        click(rec.find("text", "#" + Integer.toHexString(Config.get().fire.color).toUpperCase()));
        for (int i = 0; i < 6; i++) menu.key(Menu.BACK);
        for (char c : hex.toCharArray()) menu.type(c);
        menu.key(Menu.ENTER);
        assertEquals(Integer.parseInt(hex, 16), Config.get().fire.color);
        frame(480, 270);
    }

    @Test
    void tabs() {
        rec.find("text", "Fire glow");
        assertFalse(rec.soul);
        click(rec.find("text", "Soul"));
        rec.find("text", "Soul glow");
        assertTrue(rec.soul);
        click(rec.find("text", "Effects"));
        rec.find("text", "Animation");
        rec.find("text", "Recolor flames");
        click(rec.find("text", "Show"));
        rec.find("text", "Screen fire");
        rec.find("text", "Torches and lanterns");
        assertTrue(menu.key(Menu.TAB));
        frame(480, 270);
        rec.find("text", "Fire glow");
    }

    @Test
    void soul() {
        assertTrue(Config.get().soul.on);
        click(rec.find("text", "Soul"));
        click(rec.find("text", "Soul glow"));
        assertFalse(Config.get().soul.on);
        click(rec.find("text", "#3FD8FF"));
        for (int i = 0; i < 6; i++) menu.key(Menu.BACK);
        for (char c : "8844FF".toCharArray()) menu.type(c);
        assertEquals(0x8844FF, Config.get().soul.color);
        assertEquals(Config.FIRE, Config.get().fire.color);
    }

    @Test
    void backdrops() {
        Config before = Config.get().copy();
        for (Mark m : rec.marks) {
            if (m.kind().equals("fill") && m.w() == 8 && (m.argb() & 0xFFFFFF) == 0x2E4A66) {
                click(m);
                break;
            }
        }
        assertTrue(before.same(Config.get()));
    }

    @Test
    void slider() {
        Mark label = rec.find("text", "Radius");
        float trackY = label.y() + 15;
        double left = label.x();
        assertTrue(menu.click(left + 1, trackY, 0));
        menu.release(left + 1, trackY, 0);
        assertEquals(0f, Config.get().fire.radius, 0.11f);
        double right = left + 400;
        menu.click(left + 1, trackY, 0);
        menu.dragged(right, trackY);
        menu.release(right, trackY, 0);
        assertEquals(10f, Config.get().fire.radius, 0.001f);
        frame(480, 270);
        float before = Config.get().fire.radius;
        assertTrue(menu.scroll(left + 20, trackY, -1));
        assertTrue(Config.get().fire.radius < before);
    }

    @Test
    void picker() {
        Mark sv = rec.find("sv", null);
        int before = Config.get().fire.color;
        menu.click(sv.x(), sv.y(), 0);
        menu.release(0, 0, 0);
        assertEquals(0xFFFFFF, Config.get().fire.color);
        menu.click(sv.x() + sv.w() - 0.5, sv.y() + sv.h() - 0.5, 0);
        menu.release(0, 0, 0);
        assertTrue(Config.get().fire.color < 0x101010 * 3);
        frame(480, 270);
        Mark hue = rec.find("hue", null);
        menu.click(sv.x() + sv.w() - 0.5, sv.y() + 0.5, 0);
        menu.release(0, 0, 0);
        int red = Config.get().fire.color;
        menu.click(hue.x() + hue.w() * 0.5, hue.cy(), 0);
        menu.release(0, 0, 0);
        assertNotEquals(red, Config.get().fire.color);
        assertNotEquals(before, Config.get().fire.color);
    }

    @Test
    void hex() {
        Mark field = rec.find("text", "#FF7A1F");
        click(field);
        assertTrue(menu.typing());
        for (int i = 0; i < 6; i++) menu.key(Menu.BACK);
        for (char c : "12ab3c".toCharArray()) menu.type(c);
        assertEquals(0x12AB3C, Config.get().fire.color);
        assertTrue(menu.type('q'));
        assertEquals(0x12AB3C, Config.get().fire.color);
        clip = "#FF00AA";
        menu.key(Menu.PASTE);
        assertEquals(0xFF00AA, Config.get().fire.color);
        clip = "garbage";
        menu.key(Menu.PASTE);
        assertEquals(0xFF00AA, Config.get().fire.color);
        assertTrue(menu.key(Menu.ESCAPE));
        assertFalse(menu.typing());
    }

    @Test
    void effects() {
        click(rec.find("text", "Effects"));
        click(rec.find("text", "Shimmer"));
        assertEquals(Anim.SHIMMER, Config.get().anim);
        click(rec.find("text", "Screen"));
        assertTrue(Config.get().screen);
        click(rec.find("text", "Fast"));
        assertTrue(Config.get().fast);
        click(rec.find("text", "Recolor flames"));
        assertFalse(Config.get().recolor);
        click(rec.find("text", "Show"));
        click(rec.find("text", "Campfires"));
        assertFalse(Config.get().camps);
        click(rec.find("text", "Torches and lanterns"));
        assertTrue(Config.get().torches);
        click(rec.find("text", "Screen fire"));
        assertTrue(Config.get().overlay);
        click(rec.find("text", "Burning mobs"));
        assertFalse(Config.get().mobs);
        click(rec.find("text", "Fire blocks"));
        assertFalse(Config.get().blocks);
        click(rec.find("text", "Glow over hand"));
        assertTrue(Config.get().hand);
        click(rec.find("text", "On"));
        assertFalse(Config.get().enabled);
    }

    @Test
    void buttons() {
        recolor("4A10C9");
        click(rec.find("text", "Cancel"));
        assertEquals(1, closed.get());
        assertEquals(Config.FIRE, Config.get().fire.color);
        menu = new Menu(closed::incrementAndGet, () -> clip);
        frame(480, 270);
        recolor("4A10C9");
        click(rec.find("text", "Defaults"));
        assertEquals(Config.FIRE, Config.get().fire.color);
        click(rec.find("text", "Done"));
        assertEquals(2, closed.get());
    }

    @Test
    void fits() {
        for (int[] s : new int[][]{{400, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}}) {
            frame(s[0], s[1]);
            inside(s);
            rec.find("flame", null);
            float radius = rec.find("text", "Radius").y();
            float strength = rec.find("text", "Strength").y();
            float falloff = rec.find("text", "Falloff").y();
            float done = rec.find("text", "Done").y() - 5;
            assertTrue(strength - (radius + 15) >= 5, "sliders cramped at " + s[0] + "x" + s[1]);
            assertTrue(falloff - (strength + 15) >= 5, "sliders cramped at " + s[0] + "x" + s[1]);
            assertTrue(falloff + 15 <= done - 4, "sliders hit footer at " + s[0] + "x" + s[1]);
        }
    }

    @Test
    void tabsFit() {
        for (int[] s : new int[][]{{400, 240}, {427, 240}, {480, 270}, {960, 540}}) {
            menu = new Menu(() -> {
            }, () -> null);
            frame(s[0], s[1]);
            for (String t : new String[]{"Effects", "Show"}) {
                Mark m = rec.find("text", t);
                assertTrue(menu.click(m.cx(), m.cy(), 0));
                menu.release(0, 0, 0);
                frame(s[0], s[1]);
                inside(s);
                float done = rec.find("text", "Done").y() - 5;
                String lastRow = t.equals("Effects") ? "Recolor flames" : "Glow over hand";
                Mark row = rec.find("text", lastRow);
                assertTrue(row.y() + 13 <= done, lastRow + " hits footer at " + s[0] + "x" + s[1]);
            }
        }
    }

    private void inside(int[] s) {
        for (Mark m : rec.marks) {
            if (m.kind.equals("text")) {
                assertTrue(m.x() >= 0 && m.y() >= 0 && m.x() + m.w() <= s[0] + 1 && m.y() + m.h() <= s[1] + 1, m + " off screen at " + s[0] + "x" + s[1]);
            }
        }
    }
}
