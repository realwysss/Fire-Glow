package hits.fire;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class Fires {
    public static final int FIRE = 1;
    public static final int CAMP = 2;
    public static final int TORCH = 3;
    public static final int MOB = 4;
    public static final int SCREEN = 5;
    public static final double RANGE = 64;
    private static final Long2ObjectOpenHashMap<Long2ByteOpenHashMap> CHUNKS = new Long2ObjectOpenHashMap<>();
    private static Object level;
    private static int count;

    public interface Sink {
        void at(int x, int y, int z, int kind);
    }

    private Fires() {
    }

    public static int kind(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof BaseFireBlock) return FIRE;
        if (b instanceof CampfireBlock) return s.getValue(CampfireBlock.LIT) ? CAMP : 0;
        if (b instanceof TorchBlock || b instanceof LanternBlock) return TORCH;
        return 0;
    }

    private static boolean lit(BlockState s) {
        return kind(s) != 0;
    }

    public static void load(ClientLevel world, LevelChunk chunk) {
        if (world != level) {
            clear();
            level = world;
        }
        int bx = chunk.getPos().getMinBlockX();
        int bz = chunk.getPos().getMinBlockZ();
        drop(bx >> 4, bz >> 4);
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection s = sections[i];
            if (s == null || s.hasOnlyAir() || !s.maybeHas(Fires::lit)) continue;
            int by = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int k = kind(s.getBlockState(x, y, z));
                        if (k != 0) set(bx + x, by + y, bz + z, k);
                    }
                }
            }
        }
    }

    public static void unload(ClientLevel world, LevelChunk chunk) {
        if (world != level) return;
        drop(chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4);
    }

    public static void update(ClientLevel world, BlockPos pos, BlockState state) {
        if (world != level) return;
        set(pos.getX(), pos.getY(), pos.getZ(), kind(state));
    }

    public static void set(int x, int y, int z, int kind) {
        long ck = key(x >> 4, z >> 4);
        Long2ByteOpenHashMap m = CHUNKS.get(ck);
        if (kind == 0) {
            if (m == null) return;
            if (m.remove(pack(x, y, z)) != 0) count--;
            if (m.isEmpty()) CHUNKS.remove(ck);
            return;
        }
        if (m == null) {
            m = new Long2ByteOpenHashMap();
            CHUNKS.put(ck, m);
        }
        if (m.put(pack(x, y, z), (byte) kind) == 0) count++;
    }

    public static void drop(int cx, int cz) {
        Long2ByteOpenHashMap m = CHUNKS.remove(key(cx, cz));
        if (m != null) count -= m.size();
    }

    public static void clear() {
        CHUNKS.clear();
        count = 0;
        level = null;
    }

    public static int count() {
        return count;
    }

    public static void near(double x, double y, double z, double range, int kinds, Sink sink) {
        if (kinds == 0 || CHUNKS.isEmpty()) return;
        double r2 = range * range;
        int x0 = (int) Math.floor(x - range) >> 4;
        int x1 = (int) Math.floor(x + range) >> 4;
        int z0 = (int) Math.floor(z - range) >> 4;
        int z1 = (int) Math.floor(z + range) >> 4;
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                Long2ByteOpenHashMap m = CHUNKS.get(key(cx, cz));
                if (m == null) continue;
                for (Long2ByteMap.Entry e : Long2ByteMaps.fastIterable(m)) {
                    int k = e.getByteValue();
                    if ((kinds & 1 << k) == 0) continue;
                    long p = e.getLongKey();
                    int px = x(p);
                    int py = y(p);
                    int pz = z(p);
                    double dx = px + 0.5 - x;
                    double dy = py + 0.5 - y;
                    double dz = pz + 0.5 - z;
                    if (dx * dx + dy * dy + dz * dz <= r2) sink.at(px, py, pz, k);
                }
            }
        }
    }

    static long key(int cx, int cz) {
        return (long) cx << 32 | cz & 0xFFFFFFFFL;
    }

    static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | y & 0xFFFL;
    }

    static int x(long p) {
        return (int) (p >> 38);
    }

    static int y(long p) {
        return (int) (p << 52 >> 52);
    }

    static int z(long p) {
        return (int) (p << 26 >> 38);
    }
}
