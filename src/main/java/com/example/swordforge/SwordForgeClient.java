package com.example.swordforge;

import com.example.swordforge.anvil.AnvilInteractionHandler;
import com.example.swordforge.network.HammerStrikePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.AnvilBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

public class SwordForgeClient implements ClientModInitializer {
    public static KeyBinding hammerKey;

    @Override
    public void onInitializeClient() {
        hammerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.swordforge.hammer",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.swordforge.title"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hammerKey.wasPressed()) {
                if (client.player != null && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
                    BlockPos pos = hit.getBlockPos();

                    if (client.world != null && client.world.getBlockState(pos).getBlock() instanceof AnvilBlock) {
                        client.player.swingHand(Hand.MAIN_HAND);
                        ClientPlayNetworking.send(new HammerStrikePayload(pos));
                    }
                }
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            MinecraftClient client = MinecraftClient.getInstance();
            if (matrices == null || consumers == null || client.world == null) return;

            var cameraPos = context.camera().getPos();

            AnvilInteractionHandler.SESSIONS.forEach((pos, session) -> {
                if (session.baseSword.isEmpty()) return;

                matrices.push();
                matrices.translate(pos.getX() + 0.5 - cameraPos.x, pos.getY() + 1.03 - cameraPos.y, pos.getZ() + 0.5 - cameraPos.z);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
                matrices.scale(0.85f, 0.85f, 0.85f);

                client.getItemRenderer().renderItem(
                        session.baseSword, ModelTransformationMode.FIXED, 15728880, OverlayTexture.DEFAULT_UV, matrices, consumers, client.world, 0
                );
                matrices.pop();

                for (int i = 0; i < session.materials.size(); i++) {
                    double angle = (2 * Math.PI / session.materials.size()) * i;
                    double offsetX = Math.cos(angle) * 0.25;
                    double offsetZ = Math.sin(angle) * 0.25;

                    matrices.push();
                    matrices.translate(pos.getX() + 0.5 + offsetX - cameraPos.x, pos.getY() + 1.05 - cameraPos.y, pos.getZ() + 0.5 + offsetZ - cameraPos.z);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
                    matrices.scale(0.4f, 0.4f, 0.4f);

                    client.getItemRenderer().renderItem(
                            new ItemStack(session.materials.get(i)), ModelTransformationMode.FIXED, 15728880, OverlayTexture.DEFAULT_UV, matrices, consumers, client.world, 0
                    );
                    matrices.pop();
                }
            });
        });
    }
}
