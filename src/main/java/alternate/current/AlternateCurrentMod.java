package alternate.current;

import alternate.current.redstone.block.InfinityWire;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import alternate.current.util.profiler.ACProfiler;
import alternate.current.util.profiler.Profiler;

import net.fabricmc.api.ModInitializer;

public class AlternateCurrentMod implements ModInitializer {
	
	public static final String MOD_ID = "alternatecurrent";
	public static final String MOD_NAME = "Alternate Current";
	public static final String MOD_VERSION = "1.1.0";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
	public static final boolean DEBUG = false;
	
	public static boolean on = true;

	public static Block InfinityWireBlock;
	public static BlockItem InfinityWireBlockItem;

	@Override
	public void onInitialize() {
		LOGGER.info(String.format("%s %s has been initialized!", MOD_NAME, MOD_VERSION));
		
		if (DEBUG) {
			LOGGER.warn(String.format("You are running a DEBUG version of %s!", MOD_NAME));
		}
		InfinityWireBlock = new InfinityWire(FabricBlockSettings.of(Material.DECORATION).noCollision().breakInstantly().nonOpaque());
		InfinityWireBlockItem = new BlockItem(InfinityWireBlock, new FabricItemSettings().group(ItemGroup.REDSTONE));
		Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "infinity_wire"), InfinityWireBlock);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "infinity_wire"), InfinityWireBlockItem);

		BlockRenderLayerMap.INSTANCE.putBlock(InfinityWireBlock, RenderLayer.getCutout());

		ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> InfinityWire.getWireColor(state.get(InfinityWire.POWER)), InfinityWireBlock);
	}
	
	public static Profiler createProfiler() {
		return DEBUG ? new ACProfiler() : Profiler.DUMMY;
	}
}
