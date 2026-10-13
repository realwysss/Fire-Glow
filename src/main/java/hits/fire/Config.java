package hits.fire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Config {
    public static final int FIRE = 0xFF7A1F;
    public static final int SOUL = 0x3FD8FF;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config now = new Config();
    private static Path file;

    public boolean enabled = true;
    public Layer fire = new Layer(FIRE, 3f, 1f, 0.8f);
    public Layer soul = new Layer(SOUL, 3f, 1f, 0.8f);
    public Anim anim = Anim.FLICKER;
    public float speed = 1f;
    public float inner = 0.35f;
    public boolean recolor = true;
    public boolean screen = false;
    public boolean fast = false;
    public boolean blocks = true;
    public boolean camps = true;
    public boolean torches = false;
    public boolean mobs = true;
    public boolean overlay = false;
    public boolean hand = false;

    public static Config get() {
        return now;
    }

    public static void set(Config c) {
        c.fix();
        now = c;
    }

    public Config copy() {
        Config c = GSON.fromJson(GSON.toJson(this), Config.class);
        c.fix();
        return c;
    }

    public boolean same(Config o) {
        return o != null && GSON.toJson(this).equals(GSON.toJson(o));
    }

    public void fix() {
        if (fire == null) fire = new Layer(FIRE, 3f, 1f, 0.8f);
        if (soul == null) soul = new Layer(SOUL, 3f, 1f, 0.8f);
        if (anim == null) anim = Anim.FLICKER;
        fire.fix();
        soul.fix();
        speed = Float.isNaN(speed) ? 1f : Colors.clamp(speed, 0.1f, 4f);
        inner = Float.isNaN(inner) ? 0.35f : Colors.clamp(inner, 0f, 1f);
    }

    public boolean live() {
        return enabled && (fire.live() || soul.live());
    }

    public boolean any() {
        return blocks || camps || torches || mobs || overlay;
    }

    public static void load(Path path) {
        file = path;
        Config c = null;
        if (Files.isRegularFile(path)) {
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                c = GSON.fromJson(r, Config.class);
            } catch (IOException | JsonParseException | IllegalStateException e) {
                Glow.LOG.warn("bad config, using defaults: {}", e.getMessage());
            }
        }
        set(c == null ? new Config() : c);
        if (c == null) save();
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(now, w);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Glow.LOG.warn("couldnt save config: {}", e.getMessage());
        }
    }
}
