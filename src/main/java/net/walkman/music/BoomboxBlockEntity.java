package net.walkman.music;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BoomboxBlockEntity extends BlockEntity {
    private ItemStack cassette = ItemStack.EMPTY;
    private boolean isPlaying = false;
    private int soundTimer = 0;

    public BoomboxBlockEntity(BlockPos pos, BlockState state) {
        super(Music.BOOMBOX_BE.get(), pos, state);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, BoomboxBlockEntity be) {
        if (be.soundTimer > 0) {
            be.soundTimer--;
            if (be.soundTimer == 0) {
                be.setPlaying(true);
            }
            be.setChanged();
            if (level != null) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }

        if (be.isPlaying && level != null) {
            if (level.isClientSide) {
                if (level.getGameTime() % 20 == 0) {
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.NOTE, 
                        (double)pos.getX() + 0.5, (double)pos.getY() + 0.7, (double)pos.getZ() + 0.5, 
                        (double)level.random.nextInt(24) / 24.0, 0.0, 0.0);
                }
            } else {
                // Server-side: Make mobs "dance" nearby at night if enabled in config
                if (Config.enableBoomboxDancing && level.getGameTime() % 20 == 0 && level.isNight()) {
                    double radius = 10.0;
                    net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(pos).inflate(radius);
                    java.util.List<net.minecraft.world.entity.LivingEntity> entities = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area, 
                        e -> e instanceof net.minecraft.world.entity.monster.Zombie || e instanceof net.minecraft.world.entity.monster.AbstractSkeleton);
                    
                    for (net.minecraft.world.entity.LivingEntity entity : entities) {
                        // Move them towards the boombox
                        if (entity instanceof net.minecraft.world.entity.Mob mob) {
                            mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
                            
                            // If very close, stop and clear target to dance
                            if (entity.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 4.0) {
                                mob.setTarget(null);
                                mob.getNavigation().stop();
                            }
                        }
                        
                        // Apply a jittery movement/rotation to simulate dancing - increased for "more spin"
                        float jitter = (level.random.nextFloat() - 0.5f) * 120.0f;
                        entity.setYBodyRot(entity.yBodyRot + jitter);
                        entity.setYHeadRot(entity.getYHeadRot() + jitter);
                        
                        if (level.random.nextFloat() < 0.2f) {
                            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.25, 0));
                        }
                    }
                }
            }
        }
    }

    public void startSoundTimer(int ticks) {
        this.soundTimer = ticks;
        this.setChanged();
    }

    public ItemStack getCassette() {
        return cassette;
    }

    public void setCassette(ItemStack cassette) {
        this.cassette = cassette;
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (cassette != null && !cassette.isEmpty()) {
            tag.put("Cassette", cassette.save(registries));
        }
        tag.putBoolean("IsPlaying", isPlaying);
        tag.putInt("SoundTimer", soundTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Cassette")) {
            this.cassette = ItemStack.parse(registries, tag.getCompound("Cassette")).orElse(ItemStack.EMPTY);
        } else {
            this.cassette = ItemStack.EMPTY;
        }
        this.isPlaying = tag.getBoolean("IsPlaying");
        this.soundTimer = tag.getInt("SoundTimer");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        // Include position for easier identification on client (e.g. for held items)
        tag.putInt("x", worldPosition.getX());
        tag.putInt("y", worldPosition.getY());
        tag.putInt("z", worldPosition.getZ());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
