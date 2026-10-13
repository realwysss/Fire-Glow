package hits.fire.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class Client {
    private Client() {
    }

    static Screen screen(Minecraft mc) {
        return mc.screen;
    }

    static void hud(Minecraft mc, boolean hidden) {
        mc.options.hideGui = hidden;
    }

    static void chunks(TestSingleplayerContext sp) {
        sp.getClientWorld().waitForChunksRender();
    }
}
