package com.example.swordforge.anvil;

import com.example.swordforge.item.CustomSwords;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AnvilBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnvilInteractionHandler {
    public static final Map<BlockPos, ForgeSession> SESSIONS = new ConcurrentHashMap<>();

    public static class ForgeSession {
        public ItemStack baseSword = ItemStack.EMPTY;
        public List<Item> materials = new ArrayList<>();
        public int hammerHits = 0;
    }

    public static void registerEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();

            if (world.getBlockState(pos).getBlock() instanceof AnvilBlock && player.isSneaking()) {
                ItemStack held = player.getMainHandStack();
                ForgeSession session = SESSIONS.computeIfAbsent(pos, k -> new ForgeSession());

                if (session.baseSword.isEmpty() && isValidBaseSword(held.getItem())) {
                    session.baseSword = held.copyWithCount(1);
                    if (!world.isClient) {
                        if (!player.isCreative()) held.decrement(1);
                        player.sendMessage(Text.literal("§e모루 위에 검을 올렸습니다. 재료를 추가하세요."), true);
                    }
                    return ActionResult.SUCCESS;
                }

                if (!session.baseSword.isEmpty() && !held.isEmpty()) {
                    session.materials.add(held.getItem());
                    if (!world.isClient) {
                        if (!player.isCreative()) held.decrement(1);
                        player.sendMessage(Text.literal("§a재료 추가됨 (" + session.materials.size() + "개) - 모루를 보고 R키를 눌러 망치질하세요!"), true);
                    }
                    return ActionResult.SUCCESS;
                }

                if (held.isEmpty() && !session.baseSword.isEmpty()) {
                    if (!world.isClient) {
                        player.getInventory().offerOrDrop(session.baseSword);
                        for (Item mat : session.materials) {
                            player.getInventory().offerOrDrop(new ItemStack(mat));
                        }
                        player.sendMessage(Text.literal("§c모루 위의 아이템을 회수했습니다."), true);
                    }
                    SESSIONS.remove(pos);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }

    public static void handleHammerStrike(ServerPlayerEntity player, BlockPos pos) {
        ForgeSession session = SESSIONS.get(pos);
        if (session == null || session.baseSword.isEmpty()) return;

        ItemStack result = checkRecipe(session.baseSword.getItem(), session.materials);
        if (result.isEmpty()) {
            player.sendMessage(Text.literal("§c재료가 올바르지 않거나 부족합니다!"), true);
            return;
        }

        ServerWorld world = player.getServerWorld();
        session.hammerHits++;

        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0f, 0.8f + (session.hammerHits * 0.1f));
        world.spawnParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 15, 0.2, 0.1, 0.2, 0.05);

        if (session.hammerHits >= 5) {
            world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.3, 0.3, 0.3, 0.1);
            
            session.baseSword = result;
            session.materials.clear();
            session.hammerHits = 0;
            player.sendMessage(Text.literal("§6§l[강화 성공!] " + result.getName().getString() + " 완성! (Shift+빈손 우클릭으로 획득)"), true);
        } else {
            player.sendMessage(Text.literal("§e단조 중... (" + session.hammerHits + "/5)"), true);
        }
    }

    private static boolean isValidBaseSword(Item item) {
        return item == Items.NETHERITE_SWORD || item == CustomSwords.HELLFIRE_SWORD || item == CustomSwords.WITHER_SWORD;
    }

    private static ItemStack checkRecipe(Item base, List<Item> mats) {
        if (base == Items.NETHERITE_SWORD && containsExact(mats, Items.BLAZE_POWDER, Items.FIRE_CHARGE)) {
            return new ItemStack(CustomSwords.HELLFIRE_SWORD);
        }
        if (base == CustomSwords.HELLFIRE_SWORD && containsExact(mats, Items.NETHER_STAR, Items.SOUL_SAND)) {
            return new ItemStack(CustomSwords.WITHER_SWORD);
        }
        if (base == CustomSwords.WITHER_SWORD && containsExact(mats, Items.DRAGON_EGG, Items.DRAGON_HEAD, Items.LAVA_BUCKET, Items.GOLD_BLOCK)) {
            return new ItemStack(CustomSwords.APOCALYPSE_SWORD);
        }
        return ItemStack.EMPTY;
    }

    private static boolean containsExact(List<Item> actual, Item... required) {
        if (actual.size() != required.length) return false;
        List<Item> copy = new ArrayList<>(actual);
        for (Item req : required) {
            if (!copy.remove(req)) return false;
        }
        return true;
    }
}
