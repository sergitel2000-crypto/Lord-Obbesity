package com.lordobesity;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Comandos de pruebas (solo operadores): /lordobesity crack | nightmare | scare | haunt */
@Mod.EventBusSubscriber(modid = LordObesityMod.MODID)
public class ModCommands {
    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("lordobesity")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("crack").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean ok = NightmareManager.createCrack(p);
                    ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Grieta creada cerca de ti."
                            : "No se pudo (ya hay una grieta, o la dimension de la pesadilla no existe)."), false);
                    return ok ? Command.SINGLE_SUCCESS : 0;
                }))
                .then(Commands.literal("nightmare").executes(ctx -> {
                    NightmareManager.enterNow(ctx.getSource().getPlayerOrException(), 0);
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("mundo", IntegerArgumentType.integer(0, 2)).executes(ctx -> {
                    NightmareManager.enterNow(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "mundo"));
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("scare").executes(ctx -> {
                    HauntManager.spawnScare(ctx.getSource().getPlayerOrException());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("haunt").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    long now = p.server.overworld().getGameTime();
                    HauntData.get(p.server).add(p.getUUID(), now + 1200L, now + 100L);
                    ctx.getSource().sendSuccess(() -> Component.literal("Empieza el acecho: sustos ya y el Devorador vuelve en 1 minuto."), false);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
