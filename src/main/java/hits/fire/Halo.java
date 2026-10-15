package hits.fire;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Halo {
    private static final float SPREAD = 1f;
    private static final Map<Object, Slot> SLOTS = new HashMap<>();
    private static long frame;
    private static Gpu.Pipe grab;
    private static Gpu.Pipe blur;
    private static Gpu.Pipe merge;
    private static Gpu.Pipe blit;
    private static Gpu.Buf grabUbo;
    private static Gpu.Buf blurX;
    private static Gpu.Buf blurY;
    private static Gpu.Buf mergeUbo;

    public record Result(Gpu.Tex view, int pad) {
    }

    private static final class Slot {
        final int size;
        final int low;
        final Gpu.Tex sharp;
        final Gpu.Tex small;
        final Gpu.Tex temp;
        final Gpu.Tex soft;
        final Gpu.Tex out;
        final Result result;
        long seen;
        long baked = -1;

        Slot(int size, int low, int pad) {
            this.size = size;
            this.low = low;
            sharp = Gpu.texture("fireglow halo sharp", size, size);
            out = Gpu.texture("fireglow halo", size, size);
            small = Gpu.texture("fireglow halo small", low, low);
            temp = Gpu.texture("fireglow halo temp", low, low);
            soft = Gpu.texture("fireglow halo soft", low, low);
            result = new Result(out, pad);
        }

        void close() {
            sharp.close();
            out.close();
            small.close();
            temp.close();
            soft.close();
        }
    }

    private Halo() {
    }

    public static void tick() {
        frame++;
        if (SLOTS.isEmpty()) return;
        Iterator<Slot> it = SLOTS.values().iterator();
        while (it.hasNext()) {
            Slot s = it.next();
            if (frame - s.seen > 120) {
                s.close();
                it.remove();
            }
        }
    }

    public static boolean want() {
        return Config.get().live();
    }

    public static Gpu.Pipe pipeline() {
        setup();
        return blit;
    }

    public static Result bake(Gpu.Tex atlas, float u0, float v0, float u1, float v1, Object key, int zoom) {
        Config c = Config.get();
        Look look = Look.of(c, Glow.seconds());
        if (look.dark()) return null;
        setup();
        int scale = Math.max(1, Gpu.guiScale());
        float sf = c.fire.live() ? Math.max(0.05f, c.fire.radius * SPREAD) : 0.05f;
        float ss = c.soul.live() ? Math.max(0.05f, c.soul.radius * SPREAD) : 0.05f;
        float top = Math.max(sf, ss);
        int pad = Math.min(28, (int) Math.ceil(top * 3f) + 1);
        int low = (16 + 2 * pad) * scale;
        int size = low * Math.max(1, zoom);
        Slot s = SLOTS.get(key);
        if (s == null || s.size != size || s.low != low) {
            if (s != null) s.close();
            s = new Slot(size, low, pad);
            SLOTS.put(key, s);
        }
        s.seen = frame;
        if (s.baked == frame) return s.result;
        s.baked = frame;

        float span = (16f + 2f * pad) / 16f;
        Gpu.write(grabUbo, u0, v0, u1 - u0, v1 - v0, span, pad / 16f, 0, 0);
        Gpu.draw(grab, s.sharp, "Pass", grabUbo, atlas, null);
        Gpu.draw(grab, s.small, "Pass", grabUbo, atlas, null);
        float fs = sf * scale;
        float so = ss * scale;
        float taps = (float) Math.min(96, Math.ceil(Math.max(fs, so) * 3f));
        Gpu.write(blurX, 1f / low, 0f, 0, 0, fs, so, taps, 0);
        Gpu.draw(blur, s.temp, "Pass", blurX, s.small, null);
        Gpu.write(blurY, 0f, 1f / low, 0, 0, fs, so, taps, 0);
        Gpu.draw(blur, s.soft, "Pass", blurY, s.temp, null);
        merge(mergeUbo, look, size, size, 6f);
        Gpu.draw(merge, s.out, "Merge", mergeUbo, s.soft, s.sharp);
        return s.result;
    }

    static void merge(Gpu.Buf ubo, Look look, float w, float h, float bands) {
        float[] f = look.fire();
        float[] s = look.soul();
        float[] ft = look.fireTint();
        float[] st = look.soulTint();
        Gpu.write(ubo,
            f[0], f[1], f[2], f[3],
            s[0], s[1], s[2], s[3],
            look.inner(), look.time(), look.shimmer(), Config.get().screen ? 1f : 0f,
            w, h, bands, 0f,
            ft[0], ft[1], ft[2], ft[3],
            st[0], st[1], st[2], st[3]);
    }

    private static void setup() {
        if (grab != null) return;
        grab = Gpu.post("grab", "Pass", Gpu.NONE, false);
        blur = Gpu.post("blur", "Pass", Gpu.NONE, false);
        merge = Gpu.post("merge", "Merge", Gpu.NONE, true);
        blit = Gpu.halo();
        grabUbo = Gpu.ubo("fireglow halo grab", 32);
        blurX = Gpu.ubo("fireglow halo x", 32);
        blurY = Gpu.ubo("fireglow halo y", 32);
        mergeUbo = Gpu.ubo("fireglow halo merge", 96);
    }
}
