package com.lordobesity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Qué piezas (bits: 1 sombrero, 2 chaqueta, 4 bastón) lleva cada caldero. */
public class RitualData extends SavedData {
    private final Map<Long, Integer> masks = new HashMap<>();

    public static RitualData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RitualData::load, RitualData::new, "lordobesity_rituals");
    }

    public int getMask(BlockPos pos) {
        return masks.getOrDefault(pos.asLong(), 0);
    }

    public void setMask(BlockPos pos, int mask) {
        masks.put(pos.asLong(), mask);
        setDirty();
    }

    public void remove(BlockPos pos) {
        masks.remove(pos.asLong());
        setDirty();
    }

    public static RitualData load(CompoundTag tag) {
        RitualData d = new RitualData();
        CompoundTag m = tag.getCompound("m");
        for (String k : m.getAllKeys()) d.masks.put(Long.parseLong(k), m.getInt(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag m = new CompoundTag();
        masks.forEach((k, v) -> m.putInt(Long.toString(k), v));
        tag.put("m", m);
        return tag;
    }
}
