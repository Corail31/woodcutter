package ovh.corail.woodcutter.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import ovh.corail.woodcutter.helper.Helper;
import ovh.corail.woodcutter.inventory.WoodcutterContainer;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModMenuTypes {
    public static MenuType<WoodcutterContainer> WOODCUTTER = Helper.unsafeNullCast();

    static void onRegister(final RegisterEvent event) {
        WOODCUTTER = new MenuType<>(WoodcutterContainer::new);
        event.register(ForgeRegistries.Keys.MENU_TYPES, new ResourceLocation(MOD_ID, "woodcutter"), () -> WOODCUTTER);
    }
}
