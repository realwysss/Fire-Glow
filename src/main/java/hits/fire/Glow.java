package hits.fire;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Glow implements ClientModInitializer {
    public static final String ID = "fireglow";
    public static final Logger LOG = LoggerFactory.getLogger("Fire Glow");
    private static final long START = System.nanoTime();

    @Override
    public void onInitializeClient() {
        Config.load(FabricLoader.getInstance().getConfigDir().resolve(ID + ".json"));
        Keys.register();
        ClientChunkEvents.CHUNK_LOAD.register(Fires::load);
        ClientChunkEvents.CHUNK_UNLOAD.register(Fires::unload);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    public static double seconds() {
        return (System.nanoTime() - START) / 1e9;
    }
}
