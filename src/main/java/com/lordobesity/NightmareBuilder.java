package com.lordobesity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Construye (una sola vez) la pesadilla: estructuras abstractas y sin sentido ligadas a Lord Obesity.
 * Suelo solido hasta y=4; el jugador aparece en y=5.
 */
public class NightmareBuilder {
    private static final int FLAGS = 18;     // sin actualizaciones de vecinos
    private static final int FLOOR = 4;
    public static final int HOUSE_SPAWN_Z = 28;     // donde apareces frente a la casa
    public static final int CHASE_START = 2;        // centro de la celda de salida del laberinto
    public static final int CHASE_EXIT = 38;        // centro de la celda de la salida
    private static final int CN = 10, CP = 4;       // laberinto: 10x10 celdas, 4 bloques por celda
    private static ServerLevel L;
    private static RandomSource R;

    private static void set(int x, int y, int z, Block b) {
        L.setBlock(new BlockPos(x, y, z), b.defaultBlockState(), FLAGS);
    }

    private static void set(int x, int y, int z, BlockState s) {
        L.setBlock(new BlockPos(x, y, z), s, FLAGS);
    }

    private static void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, b);
    }

    private static void disc(int cx, int y, int cz, double r, Block b) {
        int ir = (int) Math.ceil(r);
        for (int dx = -ir; dx <= ir; dx++)
            for (int dz = -ir; dz <= ir; dz++)
                if (dx * dx + dz * dz <= r * r) set(cx + dx, y, cz + dz, b);
    }

    private static void ring(int cx, int y, int cz, double r, double thick, Block b) {
        int ir = (int) Math.ceil(r);
        double in = Math.max(0, r - thick);
        for (int dx = -ir; dx <= ir; dx++)
            for (int dz = -ir; dz <= ir; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 <= r * r && d2 >= in * in) set(cx + dx, y, cz + dz, b);
            }
    }

    private static void cyl(int cx, int y0, int cz, double r, int h, Block b) {
        for (int y = y0; y < y0 + h; y++) disc(cx, y, cz, r, b);
    }

    public static void build(ServerLevel level) {
        L = level;
        R = level.random;
        try {
            platform();
            bigHat(30, 6);
            mouthRing(-32, -10);
            eyePillars(0, 36);
            cakeTower(-28, 28);
            spiral(32, -30);
            doorRing(-10, -36);
            bellyWall(0, -52);
            smileSky(0, -80, 52);
            floatingCubes(45);
            floorCracks();
        } finally {
            L = null;
            R = null;
        }
    }

    /** Plataforma de llegada: tablero blanco y negro con una sonrisa amarilla y dos ojos. */
    private static void platform() {
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 8; z++)
                if (x * x + z * z <= 64) set(x, FLOOR, z, ((x + z) & 1) == 0 ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE);
        for (int a = 205; a <= 335; a += 3) {
            double rad = Math.toRadians(a);
            set((int) Math.round(Math.cos(rad) * 5.5), FLOOR, (int) Math.round(-Math.sin(rad) * 5.5) + 1, Blocks.YELLOW_CONCRETE);
        }
        for (int e : new int[]{-3, 3}) {
            set(e, FLOOR, -3, Blocks.GLOWSTONE);
            set(e + 1, FLOOR, -3, Blocks.GLOWSTONE);
        }
        for (int[] c : new int[][]{{8, 0}, {-8, 0}, {0, 8}, {0, -8}}) {
            fill(c[0], 5, c[1], c[0], 7, c[1], Blocks.OBSIDIAN);
            set(c[0], 8, c[1], Blocks.GLOWSTONE);
        }
    }

    /** Sombrero gigante en el suelo: se puede subir por el ala y recorrer la copa. */
    private static void bigHat(int cx, int cz) {
        disc(cx, 5, cz, 13, Blocks.WHITE_CONCRETE);
        ring(cx, 5, cz, 13, 1.6, Blocks.YELLOW_CONCRETE);
        cyl(cx, 6, cz, 6, 10, Blocks.WHITE_CONCRETE);
        for (int y = 7; y <= 9; y++) ring(cx, y, cz, 6, 1.5, Blocks.YELLOW_CONCRETE);
        disc(cx, 15, cz, 6, Blocks.WHITE_CONCRETE);
        cyl(cx, 14, cz, 3.2, 2, Blocks.AIR);           // la copa esta hundida
        set(cx, 14, cz, Blocks.GLOWSTONE);
    }

    /** Anillo de dientes alrededor de una lengua roja, vigilado por dos ojos flotantes. */
    private static void mouthRing(int cx, int cz) {
        disc(cx, FLOOR, cz, 8, Blocks.RED_CONCRETE);
        disc(cx, FLOOR, cz, 3, Blocks.BLACK_CONCRETE);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            int x = cx + (int) Math.round(Math.cos(a) * 10), z = cz + (int) Math.round(Math.sin(a) * 10);
            int h = 5 + R.nextInt(7);
            fill(x, 5, z, x + 1, 5 + h, z + 1, Blocks.QUARTZ_BLOCK);
            set(x, 6 + h, z, Blocks.WHITE_CONCRETE);
        }
        for (int s : new int[]{-8, 8}) {                // ojos en el aire
            fill(cx + s - 3, 24, cz, cx + s + 3, 30, cz, Blocks.BLACK_CONCRETE);
            fill(cx + s - 1, 26, cz, cx + s + 1, 28, cz, Blocks.GLOWSTONE);
        }
    }

    /** Pilares oscuros con pares de ojos brillantes mirando al centro. */
    private static void eyePillars(int cx, int cz) {
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + R.nextDouble() * 0.3;
            double cs = Math.cos(a), sn = Math.sin(a);
            int px = cx + (int) Math.round(cs * 13), pz = cz + (int) Math.round(sn * 13);
            int h = 10 + R.nextInt(10);
            fill(px - 1, 5, pz - 1, px + 1, 5 + h, pz + 1, Blocks.OBSIDIAN);
            int y = 5 + h - 3;
            if (Math.abs(cs) > Math.abs(sn)) {
                int fx = cs > 0 ? -2 : 2;               // cara que mira al centro
                set(px + fx, y, pz - 1, Blocks.GLOWSTONE);
                set(px + fx, y, pz + 1, Blocks.GLOWSTONE);
            } else {
                int fz = sn > 0 ? -2 : 2;
                set(px - 1, y, pz + fz, Blocks.GLOWSTONE);
                set(px + 1, y, pz + fz, Blocks.GLOWSTONE);
            }
        }
    }

    /** Torre de tartas absurda que gotea miel. */
    private static void cakeTower(int cx, int cz) {
        cyl(cx, 5, cz, 7, 4, Blocks.WHITE_CONCRETE);
        cyl(cx, 9, cz, 5.5, 4, Blocks.PINK_CONCRETE);
        cyl(cx, 13, cz, 4, 4, Blocks.YELLOW_CONCRETE);
        cyl(cx, 17, cz, 2.5, 3, Blocks.WHITE_CONCRETE);
        set(cx, 20, cz, Blocks.TORCH);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            int x = cx + (int) Math.round(Math.cos(a) * 7), z = cz + (int) Math.round(Math.sin(a) * 7);
            fill(x, 6, z, x, 8, z, Blocks.HONEY_BLOCK);
            set(cx + (int) Math.round(Math.cos(a) * 5), 13, cz + (int) Math.round(Math.sin(a) * 5), Blocks.RED_CONCRETE);
        }
    }

    /** Escalera de caracol que sube y se acaba de golpe. */
    private static void spiral(int cx, int cz) {
        fill(cx, 5, cz, cx, 60, cz, Blocks.OBSIDIAN);
        for (int i = 0; i < 170; i++) {
            double a = i * 0.22;
            int y = 5 + i / 3;
            for (int r = 3; r <= 5; r++) {
                set(cx + (int) Math.round(Math.cos(a) * r), y, cz + (int) Math.round(Math.sin(a) * r), Blocks.SMOOTH_QUARTZ);
            }
        }
        set(cx, 61, cz, Blocks.GLOWSTONE);
    }

    /** Siete puertas sueltas en circulo, sin nada detras, y una tarta en el centro. */
    private static void doorRing(int cx, int cz) {
        set(cx, 5, cz, Blocks.GOLD_BLOCK);
        set(cx, 6, cz, Blocks.CAKE);
        for (int i = 0; i < 7; i++) {
            double a = i * Math.PI * 2 / 7;
            double cs = Math.cos(a), sn = Math.sin(a);
            int dx = cx + (int) Math.round(cs * 7), dz = cz + (int) Math.round(sn * 7);
            Direction f = Math.abs(cs) > Math.abs(sn) ? (cs > 0 ? Direction.WEST : Direction.EAST)
                                                       : (sn > 0 ? Direction.NORTH : Direction.SOUTH);
            BlockState lower = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, f).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            set(dx, 5, dz, lower);
            set(dx, 6, dz, upper);
            set(dx, 7, dz, Blocks.GLOWSTONE);
        }
    }

    /** Pared amarilla con tres botones gigantes y un cuello blanco: una barriga enorme. */
    private static void bellyWall(int cx, int z) {
        fill(cx - 10, 5, z, cx + 10, 22, z + 1, Blocks.YELLOW_CONCRETE);
        fill(cx - 10, 5, z, cx - 8, 22, z + 1, Blocks.WHITE_CONCRETE);
        fill(cx + 8, 5, z, cx + 10, 22, z + 1, Blocks.WHITE_CONCRETE);
        for (int r = 0; r < 7; r++) fill(cx - 6 + r, 22 - r, z + 2, cx + 6 - r, 22 - r, z + 2, Blocks.WHITE_CONCRETE);
        for (int by : new int[]{8, 12, 16}) {
            for (int dx = -2; dx <= 2; dx++)
                for (int dy = -2; dy <= 2; dy++)
                    if (dx * dx + dy * dy <= 5) { set(cx + dx, by + dy, z + 2, Blocks.GOLD_BLOCK); set(cx + dx, by + dy, z + 3, Blocks.GOLD_BLOCK); }
            set(cx, by, z + 4, Blocks.GLOWSTONE);
        }
    }

    /** Una sonrisa y dos ojos gigantes colgando del cielo, lejos. */
    private static void smileSky(int cx, int z, int y) {
        for (int a = 192; a <= 348; a += 1) {
            double rad = Math.toRadians(a);
            int x = cx + (int) Math.round(Math.cos(rad) * 28);
            int yy = y + (int) Math.round(Math.sin(rad) * 18);
            set(x, yy, z, Blocks.YELLOW_CONCRETE);
            set(x, yy + 1, z, Blocks.YELLOW_CONCRETE);
            set(x + 1, yy, z, Blocks.YELLOW_CONCRETE);
            if (a % 5 == 0) {                            // dientes
                int tx = cx + (int) Math.round(Math.cos(rad) * 25), ty = y + (int) Math.round(Math.sin(rad) * 15);
                fill(tx, ty, z, tx + 1, ty + 2, z, Blocks.WHITE_CONCRETE);
            }
        }
        for (int s : new int[]{-14, 14}) {
            int ex = cx + s, ey = y + 24;
            for (int dx = -5; dx <= 5; dx++)
                for (int dy = -8; dy <= 8; dy++)
                    if (dx * dx / 25.0 + dy * dy / 64.0 <= 1.0) set(ex + dx, ey + dy, z, Blocks.BLACK_CONCRETE);
            fill(ex - 1, ey - 1, z, ex + 1, ey + 1, z, Blocks.GLOWSTONE);
        }
    }

    /** Cubos y tartas flotando sin ningun motivo. */
    private static void floatingCubes(int n) {
        Block[] pool = {Blocks.CAKE, Blocks.YELLOW_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.HONEY_BLOCK, Blocks.GLOWSTONE, Blocks.ORANGE_CONCRETE};
        for (int i = 0; i < n; i++) {
            int x = R.nextInt(97) - 48, z = R.nextInt(97) - 48, y = 9 + R.nextInt(26);
            if (Math.abs(x) < 10 && Math.abs(z) < 10) continue;
            Block b = pool[R.nextInt(pool.length)];
            if (b == Blocks.CAKE) set(x, y, z, b);
            else fill(x, y, z, x + 1, y + 1, z + 1, b);
        }
    }

    /** Grietas de obsidiana llorona que salen de la plataforma. */
    private static void floorCracks() {
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + R.nextDouble() * 0.4;
            double x = Math.cos(a) * 9, z = Math.sin(a) * 9;
            for (int s = 0; s < 46; s++) {
                a += (R.nextDouble() - 0.5) * 0.5;
                x += Math.cos(a);
                z += Math.sin(a);
                set((int) Math.round(x), FLOOR, (int) Math.round(z), Blocks.CRYING_OBSIDIAN);
            }
        }
    }

    // =====================================================================================
    //  MUNDO 3: PERSECUCION (laberinto nuevo cada vez)
    // =====================================================================================
    private static boolean isAirAt(int x, int y, int z) {
        return L.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    private static void carveCell(int i, int j) {
        fill(i * CP + 1, 5, j * CP + 1, i * CP + 3, 9, j * CP + 3, Blocks.AIR);
    }

    private static void carvePassage(int i1, int j1, int i2, int j2) {
        if (i1 != i2) {
            int wx = Math.max(i1, i2) * CP;
            fill(wx, 5, j1 * CP + 1, wx, 9, j1 * CP + 3, Blocks.AIR);
        } else {
            int wz = Math.max(j1, j2) * CP;
            fill(i1 * CP + 1, 5, wz, i1 * CP + 3, 9, wz, Blocks.AIR);
        }
    }

    public static void buildChase(ServerLevel level) {
        L = level;
        R = level.random;
        try {
            int size = CN * CP + 1;
            fill(-3, 5, -3, size + 2, 16, size + 2, Blocks.AIR);                  // borra el laberinto anterior
            for (ItemEntity old : level.getEntitiesOfClass(ItemEntity.class, new AABB(-6, 0, -6, size + 6, 70, size + 6))) old.discard();
            fill(-3, FLOOR, -3, size + 2, FLOOR, size + 2, Blocks.BLACK_CONCRETE);
            fill(0, 5, 0, size - 1, 9, size - 1, Blocks.POLISHED_BLACKSTONE_BRICKS);

            boolean[][] seen = new boolean[CN][CN];
            int[] sx = new int[CN * CN], sz = new int[CN * CN];
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            int sp = 1;
            sx[0] = 0;
            sz[0] = 0;
            seen[0][0] = true;
            carveCell(0, 0);
            while (sp > 0) {
                int cx = sx[sp - 1], cz = sz[sp - 1];
                int[] opts = new int[4];
                int n = 0;
                for (int k = 0; k < 4; k++) {
                    int nx = cx + dirs[k][0], nz = cz + dirs[k][1];
                    if (nx >= 0 && nz >= 0 && nx < CN && nz < CN && !seen[nx][nz]) opts[n++] = k;
                }
                if (n == 0) { sp--; continue; }
                int k = opts[R.nextInt(n)];
                int nx = cx + dirs[k][0], nz = cz + dirs[k][1];
                seen[nx][nz] = true;
                carveCell(nx, nz);
                carvePassage(cx, cz, nx, nz);
                sx[sp] = nx;
                sz[sp] = nz;
                sp++;
            }
            for (int i = 0; i < 12; i++) {                                         // atajos: mas de un camino
                int cx = R.nextInt(CN - 1), cz = R.nextInt(CN);
                carvePassage(cx, cz, cx + 1, cz);
            }

            // sangre en el suelo
            for (int i = 0; i < 70; i++) {
                int x = 1 + R.nextInt(size - 2), z = 1 + R.nextInt(size - 2);
                if (isAirAt(x, 5, z)) set(x, FLOOR, z, Blocks.RED_TERRACOTTA);
            }
            // ojos brillantes en las paredes
            for (int i = 0; i < 160; i++) {
                int x = 1 + R.nextInt(size - 3), z = 1 + R.nextInt(size - 3);
                boolean wallX = !isAirAt(x, 7, z) && !isAirAt(x + 1, 7, z);
                if (wallX && ((isAirAt(x, 7, z + 1) && isAirAt(x + 1, 7, z + 1)) || (isAirAt(x, 7, z - 1) && isAirAt(x + 1, 7, z - 1)))) {
                    set(x, 7, z, Blocks.GLOWSTONE);
                    set(x + 1, 7, z, Blocks.GLOWSTONE);
                }
            }
            // salida: suelo dorado, cuatro faros y una columna de luz visible desde todo el laberinto
            int e = CHASE_EXIT;
            fill(e - 1, FLOOR, e - 1, e + 1, FLOOR, e + 1, Blocks.GOLD_BLOCK);
            for (int dx : new int[]{-1, 1})
                for (int dz : new int[]{-1, 1}) fill(e + dx, 5, e + dz, e + dx, 8, e + dz, Blocks.SEA_LANTERN);
            fill(e, 5, e, e, 52, e, Blocks.GLOWSTONE);
            // entrada
            int s0 = CHASE_START;
            fill(s0 - 1, FLOOR, s0 - 1, s0 + 1, FLOOR, s0 + 1, Blocks.RED_CONCRETE);
        } finally {
            L = null;
            R = null;
        }
    }

    // =====================================================================================
    //  MUNDO 2: CASA ENCANTADA
    // =====================================================================================
    private static BlockState stair(Block b, Direction f) {
        return b.defaultBlockState().setValue(StairBlock.FACING, f);
    }

    private static BlockState candle() {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
    }

    private static BlockState hangingLantern() {
        return Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    public static void buildHouse(ServerLevel level) {
        L = level;
        R = level.random;
        try {
            houseShell();
            houseRooms();
            houseRoof();
            houseYard();
            scarecrow(7, 21);
            scarecrow(-8, 27);
        } finally {
            L = null;
            R = null;
        }
    }

    private static void houseShell() {
        fill(-11, FLOOR, -9, 11, FLOOR, 9, Blocks.DARK_OAK_PLANKS);                 // suelo
        fill(-11, 5, -9, 11, 9, 9, Blocks.DARK_OAK_PLANKS);                          // muros
        fill(-10, 5, -8, 10, 9, 8, Blocks.AIR);                                       // interior
        fill(-11, 10, -9, 11, 10, 9, Blocks.DARK_OAK_PLANKS);                        // techo
        for (int[] c : new int[][]{{-11, -9}, {11, -9}, {-11, 9}, {11, 9}}) fill(c[0], 5, c[1], c[0], 10, c[1], Blocks.STRIPPED_DARK_OAK_LOG);
        fill(-1, 5, 9, 0, 7, 9, Blocks.AIR);                                          // puerta rota
        for (int x : new int[]{-8, -5, 5, 8}) windowAt(x, 9, true);
        for (int x : new int[]{-7, -3, 3, 7}) windowAt(x, -9, true);
        for (int z : new int[]{-5, 0, 5}) { windowAt(-11, z, false); windowAt(11, z, false); }
        set(-2, 5, 10, Blocks.COBWEB);
        set(1, 6, 10, Blocks.COBWEB);
    }

    /** Ventana: hueco roto o con barrotes. */
    private static void windowAt(int x, int z, boolean alongX) {
        Block b = R.nextBoolean() ? Blocks.AIR : Blocks.IRON_BARS;
        if (alongX) fill(x, 6, z, x + 1, 7, z, b);
        else fill(x, 6, z, x, 7, z + 1, b);
    }

    private static void portrait(int xMin, int yMin, int z) {
        fill(xMin, yMin, z, xMin + 6, yMin + 4, z, Blocks.YELLOW_CONCRETE);          // un retrato enorme y sonriente
        set(xMin + 1, yMin + 3, z, Blocks.BLACK_CONCRETE);
        set(xMin + 5, yMin + 3, z, Blocks.BLACK_CONCRETE);
        for (int i = 1; i <= 5; i++) set(xMin + i, yMin + (i == 1 || i == 5 ? 1 : 0), z, Blocks.BLACK_CONCRETE);
        for (int i = 2; i <= 4; i++) set(xMin + i, yMin + 1, z, Blocks.WHITE_CONCRETE);
        fill(xMin - 1, yMin + 5, z, xMin + 7, yMin + 5, z, Blocks.WHITE_CONCRETE);   // y su sombrero
        fill(xMin + 1, yMin + 6, z, xMin + 5, yMin + 7, z, Blocks.WHITE_CONCRETE);
    }

    private static void houseRooms() {
        // paredes interiores con huecos de puerta
        fill(-3, 5, -8, -3, 9, 8, Blocks.DARK_OAK_PLANKS);
        fill(3, 5, -8, 3, 9, 8, Blocks.DARK_OAK_PLANKS);
        fill(-3, 5, 4, -3, 7, 5, Blocks.AIR);
        fill(-3, 5, -5, -3, 7, -4, Blocks.AIR);
        fill(3, 5, 4, 3, 7, 5, Blocks.AIR);
        fill(3, 5, -5, 3, 7, -4, Blocks.AIR);
        fill(-10, 5, 0, -4, 9, 0, Blocks.DARK_OAK_PLANKS);
        fill(4, 5, 0, 10, 9, 0, Blocks.DARK_OAK_PLANKS);
        fill(-7, 5, 0, -6, 7, 0, Blocks.AIR);
        fill(6, 5, 0, 7, 7, 0, Blocks.AIR);

        // salon (retrato, mesa con velas y cabeza de calavera, estanterias)
        portrait(-9, 5, 1);
        fill(-10, 5, 2, -10, 7, 7, Blocks.BOOKSHELF);
        set(-7, 5, 5, Blocks.DARK_OAK_FENCE);
        set(-7, 6, 5, Blocks.DARK_OAK_SLAB.defaultBlockState());
        set(-7, 7, 5, candle());
        set(-8, 6, 5, Blocks.SKELETON_SKULL);
        set(-8, 5, 5, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST));
        set(-6, 5, 5, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST));
        // cocina: caldero, horno y una barra con tartas
        set(-9, 5, -7, Blocks.CAULDRON);
        set(-9, 5, -5, Blocks.FURNACE);
        fill(-9, 5, -2, -5, 5, -2, Blocks.QUARTZ_SLAB);
        for (int x = -9; x <= -5; x += 2) set(x, 6, -2, Blocks.CAKE);
        // dormitorio: cama manchada, cofre y velas
        fill(8, 5, 3, 9, 5, 7, Blocks.RED_WOOL);
        fill(8, 5, 7, 9, 5, 7, Blocks.WHITE_WOOL);
        set(8, 6, 3, Blocks.SKELETON_SKULL);
        set(10, 5, 2, Blocks.CHEST);
        set(5, 5, 7, candle());
        set(6, 5, 2, candle());
        // estudio: atril y ojos brillantes en la pared
        set(8, 5, -5, Blocks.LECTERN);
        set(6, 8, -8, Blocks.GLOWSTONE);
        set(8, 8, -8, Blocks.GLOWSTONE);
        // pasillo: fuente de tartas al fondo y farolillos de almas
        fill(-2, 5, -7, 2, 5, -7, Blocks.QUARTZ_SLAB);
        for (int x : new int[]{-2, 0, 2}) set(x, 6, -7, Blocks.CAKE);
        set(0, 9, 6, hangingLantern());
        set(0, 9, -2, hangingLantern());
        // sangre, velas y telarañas
        for (int i = 0; i < 26; i++) {
            int x = -9 + R.nextInt(19), z = -7 + R.nextInt(15);
            if (isAirAt(x, 5, z) && R.nextInt(3) > 0) set(x, FLOOR, z, Blocks.RED_TERRACOTTA);
            else if (isAirAt(x, 5, z) && R.nextInt(4) == 0) set(x, 5, z, candle());
        }
        for (int[] c : new int[][]{{-10, 9}, {10, 9}, {-10, -8}, {10, -8}, {0, 9}, {-2, 8}, {2, -8}}) set(c[0], 9, c[1], Blocks.COBWEB);
    }

    private static void houseRoof() {
        for (int i = 0; i < 7; i++) {
            int y = 11 + i;
            for (int z = -10; z <= 10; z++) {
                set(-12 + i, y, z, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST));
                set(12 - i, y, z, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST));
            }
            for (int x = -11 + i; x <= 11 - i; x++) {                              // frontones cerrados
                set(x, y, -10, Blocks.DARK_OAK_PLANKS);
                set(x, y, 10, Blocks.DARK_OAK_PLANKS);
            }
        }
        fill(-5, 18, 10, 5, 18, 10, Blocks.DARK_OAK_PLANKS);
        for (int e : new int[]{-2, 2}) set(e, 15, 11, Blocks.GLOWSTONE);              // ojos en el desvan
    }

    private static void houseYard() {
        // valla y puerta
        for (int x = -18; x <= 18; x++) {
            set(x, 5, -16, Blocks.NETHER_BRICK_FENCE);
            if (Math.abs(x) > 1) set(x, 5, 34, Blocks.NETHER_BRICK_FENCE);
        }
        for (int z = -16; z <= 34; z++) {
            set(-18, 5, z, Blocks.NETHER_BRICK_FENCE);
            set(18, 5, z, Blocks.NETHER_BRICK_FENCE);
        }
        for (int gx : new int[]{-2, 2}) {
            fill(gx, 5, 34, gx, 7, 34, Blocks.STONE_BRICK_WALL);
            set(gx, 8, 34, Blocks.SOUL_LANTERN);
        }
        fill(-1, FLOOR, 10, 0, FLOOR, 34, Blocks.GRAVEL);                              // camino a la puerta
        // arboles muertos
        for (int i = 0; i < 14; i++) {
            int x = R.nextInt(33) - 16, z = R.nextInt(48) - 14;
            if ((Math.abs(x) <= 13 && z >= -11 && z <= 12) || (Math.abs(x) <= 2 && z > 12)) continue;
            int h = 5 + R.nextInt(4);
            fill(x, 5, z, x, 4 + h, z, Blocks.DARK_OAK_LOG);
            for (int b = 0; b < 3; b++) {
                int by = 3 + h - R.nextInt(3);
                set(x + (R.nextBoolean() ? 1 : -1) * (1 + R.nextInt(2)), by, z + (R.nextBoolean() ? 1 : -1) * R.nextInt(2), Blocks.DARK_OAK_LOG);
            }
        }
        // lapidas
        for (int i = 0; i < 8; i++) {
            int x = R.nextInt(29) - 14, z = 14 + R.nextInt(18);
            if (Math.abs(x) <= 3) continue;
            set(x, 5, z, Blocks.COBBLESTONE_WALL);
            set(x, 6, z, Blocks.STONE_SLAB);
        }
    }

    /** Espantapajaros con cara sonriente, chaleco amarillo y sombrero blanco (mira hacia la entrada, +z). */
    private static void scarecrow(int x, int z) {
        fill(x, 5, z, x, 8, z, Blocks.OAK_FENCE);
        fill(x - 2, 7, z, x + 2, 7, z, Blocks.OAK_FENCE);
        fill(x - 1, 6, z, x + 1, 8, z, Blocks.YELLOW_CONCRETE);
        fill(x - 1, 9, z - 1, x + 1, 11, z + 1, Blocks.YELLOW_CONCRETE);
        set(x - 1, 10, z + 1, Blocks.BLACK_CONCRETE);
        set(x + 1, 10, z + 1, Blocks.BLACK_CONCRETE);
        set(x - 1, 9, z + 1, Blocks.BLACK_CONCRETE);
        set(x, 9, z + 1, Blocks.WHITE_CONCRETE);
        set(x + 1, 9, z + 1, Blocks.BLACK_CONCRETE);
        fill(x - 2, 12, z - 2, x + 2, 12, z + 2, Blocks.WHITE_CONCRETE);
        fill(x - 1, 13, z - 1, x + 1, 15, z + 1, Blocks.WHITE_CONCRETE);
        fill(x - 1, 13, z - 1, x + 1, 13, z + 1, Blocks.YELLOW_CONCRETE);
    }

    // =====================================================================================
    //  LIBROS DE LORE: uno por pesadilla, brillando para poder verlo desde lejos
    // =====================================================================================
    /** Deja (si no esta ya) el libro de lore de ese mundo flotando y resplandeciente. */
    public static void ensureBook(ServerLevel nl, int world) {
        Item item = world == 1 ? ModItems.LORE_BOOK_2.get() : (world == 2 ? ModItems.LORE_BOOK_3.get() : ModItems.LORE_BOOK_1.get());
        double x, y, z;
        if (world == 1) {                 // casa: sobre el atril del estudio
            x = 8.5; y = 6.3; z = -4.5;
        } else if (world == 2) {          // persecucion: en la celda de salida, a tu lado
            x = 3.5; y = 5.4; z = 3.5;
        } else {                          // mundo abstracto: sobre un pedestal en el centro de la lengua roja
            x = -31.5; y = 6.5; z = -9.5;
            nl.getChunkAt(new BlockPos(-32, 5, -10));
            nl.setBlock(new BlockPos(-32, 5, -10), Blocks.OBSIDIAN.defaultBlockState(), FLAGS);
        }
        nl.getChunkAt(BlockPos.containing(x, y, z));
        AABB box = new AABB(x - 2, y - 2, z - 2, x + 2, y + 2, z + 2);
        if (!nl.getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item)).isEmpty()) return;
        ItemEntity ie = new ItemEntity(nl, x, y, z, new ItemStack(item));
        ie.setNoGravity(true);
        ie.setUnlimitedLifetime();
        ie.setGlowingTag(true);
        ie.setDeltaMovement(0, 0, 0);
        nl.addFreshEntity(ie);
    }
}
