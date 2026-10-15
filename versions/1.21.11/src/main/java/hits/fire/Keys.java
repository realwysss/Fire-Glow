package hits.fire;

import com.mojang.blaze3d.platform.InputConstants;
import hits.fire.ui.Panel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public final class Keys {
    private static KeyMapping open;
    private static boolean warmed;

    private Keys() {
    }

    public static boolean matches(net.minecraft.client.input.KeyEvent e) {
        return open != null && open.matches(e);
    }

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(Glow.id("main"));
        open = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.fireglow.open", InputConstants.KEY_J, category));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!warmed && mc.level != null) {
                warmed = true;
                Gpu.warm();
            }
            while (open.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new Panel(null));
            }
        });
    }
}
