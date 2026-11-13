package com.lordobesity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Jugadores a los que Lord Obesity "persigue" tras hundirse: cuándo vuelve y cuándo da sustos. */
public class HauntData extends SavedData {
    public static class Entry {
        public UUID player;
        public long returnTime;
        public long nextScare;
    }

    public final List<Entry> entries = new ArrayList<>();

    public static HauntData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(HauntData::load, HauntData::new, "lordobesity_haunt");
    }

    public void add(UUID player, long returnTime, long nextScare) {
        Entry e = new Entry();
        e.player = player;
        e.returnTime = returnTime;
        e.nextScare = nextScare;
        entries.add(e);
        setDirty();
    }

    public static HauntData load(CompoundTag tag) {
        HauntData d = new HauntData();
        ListTag list = tag.getList("e", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Entry e = new Entry();
            e.player = c.getUUID("p");
            e.returnTime = c.getLong("r");
            e.nextScare = c.getLong("s");
            d.entries.add(e);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag c = new CompoundTag();
            c.putUUID("p", e.player);
            c.putLong("r", e.returnTime);
            c.putLong("s", e.nextScare);
            list.add(c);
        }
        tag.put("e", list);
        return tag;
    }
}
