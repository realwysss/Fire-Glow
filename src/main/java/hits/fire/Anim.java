package hits.fire;

public enum Anim {
    STATIC("Static"),
    PULSE("Pulse"),
    RAINBOW("Rainbow"),
    FLICKER("Flicker"),
    SHIMMER("Shimmer");

    public final String label;

    Anim(String label) {
        this.label = label;
    }
}
