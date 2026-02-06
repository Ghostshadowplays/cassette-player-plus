package net.walkman.music;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.EnumMap;
import java.util.Map;

public class BoomboxBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(3, 0, 6, 13, 4, 9),    // Main body
            Block.box(12, 4, 7, 13, 5, 8),  // Right knob
            Block.box(3, 4, 7, 4, 5, 8),    // Left knob
            Block.box(3, 5, 7, 13, 6, 8)    // Handle
    );

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            SHAPES.put(direction, calculateShape(direction, SHAPE_NORTH));
        }
    }

    private static VoxelShape calculateShape(Direction direction, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            switch (direction) {
                case NORTH -> buffer[0] = Shapes.or(buffer[0], Block.box(minX * 16, minY * 16, minZ * 16, maxX * 16, maxY * 16, maxZ * 16));
                case SOUTH -> buffer[0] = Shapes.or(buffer[0], Block.box((1 - maxX) * 16, minY * 16, (1 - maxZ) * 16, (1 - minX) * 16, maxY * 16, (1 - minZ) * 16));
                case WEST -> buffer[0] = Shapes.or(buffer[0], Block.box(minZ * 16, minY * 16, (1 - maxX) * 16, maxZ * 16, maxY * 16, (1 - minX) * 16));
                case EAST -> buffer[0] = Shapes.or(buffer[0], Block.box((1 - maxZ) * 16, minY * 16, minX * 16, (1 - minZ) * 16, maxY * 16, maxX * 16));
            }
        });
        return buffer[0];
    }

    public BoomboxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_NORTH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoomboxBlockEntity(pos, state);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BoomboxBlockEntity boombox) {
                if (boombox.isPlaying() && !boombox.getCassette().isEmpty()) {
                    String boomboxId = "boombox-" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
                    net.walkman.walkman.CassettePlayerSoundHandler.skipTrack(boomboxId);
                }
            }
        }
        super.attack(state, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BoomboxBlockEntity boombox) {
            // PICKUP MECHANIC: Sneak + Right Click
            if (player.isSecondaryUseActive()) {
                if (!level.isClientSide) {
                    ItemStack boomboxStack = new ItemStack(this);
                    
                    // Save BE data to the item stack
                    CompoundTag beData = boombox.saveWithFullMetadata(level.registryAccess());
                    boomboxStack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(beData));
                    
                    if (level.isClientSide) {
                        // On client, stop the world sound for this position
                        net.walkman.walkman.CassettePlayerSoundHandler.stopMusic(
                            java.util.UUID.nameUUIDFromBytes(("boombox-" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ()).getBytes())
                        );
                    } else {
                        // Also notify client to stop the world sound
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                        // Force a block update so clients see it's being removed/cleared
                        level.sendBlockUpdated(pos, state, state, 3);
                    }

                    if (!player.getInventory().add(boomboxStack)) {
                        player.drop(boomboxStack, false);
                    }
                    
                    // Clear the cassette from the BE so onRemove doesn't drop it twice
                    boombox.setCassette(ItemStack.EMPTY);
                    level.removeBlock(pos, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack cassetteInBoombox = boombox.getCassette();

            if (cassetteInBoombox.isEmpty()) {
                if (inHand.is(Music.CASSETTE.get())) {
                    if (!level.isClientSide) {
                        boombox.setCassette(inHand.copy());
                        inHand.shrink(1);
                        level.playSound(null, pos, Music.CASSETTE_TAPE_SOUND_EFFECT.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                        // Ensure it's not playing music while timer is running
                        boombox.setPlaying(false);
                        // Delay playing by setting a timer (approx 160 ticks for the sound effect which is 8 seconds)
                        boombox.startSoundTimer(160);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            } else {
                if (!level.isClientSide) {
                    player.getInventory().placeItemBackInInventory(cassetteInBoombox);
                    boombox.setCassette(ItemStack.EMPTY);
                    boombox.setPlaying(false);
                    // Also clear timer to be safe
                    boombox.startSoundTimer(0);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BoomboxBlockEntity boombox) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), boombox.getCassette());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type, Music.BOOMBOX_BE.get(), BoomboxBlockEntity::tick);
    }

    @SuppressWarnings("unchecked")
    protected static <E extends BlockEntity, A extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<A> createTickerHelper(net.minecraft.world.level.block.entity.BlockEntityType<A> p_152133_, net.minecraft.world.level.block.entity.BlockEntityType<E> p_152134_, net.minecraft.world.level.block.entity.BlockEntityTicker<? super E> p_152135_) {
        return p_152134_ == p_152133_ ? (net.minecraft.world.level.block.entity.BlockEntityTicker<A>)p_152135_ : null;
    }
}
