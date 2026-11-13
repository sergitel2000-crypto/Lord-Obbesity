package com.lordobesity;

import net.minecraft.client.Minecraft;

/** Solo cliente: crea (una vez) el modelo del sombrero. */
public class ClientArmor {
    private static LordHatModel hat;

    public static LordHatModel hatModel() {
        if (hat == null) {
            hat = new LordHatModel(Minecraft.getInstance().getEntityModels().bakeLayer(LordHatModel.LAYER));
        }
        return hat;
    }
}
