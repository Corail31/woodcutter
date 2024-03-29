package ovh.corail.woodcutter.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

public class CustomConfig extends ModConfig {
    public CustomConfig(ForgeConfigSpec spec, ModContainer container) {
        super(Type.SERVER, spec, container, String.format(Locale.US, "%s-%s.toml", container.getModId(), Type.SERVER.extension()));
    }

    @Override
    public ConfigFileTypeHandler getHandler() {
        return CONFIG_FILE_TYPE_HANDLER;
    }

    private static final ConfigFileTypeHandler CONFIG_FILE_TYPE_HANDLER = new ConfigFileTypeHandler() {

        @Override
        public Function<ModConfig, CommentedFileConfig> reader(Path configBasePath) {
            return super.reader(FMLPaths.CONFIGDIR.get());
        }

        @Override
        public void unload(Path configBasePath, ModConfig config) {
            super.unload(FMLPaths.CONFIGDIR.get(), config);
        }
    };
}
