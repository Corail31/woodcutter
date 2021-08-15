package ovh.corail.woodcutter.compatibility;

import net.minecraft.util.StringRepresentable;
import net.minecraftforge.fml.ModList;

import java.util.Optional;

public enum SupportMods implements StringRepresentable {
    BIOMESOPLENTY("biomesoplenty"),
    QUARK("quark"),
    TWILIGHT_FOREST("twilightforest"),
    EXTENSION_BYG("corail_woodcutter_extension_byg"),
    BYG("byg"),
    TROPICRAFT("tropicraft");

    private final String modid;
    private final boolean loaded;

    SupportMods(String modid) {
        this.modid = modid;
        this.loaded = Optional.ofNullable(ModList.get()).map(list -> list.getModContainerById(modid).isPresent()).orElse(false);
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    @Override
    public String getSerializedName() {
        return this.modid;
    }
}
