package hits.fire.test;

import hits.fire.ui.Canvas;
import java.util.ArrayList;
import java.util.List;

final class Probe implements Canvas {
    record Mark(String kind, String text, float x, float y, float w, float h, int argb) {
        float cx() {
            return x + w / 2f;
        }

        float cy() {
            return y + h / 2f;
        }
    }

    final List<Mark> marks = new ArrayList<>();

    @Override
    public void fill(float x, float y, float w, float h, float r, int top, int bottom) {
        marks.add(new Mark("fill", null, x, y, w, h, top));
    }

    @Override
    public void ring(float x, float y, float w, float h, float r, float t, int top, int bottom) {
        marks.add(new Mark("ring", null, x, y, w, h, top));
    }

    @Override
    public void shadow(float x, float y, float w, float h, float r, float blur, int argb) {
    }

    @Override
    public void hue(float x, float y, float w, float h, float r, int argb) {
        marks.add(new Mark("hue", null, x, y, w, h, argb));
    }

    @Override
    public void sv(float x, float y, float w, float h, float r, int argb) {
        marks.add(new Mark("sv", null, x, y, w, h, argb));
    }

    @Override
    public void text(String s, float x, float y, int argb) {
        marks.add(new Mark("text", s, x, y, width(s), 8, argb));
    }

    @Override
    public int width(String s) {
        return s.length() * 6;
    }

    @Override
    public void flame(float x, float y, float size, boolean soul) {
        marks.add(new Mark("flame", null, x, y, size, size, -1));
    }

    Mark text(String s) {
        for (Mark m : marks) {
            if ("text".equals(m.kind) && s.equals(m.text)) return m;
        }
        throw new AssertionError("No text drawn: " + s);
    }
}
