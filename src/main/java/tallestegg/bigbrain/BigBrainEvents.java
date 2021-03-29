package tallestegg.bigbrain;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.entity.monster.WitherSkeletonEntity;
import net.minecraft.entity.monster.piglin.PiglinEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.fish.AbstractFishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.TableLootEntry;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.ExplosionContext;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import tallestegg.bigbrain.capablities.LoafProvider;
import tallestegg.bigbrain.entity.IBucklerUser;
import tallestegg.bigbrain.entity.IOneCriticalAfterCharge;
import tallestegg.bigbrain.entity.ai.goals.PressureEntityWithMultishotCrossbowGoal;
import tallestegg.bigbrain.entity.ai.goals.RunWhileChargingGoal;
import tallestegg.bigbrain.entity.ai.goals.UseBucklerGoal;
import tallestegg.bigbrain.items.BucklerItem;

@Mod.EventBusSubscriber(modid = BigBrain.MODID)
public class BigBrainEvents {
    @SubscribeEvent
    public static void onAttachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof CatEntity) {
            event.addCapability(new ResourceLocation(BigBrain.MODID, "loaf"), new LoafProvider());
        }
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getParentA() instanceof PigEntity && event.getParentB() instanceof PigEntity) {
            PigEntity pig = (PigEntity) event.getParentA();
            for (int i = 0; i < BigBrainConfig.minPigBabiesBred + pig.world.rand.nextInt(BigBrainConfig.maxPigBabiesBred + 1); ++i) {
                PigEntity baby = EntityType.PIG.create(event.getChild().world);
                baby.copyLocationAndAnglesFrom(pig);
                baby.setChild(true);
                pig.getEntityWorld().addEntity(baby);
            }
        }
    }

    @SubscribeEvent
    public static void onJump(LivingJumpEvent event) {
        if (event.getEntity() instanceof IBucklerUser) {
            if (((IBucklerUser) event.getEntity()).isBucklerDashing()) {
                event.getEntity().setMotion(event.getEntity().getMotion().getX(), 0.0D, event.getEntity().getMotion().getZ());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttacked(LivingAttackEvent event) {
    }

    @SubscribeEvent
    public static void modifiyVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            if (entity.isPotionActive(Effects.BLINDNESS)) {
                event.modifyVisibility(0.9D);
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent.Throwable event) {
        if (event.getThrowable() instanceof SnowballEntity && event.getThrowable().func_234616_v_() instanceof SnowGolemEntity) {
            if (event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY) {
                Entity entity = ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) entity;
                    living.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 20, 3));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingUpdateEvent event) {
        if (event.getEntity() instanceof IBucklerUser) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            int turningLevel = BigBrainEnchantments.getBucklerEnchantsOnHands(BigBrainEnchantments.TURNING.get(), entity);
            ((IBucklerUser) entity).getBucklerUseTimer();
            if (!((IBucklerUser) entity).isBucklerDashing()) {
                ((IBucklerUser) entity).setBucklerUseTimer(((IBucklerUser) entity).getBucklerUseTimer() + 1);
                int configValue = turningLevel == 0 ? BigBrainConfig.BucklerRunTime : BigBrainConfig.BucklerTurningRunTime;
                if (((IBucklerUser) entity).getBucklerUseTimer() > configValue)
                    ((IBucklerUser) entity).setBucklerUseTimer(configValue);
                ((IBucklerUser) entity).setCooldown(((IBucklerUser) entity).getCooldown() + 1);
                if (((IBucklerUser) entity).getCooldown() > BigBrainConfig.BucklerCooldown)
                    ((IBucklerUser) entity).setCooldown(BigBrainConfig.BucklerCooldown);
            }

            if (((IBucklerUser) entity).isBucklerDashing()) {
                BucklerItem.moveFowards(entity);
                ((IBucklerUser) entity).setBucklerUseTimer(((IBucklerUser) entity).getBucklerUseTimer() - 1);
                ((IBucklerUser) entity).setCooldown(((IBucklerUser) entity).getCooldown() - 1);
                BigBrainEvents.spawnRunningEffectsWhileCharging(entity);
                BigBrainEvents.shieldBash(entity, turningLevel);
                if (((IBucklerUser) entity).getBucklerUseTimer() <= 0) {
                    Hand hand = entity.getHeldItemMainhand().getItem() instanceof BucklerItem ? Hand.MAIN_HAND : Hand.OFF_HAND;
                    ItemStack stack = entity.getHeldItem(hand);
                    ((IBucklerUser) entity).setBucklerDashing(false);
                    ((IBucklerUser) entity).setBucklerUseTimer(0);
                    ((IBucklerUser) entity).setCooldown(0);
                    BucklerItem.setReady(stack, false);
                    entity.resetActiveHand();
                }
                if (((IBucklerUser) entity).getCooldown() <= 0) {
                    ((IBucklerUser) entity).setCooldown(0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (event.getName().toString().contains("minecraft:chests/bastion")) {
            ResourceLocation bucklerBastionLoot = new ResourceLocation(BigBrain.MODID, "chests/buckler_loot_table");
            event.getTable().addPool(LootPool.builder().name("buckler_bastion_chests").addEntry(TableLootEntry.builder(bucklerBastionLoot)).build());
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(CriticalHitEvent event) {
        if (((IOneCriticalAfterCharge) event.getPlayer()).isCritical()) {
            event.setResult(Result.ALLOW);
            event.setDamageModifier(1.5F);
            ((IOneCriticalAfterCharge) event.getPlayer()).setCritical(false);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (event.getEntity() instanceof PillagerEntity) {
            PillagerEntity pillager = (PillagerEntity) entity;
            if (BigBrainConfig.PillagerMultishot)
                pillager.goalSelector.addGoal(2, new PressureEntityWithMultishotCrossbowGoal<>(pillager, 1.0D, 3.0F));
            if (BigBrainConfig.PillagerCover)
                pillager.goalSelector.addGoal(1, new RunWhileChargingGoal(pillager, 0.9D));
        }

        if (event.getEntity() instanceof IMob && BigBrainConfig.MobsAttackAllVillagers && !BigBrainConfig.MobBlackList.contains(event.getEntity().getEntityString())) {
            MobEntity mob = (MobEntity) entity;
            mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, AbstractVillagerEntity.class, true));
        }

        if (event.getEntity() instanceof VillagerEntity && BigBrainConfig.MobsAttackAllVillagers) {
            VillagerEntity villager = (VillagerEntity) entity;
            villager.goalSelector.addGoal(2, new AvoidEntityGoal<>(villager, MobEntity.class, 8.0F, 1.0D, 0.5D, (p_213469_1_) -> {
                return !BigBrainConfig.MobBlackList.contains(p_213469_1_.getEntityString());
            }));
        }

        if (event.getEntity().getType().equals(ForgeRegistries.ENTITIES.getValue(new ResourceLocation("guardvillagers:guard")))) {
            CreatureEntity creature = (CreatureEntity) entity;
            creature.goalSelector.addGoal(0, new UseBucklerGoal<>(creature));
        }

        if (event.getEntity() instanceof PolarBearEntity) {
            PolarBearEntity polar = (PolarBearEntity) entity;
            if (BigBrainConfig.PolarBearFish)
                polar.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(polar, AbstractFishEntity.class, 10, true, true, (Predicate<LivingEntity>) null));
        }
    }

    public static void spawnRunningEffectsWhileCharging(LivingEntity entity) {
        int i = MathHelper.floor(entity.getPosX());
        int j = MathHelper.floor(entity.getPosY() - (double) 0.2F);
        int k = MathHelper.floor(entity.getPosZ());
        BlockPos blockpos = new BlockPos(i, j, k);
        BlockState blockstate = entity.world.getBlockState(blockpos);
        if (!blockstate.addRunningEffects(entity.world, blockpos, entity))
            if (blockstate.getRenderType() != BlockRenderType.INVISIBLE) {
                Vector3d vector3d = entity.getMotion();
                entity.world.addParticle(new BlockParticleData(ParticleTypes.BLOCK, blockstate).setPos(blockpos), entity.getPosX() + (entity.getRNG().nextDouble() - 1.0D) * (double) entity.getSize(entity.getPose()).width, entity.getPosY() + 0.1D,
                        entity.getPosZ() + (entity.getRNG().nextDouble() - 1.0D) * (double) entity.getSize(entity.getPose()).width, vector3d.x * -4.0D, 1.5D, vector3d.z * -4.0D);
            }
    }

    public static void shieldBash(LivingEntity entity, int turningLevel) {
        List<Entity> list = entity.world.getEntitiesInAABBexcluding(entity, entity.getBoundingBox().expand(0.5D, 0.0D, 0.5D), EntityPredicates.pushableBy(entity));
        if (!list.isEmpty() && turningLevel == 0) {
            for (int l = 0; l < list.size(); ++l) {
                Entity entity2 = list.get(l);
                entity2.applyEntityCollision(entity);
                int bangLevel = BigBrainEnchantments.getBucklerEnchantsOnHands(BigBrainEnchantments.BANG.get(), entity);
                float f = 6.0F + ((float) entity.getRNG().nextInt(3));
                float f1 = 2.0F;
                if (f1 > 0.0F && entity instanceof LivingEntity) {
                    for (int duration = 0; duration < 10; ++duration) {
                        double d0 = entity.getRNG().nextGaussian() * 0.02D;
                        double d1 = entity.getRNG().nextGaussian() * 0.02D;
                        double d2 = entity.getRNG().nextGaussian() * 0.02D;
                        BasicParticleType type = entity2 instanceof WitherEntity || entity2 instanceof WitherSkeletonEntity ? ParticleTypes.SMOKE : ParticleTypes.CLOUD;
                        if (entity.world instanceof ServerWorld) {
                            // Collision is done on the server side, so a server side method must be used.
                            ((ServerWorld) entity.world).spawnParticle(type, entity.getPosXRandom(1.0D), entity.getPosYRandom() + 1.0D, entity.getPosZRandom(1.0D), 1, d0, d1, d2, 1.0D);
                            if (!entity.isSilent())
                                ((ServerWorld) entity.world).playSound((PlayerEntity) null, (double) entity.getPosition().getX(), (double) entity.getPosition().getY(), (double) entity.getPosition().getZ(), BigBrainSounds.SHIELD_BASH.get(), entity.getSoundCategory(), 0.12F,
                                        0.8F + entity.getRNG().nextFloat() * 0.4F);
                        }
                    }
                    if (bangLevel == 0) {
                        entity2.attackEntityFrom(DamageSource.causeMobDamage(entity), f);
                        if (entity2 instanceof LivingEntity)
                            ((LivingEntity) entity2).applyKnockback(f1 * 0.8F, (double) MathHelper.sin(entity.rotationYaw * ((float) Math.PI / 180F)), (double) (-MathHelper.cos(entity.rotationYaw * ((float) Math.PI / 180F))));
                        if (entity2 instanceof PlayerEntity && ((PlayerEntity) entity2).getActiveItemStack().isShield(((PlayerEntity) entity2)))
                            ((PlayerEntity) entity2).disableShield(true);
                    } else {
                        Hand hand = entity.getHeldItemMainhand().getItem() instanceof BucklerItem ? Hand.MAIN_HAND : Hand.OFF_HAND;
                        ItemStack stack = entity.getHeldItem(hand);
                        stack.damageItem(5 * bangLevel, entity, (player1) -> {
                            player1.sendBreakAnimation(hand);
                            if (entity instanceof PlayerEntity)
                                ForgeEventFactory.onPlayerDestroyItem((PlayerEntity) entity, entity.getActiveItemStack(), hand);
                        });
                        Explosion.Mode mode = BigBrainConfig.BangBlockDestruction ? Explosion.Mode.BREAK : Explosion.Mode.NONE;
                        entity.world.createExplosion((Entity) null, DamageSource.causeExplosionDamage(entity), (ExplosionContext) null, entity.getPosX(), entity.getPosY(), entity.getPosZ(), (float) bangLevel * 1.0F, false, mode);
                        ((IBucklerUser) entity).setBucklerDashing(false);
                    }
                    entity.setLastAttackedEntity(entity2);
                    if (entity instanceof IOneCriticalAfterCharge)
                        ((IOneCriticalAfterCharge) entity).setCritical(BigBrainEnchantments.getBucklerEnchantsOnHands(BigBrainEnchantments.BANG.get(), entity) == 0);
                }
            }
        }
    }
}
