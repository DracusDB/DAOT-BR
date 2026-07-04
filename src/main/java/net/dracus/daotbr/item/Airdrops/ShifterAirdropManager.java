package net.dracus.daotbr.item.Airdrops;

import net.dracus.daotbr.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.particle.ParticleTypes;


public class ShifterAirdropManager {
    private static final List <FallingCrate> activateCrates = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public static void register(DisplayEntity.BlockDisplayEntity entity, double targetY) {
        double distance = entity.getY() - targetY;
        double fallSpeed = distance / (45 * 20); //distance over 45 secs, 20 is ticks per sec
        activateCrates.add(new FallingCrate(entity, targetY, fallSpeed));
    }

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Iterator<FallingCrate> iterator = activateCrates.iterator();
            while (iterator.hasNext()) {
                FallingCrate crate = iterator.next();

                if (crate.entity.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                if (crate.entity.getWorld() != world) {
                    continue;
                }

                double newY = crate.entity.getY() - crate.fallSpeed;

                if (newY <= crate.targetY) {
                    BlockPos landPos = new BlockPos(
                            (int) Math.floor(crate.entity.getX()),
                            (int) crate.targetY,
                            (int) Math.floor(crate.entity.getZ())
                    );

                    world.setBlockState(landPos, Blocks.CHEST.getDefaultState());

                    BlockEntity blockEntity = world.getBlockEntity(landPos);
                    if (blockEntity instanceof ChestBlockEntity chestEntity) {
                        List<Item> possibleSyringes = List.of(
                                ModItems.ARMORED_SYRINGE,
                                ModItems.ATTACK_SYRINGE,
                                ModItems.BEAST_SYRINGE,
                                ModItems.CART_SYRINGE,
                                ModItems.COLOSSAL_SYRINGE,
                                ModItems.FEMALE_SYRINGE,
                                ModItems.JAW_SYRINGE,
                                ModItems.WARHAMMER_SYRINGE
                        );

                        Item syringe = possibleSyringes.get(RANDOM.nextInt(possibleSyringes.size()));
                        chestEntity.setStack(13, new ItemStack(syringe));
                    }

                    crate.entity.discard();
                    iterator.remove();
                } else {
                    crate.entity.setPosition(crate.entity.getX(), newY, crate.entity.getZ());

                    ((ServerWorld) world).spawnParticles(
                            ParticleTypes.WHITE_SMOKE,
                            crate.entity.getX(),
                            crate.targetY + 0.5,
                            crate.entity.getZ(),
                            4,
                            0.2,
                            0.2,
                            0.2,
                            0.03
                    );
                }
            }
        });
    }

    private static class FallingCrate {
        final DisplayEntity.BlockDisplayEntity entity;
        final double targetY;
        final double fallSpeed;

        FallingCrate(DisplayEntity.BlockDisplayEntity entity, double targetY, double fallSpeed) {
            this.entity = entity;
            this.targetY = targetY;
            this.fallSpeed = fallSpeed;
        }
    }
}