package hits.fire;

public final class Bloom {
    private static final float SPREAD = 6f;
    private static final int LEVELS = 4;

    private static boolean open;
    private static boolean used;
    private static final Gpu.Tex[] down = new Gpu.Tex[LEVELS];
    private static Gpu.Tex spare;
    private static int width;
    private static int height;
    private static Gpu.Pipe shrink;
    private static Gpu.Pipe blur;
    private static Gpu.Pipe merger;
    private static Gpu.Buf[] passes;
    private static Gpu.Buf merge;

    private Bloom() {
    }

    public static void begin() {
        used = false;
        Config c = Config.get();
        open = c.live() && c.any() && Gpu.frame();
        if (open) Gpu.clearMask();
    }

    public static boolean want(int what) {
        if (!open || Gpu.redirected()) return false;
        Config c = Config.get();
        return switch (what) {
            case Fires.FIRE -> c.blocks;
            case Fires.CAMP -> c.camps;
            case Fires.TORCH -> c.torches;
            case Fires.MOB -> c.mobs;
            case Fires.SCREEN -> c.overlay;
            default -> false;
        };
    }

    public static int kinds() {
        int k = 0;
        if (want(Fires.FIRE)) k |= 1 << Fires.FIRE;
        if (want(Fires.CAMP)) k |= 1 << Fires.CAMP;
        if (want(Fires.TORCH)) k |= 1 << Fires.TORCH;
        return k;
    }

    public static void touch() {
        used = true;
    }

    public static void hand() {
        if (Config.get().hand || !flush()) return;
        Gpu.clearMask();
    }

    public static void end() {
        flush();
        open = false;
    }

    private static boolean flush() {
        boolean run = open && used;
        used = false;
        if (!run) return false;
        Config c = Config.get();
        Look look = Look.of(c, Glow.seconds());
        if (!look.dark()) merge(c, look);
        return true;
    }

    private static void merge(Config c, Look look) {
        setup();
        Gpu.Tex screen = Gpu.screen();
        Gpu.Tex mask = Gpu.mask();
        int w = screen.width();
        int h = screen.height();
        float k = SPREAD * h / 1080f;
        float sf = c.fire.live() ? Math.max(0.05f, c.fire.radius * k) : 0.05f;
        float ss = c.soul.live() ? Math.max(0.05f, c.soul.radius * k) : 0.05f;
        float top = Math.max(sf, ss);
        int levels = c.fast ? 2 : 1;
        while (levels < LEVELS && top / (1 << levels) > 12f) levels++;
        size(w, h, levels);

        Gpu.Tex src = mask;
        int sw = w;
        int sh = h;
        for (int i = 0; i < levels; i++) {
            Gpu.write(passes[i], 1f / sw, 1f / sh, 0, 0, 0, 0, 0, 0);
            Gpu.draw(shrink, down[i], "Pass", passes[i], src, null);
            src = down[i];
            sw = Math.max(1, sw >> 1);
            sh = Math.max(1, sh >> 1);
        }
        float d = 1 << levels;
        float fs = sf / d;
        float so = ss / d;
        float taps = (float) Math.min(96, Math.ceil(Math.max(fs, so) * 3f));
        Gpu.Tex last = down[levels - 1];
        Gpu.write(passes[LEVELS], 1f / sw, 0f, 0, 0, fs, so, taps, 0);
        Gpu.draw(blur, spare, "Pass", passes[LEVELS], last, null);
        Gpu.write(passes[LEVELS + 1], 0f, 1f / sh, 0, 0, fs, so, taps, 0);
        Gpu.draw(blur, last, "Pass", passes[LEVELS + 1], spare, null);

        Halo.merge(merge, look, w, h, 9f);
        Gpu.draw(merger, screen, "Merge", merge, last, mask);
    }

    private static void setup() {
        if (shrink != null) return;
        shrink = Gpu.post("down", "Pass", Gpu.NONE, false);
        blur = Gpu.post("blur", "Pass", Gpu.NONE, false);
        merger = Gpu.post("merge", "Merge", Gpu.PRE, true);
        passes = new Gpu.Buf[LEVELS + 2];
        for (int i = 0; i < passes.length; i++) passes[i] = Gpu.ubo("fireglow pass " + i, 32);
        merge = Gpu.ubo("fireglow merge", 96);
    }

    private static void size(int w, int h, int levels) {
        if (w != width || h != height) {
            for (int i = 0; i < LEVELS; i++) {
                if (down[i] != null) {
                    down[i].close();
                    down[i] = null;
                }
            }
            if (spare != null) {
                spare.close();
                spare = null;
            }
            width = w;
            height = h;
        }
        for (int i = 0; i < levels; i++) {
            if (down[i] == null) down[i] = Gpu.texture("fireglow down " + i, Math.max(1, w >> (i + 1)), Math.max(1, h >> (i + 1)));
        }
        int lw = Math.max(1, w >> levels);
        int lh = Math.max(1, h >> levels);
        if (spare == null || spare.width() != lw || spare.height() != lh) {
            if (spare != null) spare.close();
            spare = Gpu.texture("fireglow blur", lw, lh);
        }
    }
}
