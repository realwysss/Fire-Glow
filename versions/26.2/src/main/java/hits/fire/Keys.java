package hits.fire;

import com.mojang.blaze3d.platform.InputConstants;
import hits.fire.ui.Panel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
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
        open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fireglow.open", InputConstants.KEY_J, category));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!warmed && mc.level != null) {
                warmed = true;
                Gpu.warm();
            }
            while (open.consumeClick()) {
                if (mc.gui.screen() == null && FabricLoader.getInstance().isModLoaded("wysslib")) mc.gui.setScreen(new Panel(null));
            }
        });
    }
}
