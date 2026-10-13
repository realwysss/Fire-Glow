package hits.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FiresTest {
    private static final int ALL = 1 << Fires.FIRE | 1 << Fires.CAMP | 1 << Fires.TORCH;

    @BeforeEach
    void reset() {
        Fires.clear();
    }

    private static List<int[]> near(double x, double y, double z, double r, int kinds) {
        List<int[]> out = new ArrayList<>();
        Fires.near(x, y, z, r, kinds, (px, py, pz, k) -> out.add(new int[]{px, py, pz, k}));
        return out;
    }

    @Test
    void pack() {
        int[][] all = {{0, 0, 0}, {15, 319, 15}, {-1, -64, -1}, {-30000000, -2048, 29999999}, {29999999, 2047, -30000000}, {123, -7, -456}};
        for (int[] p : all) {
            long v = Fires.pack(p[0], p[1], p[2]);
            assertEquals(p[0], Fires.x(v));
            assertEquals(p[1], Fires.y(v));
            assertEquals(p[2], Fires.z(v));
        }
    }

    @Test
    void setAndDrop() {
        Fires.set(1, 64, 1, Fires.FIRE);
        Fires.set(1, 64, 1, Fires.FIRE);
        Fires.set(-1, 64, -1, Fires.CAMP);
        Fires.set(-17, 70, 5, Fires.TORCH);
        assertEquals(3, Fires.count());
        Fires.set(1, 64, 1, 0);
        assertEquals(2, Fires.count());
        Fires.set(1, 64, 1, 0);
        assertEquals(2, Fires.count());
        Fires.drop(-1, -1);
        assertEquals(1, Fires.count());
        Fires.drop(-2, 0);
        assertEquals(0, Fires.count());
        Fires.set(3, 3, 3, Fires.FIRE);
        Fires.set(3, 3, 3, Fires.TORCH);
        assertEquals(1, Fires.count());
        assertEquals(Fires.TORCH, near(3, 3, 3, 4, ALL).get(0)[3]);
    }

    @Test
    void range() {
        Fires.set(0, 64, 0, Fires.FIRE);
        Fires.set(10, 64, 0, Fires.FIRE);
        Fires.set(-40, 64, 0, Fires.FIRE);
        Fires.set(0, 64, 70, Fires.FIRE);
        Fires.set(0, 200, 0, Fires.FIRE);
        List<int[]> got = near(0.5, 64.5, 0.5, 20, ALL);
        assertEquals(2, got.size());
        assertEquals(3, near(0.5, 64.5, 0.5, 64, ALL).size());
        assertEquals(4, near(0.5, 64.5, 0.5, 71, ALL).size());
        assertEquals(0, near(1000, 64, 1000, 64, ALL).size());
    }

    @Test
    void kinds() {
        Fires.set(0, 64, 0, Fires.FIRE);
        Fires.set(1, 64, 0, Fires.CAMP);
        Fires.set(2, 64, 0, Fires.TORCH);
        assertEquals(1, near(0, 64, 0, 16, 1 << Fires.CAMP).size());
        assertEquals(2, near(0, 64, 0, 16, 1 << Fires.FIRE | 1 << Fires.TORCH).size());
        assertEquals(0, near(0, 64, 0, 16, 0).size());
        for (int[] p : near(0, 64, 0, 16, ALL)) assertTrue(p[3] >= Fires.FIRE && p[3] <= Fires.TORCH);
    }

    @Test
    void edges() {
        Fires.set(15, 0, 15, Fires.FIRE);
        Fires.set(16, 0, 16, Fires.FIRE);
        Fires.set(-16, 0, -16, Fires.FIRE);
        Fires.set(-17, 0, -17, Fires.FIRE);
        assertEquals(4, near(0, 0, 0, 30, ALL).size());
        Fires.drop(-2, -2);
        assertEquals(3, Fires.count());
        Fires.drop(-1, -1);
        Fires.drop(0, 0);
        Fires.drop(1, 1);
        assertEquals(0, Fires.count());
    }
}
