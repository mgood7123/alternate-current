package alternate.current.redstone;

import alternate.current.AlternateCurrentMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

/**
 * This interface should be implemented by each wire block type.
 * While Vanilla only has one wire block type, they could add
 * more in the future, and any mods that add more wire block
 * types that wish to take advantage of Alternate Current's
 * performance improvements should have those wire blocks
 * implement this interface.
 * 
 * @author Space Walker
 */
public interface WireBlock {
	/**
	 * overridden by wires which provide power to {@link #ALL} or the identifier returned by {@link #getWireId()}
	 * <br>
	 * <br>
	 */
	static final AlternateCurrentMod.AC_Registry.Registry NONE = new AlternateCurrentMod.AC_Registry.Registry();

	/**
	 * *NOTE* provide power to, or receive power from, all wires
	 * <br>
	 * <br>
	 */
	static final AlternateCurrentMod.AC_Registry.Registry ALL = new AlternateCurrentMod.AC_Registry.Registry();

	/**
	 * specifies what wires can give power to this wire
	 * <br>
	 * <br>
	 * the default implementation returns {@link #NONE}
	 * <br>
	 * <br>
	 * *NOTE* {@link #NONE} is overridden by wires which provide power to {@link #ALL} or the identifier returned by {@link #getWireId()}
	 * <br>
	 * <br>
	 * @param registry - add identifiers to this registry, <br>  for example:<br>{@code return registry.add("minecraft:redstone_wire");}
	 * @return {@link #NONE}, {@link #ALL}, or registry (registry must not be empty)
	 */
	public default AlternateCurrentMod.AC_Registry.Registry canBePoweredBy(AlternateCurrentMod.AC_Registry.MutableRegistry registry) {
		return NONE;
	}

	/**
	 * specifies what wires can receive power from this wire
	 * <br>
	 * <br>
	 * the default implementation returns {@link #NONE}
	 * <br>
	 * <br>
	 * *NOTE* {@link #NONE} is overridden by wires which provide power to {@link #ALL} or the identifier returned by {@link #getWireId()}
	 * <br>
	 * <br>
	 * @param registry - add identifiers to this registry, <br>  for example:<br>{@code return registry.add("minecraft:redstone_wire");}
	 * @return {@link #NONE}, {@link #ALL}, or registry (registry must not be empty)
	 */
	public default AlternateCurrentMod.AC_Registry.Registry canPower(AlternateCurrentMod.AC_Registry.MutableRegistry registry) {
		return NONE;
	}

	public default Block asBlock() {
		return (Block)this;
	}
	
	public default boolean isOf(BlockState state) {
		return asBlock() == state.getBlock();
	}
	
	/**
	 * The lowest possible power level a wire can have.
	 */
	public int getMinPower();
	
	/**
	 * The largest possible power level a wire can have.
	 */
	public int getMaxPower();
	
	/**
	 * The drop in power level from one wire to the next.
	 */
	public int getPowerStep();
	
	default int clampPower(int power) {
		return MathHelper.clamp(power, getMinPower(), getMaxPower());
	}
	
	/**
	 * Return the power level of the given wire based on its
	 * location and block state.
	 */
	public int getPower(WorldAccess world, BlockPos pos, BlockState state);
	
	/**
	 * Return a block state that holds the given new power level.
	 */
	public BlockState updatePowerState(WorldAccess world, BlockPos pos, BlockState state, int power);
	
	/**
	 * Find the connections between the given WireNode and
	 * neighboring WireNodes.
	 */
	public void findWireConnections(WireNode wire, WireHandler.NodeProvider nodeProvider);

	/**
	 * useful when debugging wires, returns the wire's <b>registered block id</b>
	 */
	public default String getWireId() {
		return Registry.BLOCK.getId(asBlock()).toString();
	};

	/**
	 * returns a reference to the current {@link WireHandler}
	 * <br>
	 * <br>
	 * this <b>MUST</b> be implemented
	 * <br>
	 * this <b>MUST NOT</b> return <b>null</b>
	 * <br>
	 * <br>
	 */
	WireHandler getWireHandler();

	/**
	 * sets the reference to the current {@link WireHandler}
	 * <br>
	 * <br>
	 * this <b>MUST</b> be implemented
	 * <br>
	 * <br>
	 */
	void setWireHandler(WireHandler wireHandler);

	/**
	 * return <b>true</b> here if the registry should be <b>rebuilt</b> on <b>block update</b>
	 * <br>
	 * <br>
	 * this can be useful when testing wire interaction
	 * <br>
	 * <br>
	 */
	default boolean rebuildRegistryOnUpdate() {
		return false;
	}
}
