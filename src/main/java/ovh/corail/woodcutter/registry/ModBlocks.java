package ovh.corail.woodcutter.registry;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import ovh.corail.woodcutter.block.WoodcutterBlock;
import ovh.corail.woodcutter.compatibility.SupportMods;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlocks {
    enum WoodcutterVariant {OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK}

    enum WoodcutterBOPVariant {CHERRY, DEAD, FIR, HELLBARK, JACARANDA, MAGIC, MAHOGANY, PALM, REDWOOD, UMBRAN, WILLOW}

    enum WoodcutterMidnightVariant {BOGSHROOM, DARK_WILLOW, DEAD_WOOD, DEWSHROOM, NIGHTSHROOM, SHADOWROOT, VIRIDSHROOM}

    public static final Set<Block> WOODCUTTERS = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static ItemStack RANDOM_STACK = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onRegisterBlocks(final RegistryEvent.Register<Block> event) {
        for (WoodcutterVariant variant : WoodcutterVariant.values()) {
            registerWoodcutter(event.getRegistry(), variant.name().toLowerCase());
        }
        if (SupportMods.BIOMESOPLENTY.isLoaded()) {
            for (WoodcutterBOPVariant variant : WoodcutterBOPVariant.values()) {
                ModBlocks.registerWoodcutter(event.getRegistry(), variant.name().toLowerCase());
            }
        }
        if (SupportMods.MIDNIGHT.isLoaded()) {
            for (WoodcutterMidnightVariant variant : WoodcutterMidnightVariant.values()) {
                ModBlocks.registerWoodcutter(event.getRegistry(), variant.name().toLowerCase());
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterBlockItems(final RegistryEvent.Register<Item> event) {
        WOODCUTTERS.forEach(woodcutterBlock -> event.getRegistry().register(new BlockItem(woodcutterBlock, new Item.Properties().group(ModTabs.mainTab)).setRegistryName(woodcutterBlock.getRegistryName())));
    }

    public static void registerWoodcutter(IForgeRegistry<Block> registry, String name) {
        Block woodcutter = new WoodcutterBlock().setRegistryName(MOD_ID, name + "_woodcutter");
        registry.register(woodcutter);
        WOODCUTTERS.add(woodcutter);
    }

    public static ItemStack createRandomStack() {
        if (RANDOM_STACK.isEmpty()) {
            RANDOM_STACK = new ItemStack(ModBlocks.WOODCUTTERS.stream().skip(RANDOM.nextInt(ModBlocks.WOODCUTTERS.size())).findFirst().orElse(Blocks.STONECUTTER));
        }
        return RANDOM_STACK;
    }
}
