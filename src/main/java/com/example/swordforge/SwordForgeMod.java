package com.example.swordforge;

import com.example.swordforge.anvil.AnvilInteractionHandler;
import com.example.swordforge.item.CustomSwords;
import com.example.swordforge.network.HammerStrikePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class SwordForgeMod implements ModInitializer {
    public static final String MOD_ID = "swordforge";

    @Override
    public void onInitialize() {
        CustomSwords.registerItems();
        AnvilInteractionHandler.registerEvents();

        PayloadTypeRegistry.playC2S().register(HammerStrikePayload.ID, HammerStrikePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HammerStrikePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                AnvilInteractionHandler.handleHammerStrike(context.player(), payload.anvilPos());
            });
        });
    }
}
