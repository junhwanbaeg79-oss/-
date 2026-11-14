package com.example.swordforge.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CustomSwords {
    public static final Item HELLFIRE_SWORD = new HellfireSword(ToolMaterials.NETHERITE, new Item.Settings().attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 6, -2.2f)));
    public static final Item WITHER_SWORD = new WitherSword(ToolMaterials.NETHERITE, new Item.Settings().attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 10, -2.0f)));
    public static final Item APOCALYPSE_SWORD = new ApocalypseSword(ToolMaterials.NETHERITE, new Item.Settings().attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 18, -1.8f)));

    public static void registerItems() {
        Registry.register(Registries.ITEM, Identifier.of("swordforge", "hellfire_sword"), HELLFIRE_SWORD);
        Registry.register(Registries.ITEM, Identifier.of("swordforge", "wither_sword"), WITHER_SWORD);
        Registry.register(Registries.ITEM, Identifier.of("swordforge", "apocalypse_sword"), APOCALYPSE_SWORD);
    }

    public static class HellfireSword extends SwordItem {
        public HellfireSword(ToolMaterial material, Settings settings) { super(material, settings); }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            target.setOnFireFor(6);
            target.getWorld().createExplosion(attacker, target.getX(), target.getY(), target.getZ(), 2.0f, false, World.ExplosionSourceType.NONE);
            return super.postHit(stack, target, attacker);
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (!world.isClient) {
                Vec3d look = user.getRotationVector();
                world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.2f);
                ((ServerWorld) world).spawnParticles(ParticleTypes.FLAME, user.getX() + look.x * 2, user.getY() + 1.0, user.getZ() + look.z * 2, 50, 0.5, 0.5, 0.5, 0.15);

                world.getEntitiesByClass(LivingEntity.class, user.getBoundingBox().expand(4.0), e -> e != user).forEach(entity -> {
                    entity.setOnFireFor(8);
                    entity.damage(world.getDamageSources().playerAttack(user), 12.0f);
                    entity.addVelocity(look.x * 1.2, 0.4, look.z * 1.2);
                });
                user.getItemCooldownManager().set(this, 60);
            }
            return TypedActionResult.success(stack, world.isClient());
        }
    }

    public static class WitherSword extends SwordItem {
        public WitherSword(ToolMaterial material, Settings settings) { super(material, settings); }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 140, 1));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2));
            return super.postHit(stack, target, attacker);
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (!world.isClient) {
                world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.9f);
                for (int i = -1; i <= 1; i++) {
                    Vec3d look = user.getRotationVector();
                    WitherSkullEntity skull = new WitherSkullEntity(world, user, new Vec3d(look.x + (i * 0.2), look.y, look.z + (i * 0.2)));
                    skull.setPosition(user.getX(), user.getEyeY(), user.getZ());
                    world.spawnEntity(skull);
                }
                user.getItemCooldownManager().set(this, 100);
            }
            return TypedActionResult.success(stack, world.isClient());
        }
    }

    public static class ApocalypseSword extends SwordItem {
        public ApocalypseSword(ToolMaterial material, Settings settings) { super(material, settings); }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            World world = target.getWorld();
            if (!world.isClient) {
                LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
                if (lightning != null) {
                    lightning.refreshPositionAfterTeleport(target.getPos());
                    world.spawnEntity(lightning);
                }
                world.createExplosion(attacker, target.getX(), target.getY(), target.getZ(), 4.5f, false, World.ExplosionSourceType.NONE);
            }
            return super.postHit(stack, target, attacker);
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (!world.isClient) {
                ServerWorld sWorld = (ServerWorld) world;
                Vec3d targetPos = user.getPos().add(user.getRotationVector().multiply(8.0));
                
                sWorld.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.4f);

                for (int i = 0; i < 3; i++) {
                    double offsetX = (sWorld.random.nextDouble() - 0.5) * 4.0;
                    double offsetZ = (sWorld.random.nextDouble() - 0.5) * 4.0;
                    
                    FireballEntity fireball = new FireballEntity(
                        sWorld, 
                        user, 
                        new Vec3d(0, -1.5, 0),
                        3
                    );
                    fireball.setPosition(targetPos.x + offsetX, targetPos.y + 12.0, targetPos.z + offsetZ);
                    sWorld.spawnEntity(fireball);
                }

                user.getItemCooldownManager().set(this, 240);
            }
            return TypedActionResult.success(stack, world.isClient());
        }
    }
}
