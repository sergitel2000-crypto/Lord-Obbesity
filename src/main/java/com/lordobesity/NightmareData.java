package com.lordobesity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Quien esta dentro de la pesadilla, cuando sale y desde donde cayo. */
public class NightmareData extends SavedData {
    public static class Entry {
        public UUID player;
        public long returnTime;
        public String dim = "minecraft:overworld";
        public double x, y, z;
        public float yaw, pitch;
        public int stage;
        public int world;      // 0 = pesadilla, 1 = casa, 2 = persecucion
    }

    public final List<Entry> entries = new ArrayList<>();
    public boolean built = false;
    public boolean builtHouse = false;

    public static NightmareData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(NightmareData::load, NightmareData::new, "lordobesity_nightmare");
    }

    public static NightmareData load(CompoundTag tag) {
        NightmareData d = new NightmareData();
        d.built = tag.getBoolean("built");
        d.builtHouse = tag.getBoolean("builtHouse");
        ListTag list = tag.getList("e", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Entry e = new Entry();
            e.player = c.getUUID("p");
            e.returnTime = c.getLong("t");
            e.dim = c.getString("d");
            e.x = c.getDouble("x");
            e.y = c.getDouble("y");
            e.z = c.getDouble("z");
            e.yaw = c.getFloat("yaw");
            e.pitch = c.getFloat("pitch");
            e.stage = c.getInt("s");
            e.world = c.getInt("w");
            d.entries.add(e);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("built", built);
        tag.putBoolean("builtHouse", builtHouse);
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag c = new CompoundTag();
            c.putUUID("p", e.player);
            c.putLong("t", e.returnTime);
            c.putString("d", e.dim);
            c.putDouble("x", e.x);
            c.putDouble("y", e.y);
            c.putDouble("z", e.z);
            c.putFloat("yaw", e.yaw);
            c.putFloat("pitch", e.pitch);
            c.putInt("s", e.stage);
            c.putInt("w", e.world);
            list.add(c);
        }
        tag.put("e", list);
        return tag;
    }
}
