package hits.fire;

public final class Layer {
    public boolean on = true;
    public int color;
    public float radius;
    public float strength;
    public float falloff;

    public Layer() {
    }

    public Layer(int color, float radius, float strength, float falloff) {
        this.color = color;
        this.radius = radius;
        this.strength = strength;
        this.falloff = falloff;
    }

    public Layer copy() {
        Layer l = new Layer(color, radius, strength, falloff);
        l.on = on;
        return l;
    }

    void fix() {
        color &= 0xFFFFFF;
        radius = Colors.clamp(radius, 0f, 10f);
        strength = Colors.clamp(strength, 0f, 5f);
        falloff = Colors.clamp(falloff, 0.25f, 3f);
        if (Float.isNaN(radius)) radius = 3f;
        if (Float.isNaN(strength)) strength = 1f;
        if (Float.isNaN(falloff)) falloff = 1f;
    }

    public boolean live() {
        return on && strength > 0.001f;
    }
}
