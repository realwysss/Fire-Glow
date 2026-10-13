package hits.fire.test;

import com.mojang.blaze3d.platform.InputConstants;
import hits.fire.Anim;
import hits.fire.Config;
import hits.fire.Fires;
import hits.fire.ModMenuHook;
import hits.fire.ui.Menu;
import hits.fire.ui.Panel;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

public final class GlowTest implements FabricClientGameTest {
    private static final int W = 1280;
    private static final int H = 720;
    private static final boolean FIXED = Boolean.getBoolean("glowtest.fixed");
    private static final double[] VIEW = {0.15, 0.1, 0.85, 0.8};
    private static final double[] LEFT = {0.15, 0.1, 0.5, 0.8};
    private static final double[] RIGHT = {0.5, 0.1, 0.85, 0.8};

    @Override
    public void runTest(ClientGameTestContext ctx) {
        if (Boolean.getBoolean("glowtest.idle")) {
            Config.get().enabled = false;
            ctx.waitTicks(200);
            ctx.takeScreenshot("idle");
            return;
        }
        resize(ctx, W, H);
        ctx.waitTicks(2);
        Config.set(new Config());

        panel(ctx, "title");

        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            Client.chunks(sp);
            cmd(sp, "time set 18000");
            cmd(sp, "weather clear");
            cmd(sp, "gamemode creative @a");
            cmd(sp, "tp @a ~ ~ ~ 0 20");
            ctx.waitTicks(20);
            cmd(sp, "execute as @p at @s run fill ~-9 ~-1 ~1 ~9 ~-1 ~14 minecraft:netherrack");
            cmd(sp, "execute as @p at @s run fill ~-9 ~ ~1 ~9 ~8 ~14 minecraft:air");
            cmd(sp, "execute as @p at @s run fill ~-2 ~ ~6 ~-1 ~ ~6 minecraft:fire");
            cmd(sp, "execute as @p at @s run fill ~1 ~-1 ~6 ~2 ~-1 ~6 minecraft:soul_sand");
            cmd(sp, "execute as @p at @s run fill ~1 ~ ~6 ~2 ~ ~6 minecraft:soul_fire");
            ctx.waitTicks(30);
            Client.chunks(sp);
            int tracked = around(ctx);
            check(tracked >= 4, "fire blocks not tracked: " + tracked);

            cmd(sp, "tick freeze");
            Config.set(only(Fires.FIRE));
            Path off = shot(ctx, "blocks-off", false);
            Path on = shot(ctx, "blocks-on", true);
            double blocks = gain(off, on, VIEW);
            double sky = gain(off, on, new double[]{0.0, 0.0, 1.0, 0.06});
            check(blocks > 0.8, "fire block glow too weak: " + blocks);
            check(sky < blocks * 0.2, "glow leaks into the sky: " + sky);

            Config.set(only(Fires.MOB));
            Path idle = shot(ctx, "blocks-toggle", true);
            check(gain(off, idle, VIEW) < blocks * 0.1, "fire block toggle does nothing");

            Config.set(only(Fires.FIRE));
            Config.get().soul.on = false;
            Path warm = shot(ctx, "fire-layer", true);
            Config.set(only(Fires.FIRE));
            Config.get().fire.on = false;
            Path cold = shot(ctx, "soul-layer", true);
            double warmRight = gain(off, warm, RIGHT);
            double warmLeft = gain(off, warm, LEFT);
            double coldRight = gain(off, cold, RIGHT);
            double coldLeft = gain(off, cold, LEFT);
            check(warmRight > warmLeft * 2, "fire glow is not on the fire: " + warmRight + " vs " + warmLeft);
            check(coldLeft > coldRight * 2, "soul glow is not on the soul fire: " + coldLeft + " vs " + coldRight);
            double[] wt = tone(off, warm, RIGHT);
            double[] ct = tone(off, cold, LEFT);
            check(wt[0] > wt[2] * 1.5, "fire glow is not warm");
            check(ct[2] > ct[0] * 1.5, "soul glow is not cold");

            Config.set(only(Fires.FIRE));
            Config.get().recolor = true;
            Config.get().fire.color = 0x2F6BFF;
            Config.get().fire.strength = 0.02f;
            Config.get().fire.radius = 0f;
            Config.get().soul.on = false;
            Path blue = shot(ctx, "recolor", true);
            double[] rt = tone(off, blue, RIGHT);
            check(rt[2] > 2 && rt[0] < -2, "recolor did nothing: " + rt[0] + " " + rt[2]);

            Config.set(new Config());
            for (int col : new int[]{0xFF2A2A, 0x3FA9FF, 0xC77DFF}) {
                Config.get().fire.color = col;
                shot(ctx, "color-" + Integer.toHexString(col), true);
            }
            Config.set(new Config());
            Config.get().anim = Anim.SHIMMER;
            shot(ctx, "anim-shimmer", true);
            Config.get().anim = Anim.RAINBOW;
            Config.get().recolor = true;
            shot(ctx, "anim-rainbow", true);
            Config.set(new Config());
            Config.get().screen = true;
            shot(ctx, "blend-screen", true);
            Config.set(new Config());
            Config.get().fast = true;
            Config.get().fire.radius = 10f;
            Config.get().soul.radius = 10f;
            shot(ctx, "wide-fast", true);
            Config.set(new Config());
            resize(ctx, 1920, 1080);
            ctx.waitTicks(5);
            shot(ctx, "blocks-1080p", true);
            resize(ctx, W, H);
            ctx.waitTicks(5);
            cmd(sp, "tick unfreeze");

            cmd(sp, "execute as @p at @s run fill ~-4 ~ ~3 ~4 ~4 ~3 minecraft:stone");
            ctx.waitTicks(10);
            Client.chunks(sp);
            Config.set(only(Fires.FIRE));
            Path wallOff = shot(ctx, "wall-off", false);
            Path wallOn = shot(ctx, "wall-on", true);
            double wall = gain(wallOff, wallOn, VIEW);
            check(wall < blocks * 0.15, "glow shows through walls: " + wall);
            cmd(sp, "execute as @p at @s run fill ~-4 ~ ~3 ~4 ~4 ~3 minecraft:air");

            cmd(sp, "execute as @p at @s run fill ~-2 ~ ~6 ~2 ~ ~6 minecraft:air");
            cmd(sp, "execute as @p at @s run setblock ~ ~ ~5 minecraft:campfire");
            ctx.waitTicks(15);
            Client.chunks(sp);
            cmd(sp, "tick freeze");
            Config.set(only(Fires.CAMP));
            Path campOff = shot(ctx, "camp-off", false);
            Path campOn = shot(ctx, "camp-on", true);
            double camp = gain(campOff, campOn, VIEW);
            check(camp > 0.2, "campfire glow missing: " + camp);
            Config.set(only(Fires.FIRE));
            check(gain(campOff, shot(ctx, "camp-toggle", true), VIEW) < camp * 0.15, "campfire toggle does nothing");
            cmd(sp, "tick unfreeze");

            cmd(sp, "execute as @p at @s run setblock ~ ~ ~5 minecraft:air");
            cmd(sp, "execute as @p at @s run setblock ~-1 ~ ~5 minecraft:torch");
            cmd(sp, "execute as @p at @s run setblock ~1 ~ ~5 minecraft:lantern");
            ctx.waitTicks(15);
            Client.chunks(sp);
            Config.set(only(Fires.TORCH));
            Path torchOff = shot(ctx, "torch-off", false);
            Path torchOn = shot(ctx, "torch-on", true);
            double torch = gain(torchOff, torchOn, VIEW);
            check(torch > 0.1, "torch glow missing: " + torch);
            cmd(sp, "execute as @p at @s run fill ~-1 ~ ~5 ~1 ~ ~5 minecraft:air");
            ctx.waitTicks(10);
            int left = around(ctx);
            check(left == 0, "removed fires still tracked: " + left);

            cmd(sp, "execute as @p at @s run summon minecraft:husk ~ ~ ~5 {NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Fire:32767s}");
            ctx.waitTicks(20);
            cmd(sp, "tick freeze");
            Config.set(only(Fires.MOB));
            Path mobOff = shot(ctx, "mob-off", false);
            Path mobOn = shot(ctx, "mob-on", true);
            double mob = gain(mobOff, mobOn, VIEW);
            check(mob > 0.3, "burning mob glow missing: " + mob);
            Config.set(only(Fires.FIRE));
            check(gain(mobOff, shot(ctx, "mob-toggle", true), VIEW) < mob * 0.15, "mob toggle does nothing");
            cmd(sp, "tick unfreeze");

            cmd(sp, "execute as @p at @s run fill ~-2 ~ ~7 ~-1 ~ ~7 minecraft:fire");
            cmd(sp, "execute as @p at @s run setblock ~2 ~ ~4 minecraft:soul_campfire");
            cmd(sp, "execute as @p at @s run setblock ~-3 ~ ~4 minecraft:lantern");
            ctx.waitTicks(15);
            Config.set(new Config());
            Config.get().torches = true;
            shot(ctx, "everything", true);
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            ctx.waitTicks(10);
            shot(ctx, "third-person", true);
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            cmd(sp, "kill @e[type=minecraft:husk]");
            cmd(sp, "execute as @p at @s run fill ~-4 ~ ~3 ~4 ~2 ~8 minecraft:air");
            ctx.waitTicks(10);

            cmd(sp, "execute as @p at @s run fill ~-6 ~ ~1 ~6 ~ ~10 minecraft:fire");
            cmd(sp, "item replace entity @a weapon.mainhand with minecraft:totem_of_undying");
            ctx.waitTicks(30);
            Client.chunks(sp);
            cmd(sp, "tick freeze");
            Config.set(only(Fires.FIRE));
            Path held = shot(ctx, "hand-off", false);
            Path under = shot(ctx, "hand-on", true);
            Config.get().hand = true;
            Path through = shot(ctx, "hand-through", true);
            double[] item = inside(under, through, under, held, new double[]{0.6, 0.35, 1.0, 0.88}, 1);
            check(item[1] > 3000, "glow over hand does nothing: " + item[1]);
            check(item[0] < 1.5, "glow draws over the held item: " + item[0]);

            cmd(sp, "tick unfreeze");
            cmd(sp, "item replace entity @a weapon.mainhand with minecraft:air");
            cmd(sp, "execute as @p at @s run fill ~-6 ~ ~1 ~6 ~ ~4 minecraft:air");
            ctx.waitTicks(60);
            cmd(sp, "tick freeze");
            ctx.runOnClient(mc -> Client.hud(mc, true));
            Config.set(only(Fires.FIRE));
            Path empty = shot(ctx, "golem-none", false);
            cmd(sp, "execute as @p at @s run summon minecraft:iron_golem ~ ~ ~3 {NoAI:1b,Silent:1b,Rotation:[180f,0f]}");
            ctx.waitTicks(5);
            Path golemOff = shot(ctx, "golem-off", false);
            Path golemOn = shot(ctx, "golem-on", true);
            double golem = inside(golemOff, empty, golemOn, golemOff, new double[]{0.25, 0.0, 0.75, 0.85}, 24)[0];
            check(golem < 3, "glow draws over a mob in front of the fire: " + golem);
            ctx.runOnClient(mc -> Client.hud(mc, false));
            cmd(sp, "tick unfreeze");
            cmd(sp, "kill @e[type=minecraft:iron_golem]");
            cmd(sp, "item replace entity @a weapon.mainhand with minecraft:air");
            cmd(sp, "execute as @p at @s run fill ~-6 ~ ~1 ~6 ~ ~10 minecraft:air");
            ctx.waitTicks(10);

            cmd(sp, "gamemode survival @a");
            cmd(sp, "effect give @a minecraft:fire_resistance infinite 0 true");
            cmd(sp, "execute as @p at @s run setblock ~ ~ ~ minecraft:fire");
            ctx.waitTicks(20);
            check(ctx.computeOnClient(mc -> mc.player.isOnFire()), "player did not catch fire");
            ctx.runOnClient(mc -> Client.hud(mc, true));
            cmd(sp, "tick freeze");
            Config.set(only(Fires.SCREEN));
            Path screenOff = shot(ctx, "screen-off", false);
            Path screenOn = shot(ctx, "screen-on", true);
            double burn = gain(screenOff, screenOn, new double[]{0.1, 0.5, 0.9, 1.0});
            check(burn > 0.5, "screen fire glow missing: " + burn);
            Config.set(only(Fires.MOB));
            check(gain(screenOff, shot(ctx, "screen-toggle", true), new double[]{0.1, 0.5, 0.9, 1.0}) < burn * 0.15, "screen fire toggle does nothing");
            cmd(sp, "tick unfreeze");
            ctx.runOnClient(mc -> Client.hud(mc, false));
            cmd(sp, "execute as @p at @s run setblock ~ ~ ~ minecraft:air");
            cmd(sp, "gamemode creative @a");
            ctx.waitTicks(5);

            Config.set(new Config());
            ctx.getInput().pressKey(InputConstants.KEY_J);
            ctx.waitTicks(8);
            check(ctx.computeOnClient(mc -> Client.screen(mc) instanceof Panel), "keybind did not open the panel");
            ctx.takeScreenshot("panel-key");
            ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> !(Client.screen(mc) instanceof Panel)), "escape did not close the keybind panel");

            panel(ctx, "ingame");
        }
        Config.set(new Config());
    }

    private static Config only(int what) {
        Config c = new Config();
        c.anim = Anim.STATIC;
        c.blocks = what == Fires.FIRE;
        c.camps = what == Fires.CAMP;
        c.torches = what == Fires.TORCH;
        c.mobs = what == Fires.MOB;
        c.overlay = what == Fires.SCREEN;
        return c;
    }

    private static int around(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            int[] n = {0};
            Fires.near(mc.player.getX(), mc.player.getY(), mc.player.getZ(), 16, 1 << Fires.FIRE | 1 << Fires.CAMP | 1 << Fires.TORCH, (x, y, z, k) -> n[0]++);
            return n[0];
        });
    }

    private static void cmd(TestSingleplayerContext sp, String command) {
        sp.getServer().runCommand(command);
    }

    private static void panel(ClientGameTestContext ctx, String where) {
        ctx.setScreen(() -> new ModMenuHook().getModConfigScreenFactory().create(null));
        ctx.waitTicks(10);
        check(ctx.computeOnClient(mc -> Client.screen(mc) instanceof Panel), "mod menu factory did not open the panel");
        int scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
        int gw = ctx.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
        int gh = ctx.computeOnClient(mc -> mc.getWindow().getGuiScaledHeight());
        Probe probe = new Probe();
        Config saved = Config.get().copy();
        new Menu(() -> {
        }, () -> null).draw(probe, gw, gh, -1, -1);
        Config.set(saved);
        ctx.takeScreenshot("panel-" + where + "-fire");
        for (String tab : new String[]{"Soul", "Effects", "Show", "Fire"}) {
            click(ctx, probe.text(tab), scale);
            ctx.takeScreenshot("panel-" + where + "-" + tab.toLowerCase());
        }
        click(ctx, probe.text("#FF7A1F"), scale);
        for (int i = 0; i < 6; i++) ctx.getInput().pressKey(InputConstants.KEY_BACKSPACE);
        ctx.getInput().typeChars("FF3E6C");
        ctx.waitTicks(3);
        check(Config.get().fire.color == 0xFF3E6C, "typing a hex colour did not apply: " + Integer.toHexString(Config.get().fire.color));
        ctx.takeScreenshot("panel-" + where + "-typed");
        ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> Client.screen(mc) instanceof Panel), "escape while typing closed the panel");
        ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
        ctx.waitTicks(5);
        check(ctx.computeOnClient(mc -> !(Client.screen(mc) instanceof Panel)), "escape did not close panel");
        Config.set(new Config());
    }

    private static void resize(ClientGameTestContext ctx, int w, int h) {
        if (!FIXED) ctx.getInput().resizeWindow(w, h);
    }

    private static void click(ClientGameTestContext ctx, Probe.Mark m, int scale) {
        ctx.getInput().setCursorPos(m.cx() * scale, m.cy() * scale);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(0);
        ctx.waitTicks(4);
    }

    private static Path shot(ClientGameTestContext ctx, String name, boolean glow) {
        Config.get().enabled = glow;
        ctx.waitTicks(3);
        return ctx.takeScreenshot(name);
    }

    private static double gain(Path off, Path on, double[] r) {
        double[] t = scan(off, on, r, true);
        return 0.2126 * t[0] + 0.7152 * t[1] + 0.0722 * t[2];
    }

    private static double[] tone(Path off, Path on, double[] r) {
        return scan(off, on, r, false);
    }

    private static double[] scan(Path off, Path on, double[] r, boolean up) {
        BufferedImage a = read(off);
        BufferedImage b = read(on);
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        int sx = (int) (w * r[0]), ex = (int) (w * r[2]), sy = (int) (h * r[1]), ey = (int) (h * r[3]);
        double[] sum = new double[3];
        long n = 0;
        for (int y = sy; y < ey; y++) {
            for (int x = sx; x < ex; x++) {
                int p = a.getRGB(x, y);
                int q = b.getRGB(x, y);
                for (int c = 0; c < 3; c++) {
                    int s = 16 - c * 8;
                    double d = (q >> s & 255) - (p >> s & 255);
                    if (Math.abs(d) <= 8) continue;
                    sum[c] += up ? Math.max(0, d) : d;
                }
                n++;
            }
        }
        if (n > 0) for (int c = 0; c < 3; c++) sum[c] /= n;
        return sum;
    }

    private static double[] inside(Path with, Path without, Path test, Path base, double[] r, int grow) {
        BufferedImage a = read(with);
        BufferedImage b = read(without);
        BufferedImage t = read(test);
        BufferedImage o = read(base);
        int w = Math.min(Math.min(a.getWidth(), b.getWidth()), Math.min(t.getWidth(), o.getWidth()));
        int h = Math.min(Math.min(a.getHeight(), b.getHeight()), Math.min(t.getHeight(), o.getHeight()));
        int sx = (int) (w * r[0]), ex = (int) (w * r[2]), sy = (int) (h * r[1]), ey = (int) (h * r[3]);
        int rw = ex - sx;
        int rh = ey - sy;
        int[] sum = new int[(rw + 1) * (rh + 1)];
        for (int y = 0; y < rh; y++) {
            for (int x = 0; x < rw; x++) {
                int s = far(a.getRGB(sx + x, sy + y), b.getRGB(sx + x, sy + y)) > 60 ? 1 : 0;
                sum[(y + 1) * (rw + 1) + x + 1] = s + sum[y * (rw + 1) + x + 1] + sum[(y + 1) * (rw + 1) + x] - sum[y * (rw + 1) + x];
            }
        }
        int full = (2 * grow + 1) * (2 * grow + 1);
        double total = 0;
        long n = 0;
        for (int y = grow; y < rh - grow; y++) {
            for (int x = grow; x < rw - grow; x++) {
                int x0 = x - grow, y0 = y - grow, x1 = x + grow + 1, y1 = y + grow + 1;
                int k = sum[y1 * (rw + 1) + x1] - sum[y0 * (rw + 1) + x1] - sum[y1 * (rw + 1) + x0] + sum[y0 * (rw + 1) + x0];
                if (k < full) continue;
                int d = far(t.getRGB(sx + x, sy + y), o.getRGB(sx + x, sy + y));
                if (d > 24) total += d / 3.0;
                n++;
            }
        }
        check(n > 200, "nothing to measure in " + test.getFileName());
        return new double[]{total / n, n};
    }

    private static int far(int p, int q) {
        return Math.abs((p >> 16 & 255) - (q >> 16 & 255)) + Math.abs((p >> 8 & 255) - (q >> 8 & 255)) + Math.abs((p & 255) - (q & 255));
    }

    private static BufferedImage read(Path p) {
        try {
            return ImageIO.read(p.toFile());
        } catch (IOException e) {
            throw new AssertionError("Could not read " + p, e);
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
