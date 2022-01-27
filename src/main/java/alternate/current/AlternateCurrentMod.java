package alternate.current;

import alternate.current.redstone.block.InfinityWire;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import alternate.current.util.profiler.ACProfiler;
import alternate.current.util.profiler.Profiler;

import net.fabricmc.api.ModInitializer;

import java.util.ArrayList;
import java.util.List;

public class AlternateCurrentMod implements ModInitializer {
	
	public static final String MOD_ID = "alternatecurrent";
	public static final String MOD_NAME = "Alternate Current";
	public static final String MOD_VERSION = "1.1.0";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
	public static final boolean DEBUG = false;
	
	public static boolean on = true;

	private static class Reg {
		static class Info {
			Identifier identifier;
			Block block;

			public Info(Identifier identifier, Block block) {
				this.identifier = identifier;
				this.block = block;
			}
		}

		private static final ArrayList<Info> wires = new ArrayList<>();

		public static void register(Block block) {
			wires.add(new Info(Registry.BLOCK.getId(block), block));
		}

		public static Block getOrNull(Identifier identifier) {
			for (Info wire : wires) {
				if (wire.identifier.equals(identifier)) {
					return wire.block;
				}
			}
			return null;
		}
	}


	public static class AC_Registry {
		public static class Registry {
			ArrayList<String> string_identifiers;
			ArrayList<Identifier> identifiers;

			public Registry() {}

			public Registry(ArrayList<String> string_identifiers, ArrayList<Identifier> identifiers) {
				this.string_identifiers = string_identifiers;
				this.identifiers = identifiers;
			}

			public boolean isEmpty() {
				return (identifiers == null || identifiers.isEmpty()) && (string_identifiers == null || string_identifiers.isEmpty());
			}

			public List<Identifier> getIdentifiers() {
				ArrayList<Identifier> i = new ArrayList<>();
				if (identifiers != null) {
					i.addAll(identifiers);
				}
				if (string_identifiers != null) {
					for (String string_identifier : string_identifiers) {
						if (string_identifier == null) {
							LOGGER.error("string identifier is null");
							continue;
						}
						Identifier identifier = Identifier.tryParse(string_identifier);
						if (identifier == null) {
							LOGGER.error("could not parse string identifier: " + string_identifier);
							continue;
						}
						i.add(identifier);
					}
				}
				return i;
			}
		}

		public static class MutableRegistry extends Registry {
			public MutableRegistry add(Identifier block_registry_id) {
				if (identifiers == null) {
					identifiers = new ArrayList<>();
				}
				identifiers.add(block_registry_id);
				return this;
			}

			public MutableRegistry add(String block_registry_id) {
				if (string_identifiers == null) {
					string_identifiers = new ArrayList<>();
				}
				string_identifiers.add(block_registry_id);
				return this;
			}
		}

		public static void register(Block block) {
			Reg.register(block);
		}

		public static Block getOrNull(Identifier identifier) {
			return Reg.getOrNull(identifier);
		}
	}

	public static InfinityWire InfinityWireBlock;
	public static BlockItem InfinityWireBlockItem;

	@Override
	public void onInitialize() {
		LOGGER.info(String.format("%s %s has been initialized!", MOD_NAME, MOD_VERSION));
		
		if (DEBUG) {
			LOGGER.warn(String.format("You are running a DEBUG version of %s!", MOD_NAME));
		}

		AC_Registry.register(Blocks.REDSTONE_WIRE);

		InfinityWireBlock = new InfinityWire(FabricBlockSettings.of(Material.DECORATION).noCollision().breakInstantly().nonOpaque());
		InfinityWireBlockItem = new BlockItem(InfinityWireBlock, new FabricItemSettings().group(ItemGroup.REDSTONE));
		Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "infinity_wire"), InfinityWireBlock);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "infinity_wire"), InfinityWireBlockItem);

		AC_Registry.register(InfinityWireBlock);

		BlockRenderLayerMap.INSTANCE.putBlock(InfinityWireBlock, RenderLayer.getCutout());

		ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> InfinityWire.getWireColor(state.get(InfinityWire.POWER)), InfinityWireBlock);
	}
	
	public static Profiler createProfiler() {
		return DEBUG ? new ACProfiler() : Profiler.DUMMY;
	}
}
