package alternate.current.redstone.block;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.Node;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import alternate.current.util.BlockUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;

import java.util.*;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class InfinityWire extends Block implements WireBlock {
    WireHandler handler;

    @Override
    public WireHandler getWireHandler() {
        return handler;
    }

    @Override
    public void setWireHandler(WireHandler wireHandler) {
        handler = wireHandler;
    }

    /**
     * specifies what wires can give power to this wire
     * <br>
     * <br>
     * the default implementation returns {@link #NONE}
     * <br>
     * <br>
     * *NOTE* {@link #NONE} is overriden by wires which provide power to {@link #ALL} or the identifier returned by {@link #getWireId()}
     * <br>
     * <br>
     * @param registry - add identifiers to this registry, <br>  for example:<br>{@code return registry.add("minecraft:redstone_wire");}
     * @return {@link #NONE}, {@link #ALL}, or registry (registry must not be empty)
     */
    @Override
    public AlternateCurrentMod.AC_Registry.Registry canBePoweredBy(AlternateCurrentMod.AC_Registry.MutableRegistry registry) {
        return registry.add("minecraft:redstone_wire");
    }

    /**
     * specifies what wires can receive power from this wire
     * <br>
     * <br>
     * the default implementation returns {@link #NONE}
     * <br>
     * <br>
     * *NOTE* {@link #NONE} is overriden by wires which provide power to {@link #ALL} or the identifier returned by {@link #getWireId()}
     * <br>
     * <br>
     * @param registry - add identifiers to this registry, <br>  for example:<br>{@code return registry.add("minecraft:redstone_wire");}
     * @return {@link #NONE}, {@link #ALL}, or registry (registry must not be empty)
     */
    @Override
    public AlternateCurrentMod.AC_Registry.Registry canPower(AlternateCurrentMod.AC_Registry.MutableRegistry registry) {
        return registry.add("minecraft:redstone_wire");
    }

    @Override
    public Block asBlock() {
        return WireBlock.super.asBlock();
    }

    /**
     * The lowest possible power level a wire can have.
     */
    @Override
    public int getMinPower() {
        return 0;
    }

    /**
     * The largest possible power level a wire can have.
     */
    @Override
    public int getMaxPower() {
        return 15;
    }

    /**
     * The drop in power level from one wire to the next.
     */
    @Override
    public int getPowerStep() {
        return 1;
    }

    /**
     * Return the power level of the given wire based on its
     * location and block state.
     *
     * @param world
     * @param pos
     * @param state
     */
    @Override
    public int getPower(alternate.current.redstone.WorldAccess world, BlockPos pos, BlockState state) {
        return state.get(POWER);
    }

    /**
     * Return a block state that holds the given new power level.
     *
     * @param world
     * @param pos
     * @param state
     * @param power
     */
    @Override
    public BlockState updatePowerState(alternate.current.redstone.WorldAccess world, BlockPos pos, BlockState state, int power) {
        return state.with(POWER, power);
    }

    /**
     * Find the connections between the given WireNode and
     * neighboring WireNodes.
     *
     * @param wire
     * @param nodes
     */
    @Override
    public void findWireConnections(WireNode wire, WireHandler.NodeProvider nodes) {
        boolean belowIsConductor = nodes.getNeighbor(wire, WireHandler.Directions.DOWN).isConductor();
        boolean aboveIsConductor = nodes.getNeighbor(wire, WireHandler.Directions.UP).isConductor();

        wire.connections.set((connections, iDir) -> {
            Node neighbor = nodes.getNeighbor(wire, iDir);

            if (neighbor.isWire()) {
                connections.add(neighbor.asWire(), iDir, true, true);
                return;
            }

            boolean sideIsConductor = neighbor.isConductor();

            if (!sideIsConductor) {
                Node node = nodes.getNeighbor(neighbor, WireHandler.Directions.DOWN);

                if (node.isWire()) {
                    connections.add(node.asWire(), iDir, true, belowIsConductor);
                }
            }
            if (!aboveIsConductor) {
                Node node = nodes.getNeighbor(neighbor, WireHandler.Directions.UP);

                if (node.isWire()) {
                    connections.add(node.asWire(), iDir, sideIsConductor, true);
                }
            }
        });
    }

    public static final EnumProperty<WireConnection> WIRE_CONNECTION_NORTH;
    public static final EnumProperty<WireConnection> WIRE_CONNECTION_EAST;
    public static final EnumProperty<WireConnection> WIRE_CONNECTION_SOUTH;
    public static final EnumProperty<WireConnection> WIRE_CONNECTION_WEST;
    public static final IntProperty POWER;
    public static final Map<Direction, EnumProperty<WireConnection>> DIRECTION_TO_WIRE_CONNECTION_PROPERTY;
    private static final VoxelShape DOT_SHAPE;
    private static final Map<Direction, VoxelShape> field_24414;
    private static final Map<Direction, VoxelShape> field_24415;
    private final Map<BlockState, VoxelShape> field_24416 = Maps.newHashMap();
    private static final Vec3f[] field_24466;
    private final BlockState dotState;
    private boolean wiresGivePower = true;

    public InfinityWire(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(WIRE_CONNECTION_NORTH, WireConnection.NONE).with(WIRE_CONNECTION_EAST, WireConnection.NONE).with(WIRE_CONNECTION_SOUTH, WireConnection.NONE).with(WIRE_CONNECTION_WEST, WireConnection.NONE).with(POWER, 0));
        this.dotState = this.getDefaultState().with(WIRE_CONNECTION_NORTH, WireConnection.SIDE).with(WIRE_CONNECTION_EAST, WireConnection.SIDE).with(WIRE_CONNECTION_SOUTH, WireConnection.SIDE).with(WIRE_CONNECTION_WEST, WireConnection.SIDE);
        UnmodifiableIterator var2 = this.getStateManager().getStates().iterator();

        while(var2.hasNext()) {
            BlockState blockState = (BlockState)var2.next();
            if (blockState.get(POWER) == 0) {
                this.field_24416.put(blockState, this.getShapeForState(blockState));
            }
        }

    }

    private VoxelShape getShapeForState(BlockState state) {
        VoxelShape voxelShape = DOT_SHAPE;
        Iterator var3 = Direction.Type.HORIZONTAL.iterator();

        while(var3.hasNext()) {
            Direction direction = (Direction)var3.next();
            WireConnection wireConnection = (WireConnection)state.get((Property)DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction));
            if (wireConnection == WireConnection.SIDE) {
                voxelShape = VoxelShapes.union(voxelShape, field_24414.get(direction));
            } else if (wireConnection == WireConnection.UP) {
                voxelShape = VoxelShapes.union(voxelShape, field_24415.get(direction));
            }
        }

        return voxelShape;
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.field_24416.get(state.with(POWER, 0));
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getPlacementState(ctx.getWorld(), this.dotState, ctx.getBlockPos());
    }

    private BlockState getPlacementState(BlockView world, BlockState state, BlockPos pos) {
        boolean bl = isNotConnected(state);
        state = this.method_27843(world, this.getDefaultState().with(POWER, state.get(POWER)), pos);
        if (bl && isNotConnected(state)) {
            return state;
        } else {
            boolean bl2 = state.get(WIRE_CONNECTION_NORTH).isConnected();
            boolean bl3 = state.get(WIRE_CONNECTION_SOUTH).isConnected();
            boolean bl4 = state.get(WIRE_CONNECTION_EAST).isConnected();
            boolean bl5 = state.get(WIRE_CONNECTION_WEST).isConnected();
            boolean bl6 = !bl2 && !bl3;
            boolean bl7 = !bl4 && !bl5;
            if (!bl5 && bl6) {
                state = state.with(WIRE_CONNECTION_WEST, WireConnection.SIDE);
            }

            if (!bl4 && bl6) {
                state = state.with(WIRE_CONNECTION_EAST, WireConnection.SIDE);
            }

            if (!bl2 && bl7) {
                state = state.with(WIRE_CONNECTION_NORTH, WireConnection.SIDE);
            }

            if (!bl3 && bl7) {
                state = state.with(WIRE_CONNECTION_SOUTH, WireConnection.SIDE);
            }

            return state;
        }
    }

    private BlockState method_27843(BlockView world, BlockState state, BlockPos pos) {
        boolean bl = !world.getBlockState(pos.up()).isSolidBlock(world, pos);
        Iterator var5 = Direction.Type.HORIZONTAL.iterator();

        while(var5.hasNext()) {
            Direction direction = (Direction)var5.next();
            if (!((WireConnection)state.get((Property)DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction))).isConnected()) {
                WireConnection wireConnection = this.method_27841(world, pos, direction, bl);
                state = state.with(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction), wireConnection);
            }
        }

        return state;
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state;
        } else if (direction == Direction.UP) {
            return this.getPlacementState(world, state, pos);
        } else {
            WireConnection wireConnection = this.getRenderConnectionType(world, pos, direction);
            return wireConnection.isConnected() == ((WireConnection)state.get((Property)DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction))).isConnected() && !isFullyConnected(state) ? state.with(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction), wireConnection) : this.getPlacementState(world, this.dotState.with(POWER, state.get(POWER)).with(DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction), wireConnection), pos);
        }
    }

    private static boolean isFullyConnected(BlockState state) {
        return state.get(WIRE_CONNECTION_NORTH).isConnected() && state.get(WIRE_CONNECTION_SOUTH).isConnected() && state.get(WIRE_CONNECTION_EAST).isConnected() && state.get(WIRE_CONNECTION_WEST).isConnected();
    }

    private static boolean isNotConnected(BlockState state) {
        return !state.get(WIRE_CONNECTION_NORTH).isConnected() && !state.get(WIRE_CONNECTION_SOUTH).isConnected() && !state.get(WIRE_CONNECTION_EAST).isConnected() && !state.get(WIRE_CONNECTION_WEST).isConnected();
    }

    public void prepare(BlockState state, WorldAccess world, BlockPos pos, int flags, int maxUpdateDepth) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        Iterator var7 = Direction.Type.HORIZONTAL.iterator();

        while(var7.hasNext()) {
            Direction direction = (Direction)var7.next();
            WireConnection wireConnection = (WireConnection)state.get((Property)DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction));
            if (wireConnection != WireConnection.NONE) {
                BlockState blockState1 = world.getBlockState(mutable.set(pos, direction));
                if (!blockState1.isOf(Blocks.REDSTONE_WIRE) || !blockState1.isOf(this)) {
                    mutable.move(Direction.DOWN);
                    BlockState blockState = world.getBlockState(mutable);
                    if (!blockState.isOf(Blocks.OBSERVER)) {
                        BlockPos blockPos = mutable.offset(direction.getOpposite());
                        BlockState blockState2 = blockState.getStateForNeighborUpdate(direction.getOpposite(), world.getBlockState(blockPos), world, mutable, blockPos);
                        replace(blockState, blockState2, world, mutable, flags, maxUpdateDepth);
                    }

                    mutable.set(pos, direction).move(Direction.UP);
                    BlockState blockState3 = world.getBlockState(mutable);
                    if (!blockState3.isOf(Blocks.OBSERVER)) {
                        BlockPos blockPos2 = mutable.offset(direction.getOpposite());
                        BlockState blockState4 = blockState3.getStateForNeighborUpdate(direction.getOpposite(), world.getBlockState(blockPos2), world, mutable, blockPos2);
                        replace(blockState3, blockState4, world, mutable, flags, maxUpdateDepth);
                    }
                }
            }
        }

    }

    private WireConnection getRenderConnectionType(BlockView world, BlockPos pos, Direction direction) {
        return this.method_27841(world, pos, direction, !world.getBlockState(pos.up()).isSolidBlock(world, pos));
    }

    private WireConnection method_27841(BlockView blockView, BlockPos blockPos, Direction direction, boolean bl) {
        BlockPos blockPos2 = blockPos.offset(direction);
        BlockState blockState = blockView.getBlockState(blockPos2);
        if (bl) {
            boolean bl2 = this.canRunOnTop(blockView, blockPos2, blockState);
            if (bl2 && connectsTo(blockView.getBlockState(blockPos2.up()))) {
                if (blockState.isSideSolidFullSquare(blockView, blockPos2, direction.getOpposite())) {
                    return WireConnection.UP;
                }

                return WireConnection.SIDE;
            }
        }

        return !connectsTo(blockState, direction) && (blockState.isSolidBlock(blockView, blockPos2) || !connectsTo(blockView.getBlockState(blockPos2.down()))) ? WireConnection.NONE : WireConnection.SIDE;
    }

    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        return this.canRunOnTop(world, blockPos, blockState);
    }

    private boolean canRunOnTop(BlockView world, BlockPos pos, BlockState floor) {
        return floor.isSideSolidFullSquare(world, pos, Direction.UP) || floor.isOf(Blocks.HOPPER);
    }

    private int getReceivedRedstonePower(World world, BlockPos pos) {
        this.wiresGivePower = false;
        int i = world.getReceivedRedstonePower(pos);
        this.wiresGivePower = true;
        int j = 0;
        if (i < 15) {
            Iterator var5 = Direction.Type.HORIZONTAL.iterator();

            while(true) {
                while(var5.hasNext()) {
                    Direction direction = (Direction)var5.next();
                    BlockPos blockPos = pos.offset(direction);
                    BlockState blockState = world.getBlockState(blockPos);
                    j = Math.max(j, this.increasePower(blockState));
                    BlockPos blockPos2 = pos.up();
                    if (blockState.isSolidBlock(world, blockPos) && !world.getBlockState(blockPos2).isSolidBlock(world, blockPos2)) {
                        j = Math.max(j, this.increasePower(world.getBlockState(blockPos.up())));
                    } else if (!blockState.isSolidBlock(world, blockPos)) {
                        j = Math.max(j, this.increasePower(world.getBlockState(blockPos.down())));
                    }
                }

                return Math.max(i, j - 1);
            }
        } else {
            return Math.max(i, j - 1);
        }
    }

    private int increasePower(BlockState state) {
        return (state.isOf(Blocks.REDSTONE_WIRE) || state.isOf(this)) ? state.get(POWER) : 0;
    }

    private void updateNeighbors(World world, BlockPos pos) {
        if (world.getBlockState(pos).isOf(this)) {
            world.updateNeighborsAlways(pos, this);
            Direction[] var3 = Direction.values();
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Direction direction = var3[var5];
                world.updateNeighborsAlways(pos.offset(direction), this);
            }

        }
    }

    private void updateOffsetNeighbors(World world, BlockPos pos) {
        Iterator var3 = Direction.Type.HORIZONTAL.iterator();

        Direction direction2;
        while(var3.hasNext()) {
            direction2 = (Direction)var3.next();
            this.updateNeighbors(world, pos.offset(direction2));
        }

        var3 = Direction.Type.HORIZONTAL.iterator();

        while(var3.hasNext()) {
            direction2 = (Direction)var3.next();
            BlockPos blockPos = pos.offset(direction2);
            if (world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
                this.updateNeighbors(world, blockPos.up());
            } else {
                this.updateNeighbors(world, blockPos.down());
            }
        }

    }

    private void update(World world, BlockPos pos, BlockState state) {
        if (!AlternateCurrentMod.on) {
            int i = this.getReceivedRedstonePower(world, pos);
            if (state.get(POWER) != i) {
                if (world.getBlockState(pos) == state) {
                    world.setBlockState(pos, state.with(POWER, i), 2);
                }

                Set<BlockPos> set = Sets.newHashSet();
                set.add(pos);
                Direction[] var6 = Direction.values();
                int var7 = var6.length;

                for (int var8 = 0; var8 < var7; ++var8) {
                    Direction direction = var6[var8];
                    set.add(pos.offset(direction));
                }

                Iterator var10 = set.iterator();

                while (var10.hasNext()) {
                    BlockPos blockPos = (BlockPos) var10.next();
                    world.updateNeighborsAlways(blockPos, this);
                }
            }
        }
    }

    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock()) && !world.isClient) {
            if (AlternateCurrentMod.on) {
                ((IServerWorld) world).getAccess(this).getWireHandler().onWireAdded(pos);

                // Because of a check in World.setBlockState, shape updates
                // after placing a block are omitted if the block state
                // changes while setting it in the chunk. This can happen
                // due to the above call to the wire handler. To make sure
                // connections are properly updated after placing a redstone
                // wire, shape updates are emitted here.
                BlockState newState = world.getBlockState(pos);

                if (newState != state) {
                    newState.updateNeighbors(world, pos, BlockUtil.FLAG_NOTIFY_CLIENTS);
                    newState.prepare(world, pos, BlockUtil.FLAG_NOTIFY_CLIENTS);
                }
            }
            this.update(world, pos, state);
            Iterator var6 = Direction.Type.VERTICAL.iterator();

            while (var6.hasNext()) {
                Direction direction = (Direction) var6.next();
                world.updateNeighborsAlways(pos.offset(direction), this);
            }

            this.updateOffsetNeighbors(world, pos);
        }
    }

    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (AlternateCurrentMod.on) {
            ((IServerWorld)world).getAccess(this).getWireHandler().onWireRemoved(pos);
        }
        if (!moved && !state.isOf(newState.getBlock())) {
            super.onStateReplaced(state, world, pos, newState, moved);
            if (!world.isClient) {
                Direction[] var6 = Direction.values();
                int var7 = var6.length;

                for (int var8 = 0; var8 < var7; ++var8) {
                    Direction direction = var6[var8];
                    world.updateNeighborsAlways(pos.offset(direction), this);
                }

                this.update(world, pos, state);
                this.updateOffsetNeighbors(world, pos);
            }
        }
    }

    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (!world.isClient) {
            if (AlternateCurrentMod.on) {
                ((IServerWorld) world).getAccess(this).getWireHandler().onWireUpdated(pos);
            } else {
                if (state.canPlaceAt(world, pos)) {
                    this.update(world, pos, state);
                } else {
                    dropStacks(state, world, pos);
                    world.removeBlock(pos, false);
                }
            }
        }
    }

    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return !this.wiresGivePower ? 0 : state.getWeakRedstonePower(world, pos, direction);
    }

    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (this.wiresGivePower && direction != Direction.DOWN) {
            int i = state.get(POWER);
            if (i == 0) {
                return 0;
            } else {
                return direction != Direction.UP && !((WireConnection)this.getPlacementState(world, state, pos).get((Property)DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction.getOpposite()))).isConnected() ? 0 : i;
            }
        } else {
            return 0;
        }
    }

    protected static boolean connectsTo(BlockState state) {
        return connectsTo(state, null);
    }

    protected static boolean connectsTo(BlockState state, @Nullable Direction dir) {
        if (state.isOf(Blocks.REDSTONE_WIRE) || state.isOf(AlternateCurrentMod.InfinityWireBlock)) {
            return true;
        } else if (state.isOf(Blocks.REPEATER)) {
            Direction direction = state.get(RepeaterBlock.FACING);
            return direction == dir || direction.getOpposite() == dir;
        } else if (state.isOf(Blocks.OBSERVER)) {
            return dir == state.get(ObserverBlock.FACING);
        } else {
            return state.emitsRedstonePower() && dir != null;
        }
    }

    public boolean emitsRedstonePower(BlockState state) {
        return this.wiresGivePower;
    }

    @Environment(EnvType.CLIENT)
    public static int getWireColor(int powerLevel) {
        Vec3f vec3f = field_24466[powerLevel];
        return MathHelper.packRgb(vec3f.getX(), vec3f.getY(), vec3f.getZ());
    }

    @Environment(EnvType.CLIENT)
    private void method_27936(World world, Random random, BlockPos pos, Vec3f vec3f, Direction direction, Direction direction2, float f, float g) {
        float h = g - f;
        if (!(random.nextFloat() >= 0.2F * h)) {
            float i = 0.4375F;
            float j = f + h * random.nextFloat();
            double d = 0.5D + (double)(0.4375F * (float)direction.getOffsetX()) + (double)(j * (float)direction2.getOffsetX());
            double e = 0.5D + (double)(0.4375F * (float)direction.getOffsetY()) + (double)(j * (float)direction2.getOffsetY());
            double k = 0.5D + (double)(0.4375F * (float)direction.getOffsetZ()) + (double)(j * (float)direction2.getOffsetZ());
            world.addParticle(new DustParticleEffect(vec3f.getX(), vec3f.getY(), vec3f.getZ(), 1.0F), (double)pos.getX() + d, (double)pos.getY() + e, (double)pos.getZ() + k, 0.0D, 0.0D, 0.0D);
        }
    }

    @Environment(EnvType.CLIENT)
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int i = state.get(POWER);
        if (i != 0) {
            Iterator var6 = Direction.Type.HORIZONTAL.iterator();

            while(var6.hasNext()) {
                Direction direction = (Direction)var6.next();
                WireConnection wireConnection = (WireConnection)state.get((Property)DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction));
                switch(wireConnection) {
                    case UP:
                        this.method_27936(world, random, pos, field_24466[i], direction, Direction.UP, -0.5F, 0.5F);
                    case SIDE:
                        this.method_27936(world, random, pos, field_24466[i], Direction.DOWN, direction, 0.0F, 0.5F);
                        break;
                    case NONE:
                    default:
                        this.method_27936(world, random, pos, field_24466[i], Direction.DOWN, direction, 0.0F, 0.3F);
                }
            }

        }
    }

    public BlockState rotate(BlockState state, BlockRotation rotation) {
        switch(rotation) {
            case CLOCKWISE_180:
                return state.with(WIRE_CONNECTION_NORTH, state.get(WIRE_CONNECTION_SOUTH)).with(WIRE_CONNECTION_EAST, state.get(WIRE_CONNECTION_WEST)).with(WIRE_CONNECTION_SOUTH, state.get(WIRE_CONNECTION_NORTH)).with(WIRE_CONNECTION_WEST, state.get(WIRE_CONNECTION_EAST));
            case COUNTERCLOCKWISE_90:
                return state.with(WIRE_CONNECTION_NORTH, state.get(WIRE_CONNECTION_EAST)).with(WIRE_CONNECTION_EAST, state.get(WIRE_CONNECTION_SOUTH)).with(WIRE_CONNECTION_SOUTH, state.get(WIRE_CONNECTION_WEST)).with(WIRE_CONNECTION_WEST, state.get(WIRE_CONNECTION_NORTH));
            case CLOCKWISE_90:
                return state.with(WIRE_CONNECTION_NORTH, state.get(WIRE_CONNECTION_WEST)).with(WIRE_CONNECTION_EAST, state.get(WIRE_CONNECTION_NORTH)).with(WIRE_CONNECTION_SOUTH, state.get(WIRE_CONNECTION_EAST)).with(WIRE_CONNECTION_WEST, state.get(WIRE_CONNECTION_SOUTH));
            default:
                return state;
        }
    }

    public BlockState mirror(BlockState state, BlockMirror mirror) {
        switch(mirror) {
            case LEFT_RIGHT:
                return state.with(WIRE_CONNECTION_NORTH, state.get(WIRE_CONNECTION_SOUTH)).with(WIRE_CONNECTION_SOUTH, state.get(WIRE_CONNECTION_NORTH));
            case FRONT_BACK:
                return state.with(WIRE_CONNECTION_EAST, state.get(WIRE_CONNECTION_WEST)).with(WIRE_CONNECTION_WEST, state.get(WIRE_CONNECTION_EAST));
            default:
                return super.mirror(state, mirror);
        }
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WIRE_CONNECTION_NORTH, WIRE_CONNECTION_EAST, WIRE_CONNECTION_SOUTH, WIRE_CONNECTION_WEST, POWER);
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.abilities.allowModifyWorld) {
            return ActionResult.PASS;
        } else {
            if (isFullyConnected(state) || isNotConnected(state)) {
                BlockState blockState = isFullyConnected(state) ? this.getDefaultState() : this.dotState;
                blockState = blockState.with(POWER, state.get(POWER));
                blockState = this.getPlacementState(world, blockState, pos);
                if (blockState != state) {
                    world.setBlockState(pos, blockState, 3);
                    this.updateForNewState(world, pos, state, blockState);
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        }
    }

    private void updateForNewState(World world, BlockPos pos, BlockState oldState, BlockState newState) {
        Iterator var5 = Direction.Type.HORIZONTAL.iterator();

        while(var5.hasNext()) {
            Direction direction = (Direction)var5.next();
            BlockPos blockPos = pos.offset(direction);
            if (((WireConnection)oldState.get((Property)DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction))).isConnected() != ((WireConnection)newState.get((Property)DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(direction))).isConnected() && world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
                world.updateNeighborsExcept(blockPos, newState.getBlock(), direction.getOpposite());
            }
        }

    }

    static {
        WIRE_CONNECTION_NORTH = Properties.NORTH_WIRE_CONNECTION;
        WIRE_CONNECTION_EAST = Properties.EAST_WIRE_CONNECTION;
        WIRE_CONNECTION_SOUTH = Properties.SOUTH_WIRE_CONNECTION;
        WIRE_CONNECTION_WEST = Properties.WEST_WIRE_CONNECTION;
        POWER = Properties.POWER;
        DIRECTION_TO_WIRE_CONNECTION_PROPERTY = Maps.newEnumMap((Map)ImmutableMap.of(Direction.NORTH, WIRE_CONNECTION_NORTH, Direction.EAST, WIRE_CONNECTION_EAST, Direction.SOUTH, WIRE_CONNECTION_SOUTH, Direction.WEST, WIRE_CONNECTION_WEST));
        DOT_SHAPE = Block.createCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D);
        field_24414 = Maps.newEnumMap((Map)ImmutableMap.of(Direction.NORTH, Block.createCuboidShape(3.0D, 0.0D, 0.0D, 13.0D, 1.0D, 13.0D), Direction.SOUTH, Block.createCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 16.0D), Direction.EAST, Block.createCuboidShape(3.0D, 0.0D, 3.0D, 16.0D, 1.0D, 13.0D), Direction.WEST, Block.createCuboidShape(0.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D)));
        field_24415 = Maps.newEnumMap((Map)ImmutableMap.of(Direction.NORTH, VoxelShapes.union(field_24414.get(Direction.NORTH), Block.createCuboidShape(3.0D, 0.0D, 0.0D, 13.0D, 16.0D, 1.0D)), Direction.SOUTH, VoxelShapes.union(field_24414.get(Direction.SOUTH), Block.createCuboidShape(3.0D, 0.0D, 15.0D, 13.0D, 16.0D, 16.0D)), Direction.EAST, VoxelShapes.union(field_24414.get(Direction.EAST), Block.createCuboidShape(15.0D, 0.0D, 3.0D, 16.0D, 16.0D, 13.0D)), Direction.WEST, VoxelShapes.union(field_24414.get(Direction.WEST), Block.createCuboidShape(0.0D, 0.0D, 3.0D, 1.0D, 16.0D, 13.0D))));
        field_24466 = new Vec3f[16];

        for(int i = 0; i <= 15; ++i) {
            float f = (float)i / 15.0F;
            float g = f * 0.1F + (f > 0.0F ? 0.4F : 0.3F);
            float h = MathHelper.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
            float j = f * 0.3F + (f > 0.0F ? 0.4F : 0.3F);
            field_24466[i] = new Vec3f(g, h, j);
        }

    }
}
