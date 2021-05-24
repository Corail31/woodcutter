package ovh.corail.woodcutter.registry;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import ovh.corail.woodcutter.block.WoodcutterBlock;
import ovh.corail.woodcutter.compatibility.SupportMods;
import ovh.corail.woodcutter.item.WoodcutterItem;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlocks {

    public static final Set<Block> WOODCUTTERS = new HashSet<>();
    public static final Set<Item> WOODCUTTER_ITEMS = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static ItemStack RANDOM_STACK = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onRegisterBlocks(final RegistryEvent.Register<Block> event) {
        for (VanillaWoodVariant variant : VanillaWoodVariant.values()) {
            registerWoodcutter(event.getRegistry(), variant.getString());
        }
        if (SupportMods.BIOMESOPLENTY.isLoaded()) {
            for (BOPWoodVariant variant : BOPWoodVariant.values()) {
                ModBlocks.registerWoodcutter(event.getRegistry(), variant.getString());
            }
        }
        if (SupportMods.QUARK.isLoaded()) {
            for (QuarkWoodVariant variant : QuarkWoodVariant.values()) {
                ModBlocks.registerWoodcutter(event.getRegistry(), variant.getString() + "_stained");
            }
        }
        if (SupportMods.TWILIGHT_FOREST.isLoaded()) {
            for (TFWoodVariant variant : TFWoodVariant.values()) {
                ModBlocks.registerWoodcutter(event.getRegistry(), variant.getString());
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterBlockItems(final RegistryEvent.Register<Item> event) {
        WOODCUTTERS.forEach(woodcutterBlock -> {
            Item woodcutter = new WoodcutterItem(woodcutterBlock).setRegistryName(woodcutterBlock.getRegistryName());
            WOODCUTTER_ITEMS.add(woodcutter);
            event.getRegistry().register(woodcutter);
        });
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

    public enum VanillaWoodVariant implements IStringSerializable {
        OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK, CRIMSON, WARPED;
        private final String name;

        VanillaWoodVariant() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getString() {
            return this.name;
        }
    }

    public enum BOPWoodVariant implements IStringSerializable {
        CHERRY, DEAD, FIR, HELLBARK, JACARANDA, MAGIC, MAHOGANY, PALM, REDWOOD, UMBRAN, WILLOW;
        private final String name;

        BOPWoodVariant() {
            this.name = name().toLowerCase(Locale.US);
        }

        @Override
        public String getString() {
            return this.name;
        }
    }

    public enum QuarkWoodVariant implements IStringSerializable {
        BLACK, BLUE, BROWN, CYAN, GRAY, GREEN, LIGHT_BLUE, LIGHT_GRAY, LIME, MAGENTA, ORANGE, PINK, PURPLE, RED, WHITE, YELLOW;
        private final String name, plankName;

        QuarkWoodVariant() {
            this.name = name().toLowerCase(Locale.US);
            this.plankName = this.name + "_stained_planks";
        }

        @Override
        public String getString() {
            return this.name;
        }

        public String getPlankName() {
            return this.plankName;
        }
    }

    public enum TFWoodVariant implements IStringSerializable {
        TWILIGHT_OAK, CANOPY, MANGROVE, DARK("darkwood"), TIME("timewood"), TRANS("transwood"), MINE("mining"), SORT("sortwood");

        private final String name, logTag;

        TFWoodVariant(String logTag) {
            this.name = name().toLowerCase(Locale.US);
            this.logTag = logTag + "_logs";
        }

        TFWoodVariant() {
            this.name = name().toLowerCase(Locale.US);
            this.logTag = this.name + "_logs";
        }

        @Override
        public String getString() {
            return this.name;
        }

        public String getLogTag() {
            return this.logTag;
        }

        public String getSignName() {
            return (this == DARK ? "darkwood" : this.name) + "_sign";
        }
    }
}
