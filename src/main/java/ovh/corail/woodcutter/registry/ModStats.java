package ovh.corail.woodcutter.registry;

import net.minecraft.stats.IStatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.util.ResourceLocation;

import static ovh.corail.woodcutter.WoodCutterMod.MOD_ID;

public class ModStats {
    public static final ResourceLocation INTERACT_WITH_SAWMILL = register("interact_with_sawmill");

    private static ResourceLocation register(String name) {
        return Stats.registerCustom(MOD_ID + ":" + name, IStatFormatter.DEFAULT);
    }
}
