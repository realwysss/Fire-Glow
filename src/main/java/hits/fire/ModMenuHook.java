package hits.fire;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import hits.fire.ui.Panel;
import net.fabricmc.loader.api.FabricLoader;

public final class ModMenuHook implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return p -> FabricLoader.getInstance().isModLoaded("wysslib") ? new Panel(p) : null;
    }
}
