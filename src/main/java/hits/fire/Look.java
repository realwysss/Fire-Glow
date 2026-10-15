package hits.fire;

public record Look(float[] fire, float[] soul, float[] fireTint, float[] soulTint, float inner, float time, float shimmer) {
    public static final float GAIN = 1.5f;

    public static Look of(Config c, double seconds) {
        float t = (float) (seconds * c.speed);
        float k = 1f;
        float turn = 0f;
        float shimmer = 0f;
        switch (c.anim) {
            case PULSE -> k = 0.62f + 0.38f * (float) Math.sin(t * Math.PI * 1.6);
            case RAINBOW -> turn = t * 0.12f;
            case FLICKER -> k = 0.72f + 0.28f * noise(t * 7f);
            case SHIMMER -> shimmer = 1f;
            default -> {
            }
        }
        return new Look(pack(c.fire, k * GAIN, turn), pack(c.soul, k * GAIN, turn),
            tint(c.fire, k, turn, c.recolor), tint(c.soul, k, turn, c.recolor), c.inner, t, shimmer);
    }

    private static float[] pack(Layer l, float k, float turn) {
        if (!l.live()) return new float[]{0f, 0f, 0f, 1f};
        int c = turn == 0f ? l.color : Colors.shift(l.color, turn);
        float s = l.strength * k;
        return new float[]{Colors.r(c) / 255f * s, Colors.g(c) / 255f * s, Colors.b(c) / 255f * s, l.falloff};
    }

    private static float[] tint(Layer l, float k, float turn, boolean on) {
        if (!on || !l.live()) return new float[]{0f, 0f, 0f, 0f};
        int c = turn == 0f ? l.color : Colors.shift(l.color, turn);
        float s = 0.7f + 0.3f * Math.min(1f, k);
        return new float[]{Colors.r(c) / 255f * s, Colors.g(c) / 255f * s, Colors.b(c) / 255f * s, 1f};
    }

    static float noise(float x) {
        int i = (int) Math.floor(x);
        float f = x - i;
        float u = f * f * (3f - 2f * f);
        return lerp(hash(i), hash(i + 1), u) * 0.6f + lerp(hash(i * 3 + 17), hash(i * 3 + 20), f) * 0.4f;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        int m = n * (n * n * 15731 + 789221) + 1376312589;
        return (m & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }

    public boolean dark() {
        return fire[0] + fire[1] + fire[2] + soul[0] + soul[1] + soul[2] <= 0.0001f && fireTint[3] <= 0f && soulTint[3] <= 0f;
    }
}
