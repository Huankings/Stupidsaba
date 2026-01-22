package pro.fazeclan.river.stupid_express;

import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

public class StupidExpressConfig extends Config {

    public StupidExpressConfig() {
        super(StupidExpress.id("config"));
    }

    public RolesSection rolesSection = new RolesSection();
    public static class RolesSection extends ConfigSection {

        public NecromancerSection necromancerSection = new NecromancerSection();
        public static class NecromancerSection extends ConfigSection {
            public boolean necromancerHasShop = false;
        }

        public ArsonistSection arsonistSection = new ArsonistSection();
        public static class ArsonistSection extends ConfigSection {
            public boolean arsonistKeepsGameGoing = false;
        }

        public InitiateSection initiateSection = new InitiateSection();
        public static class InitiateSection extends ConfigSection {

            public enum InitiateFallbackOptions implements EnumTranslatable {
                AMNESIAC,
                KILLER,
                NEUTRAL;

                @Override
                public @NotNull String prefix() {
                    return "stupid_express.config.initiate_fallback_options";
                }
            }

            public InitiateFallbackOptions initiateFallbackRole = InitiateFallbackOptions.AMNESIAC;
        }

        public AmnesiacSection amnesiacSection = new AmnesiacSection();
        public static class AmnesiacSection extends ConfigSection {
            public boolean bodiesGlowToAmnesiac = true;
            public boolean amnesiacGlowsDifferently = true;
        }

    }

    public ModifiersSection modifiersSection = new ModifiersSection();
    public static class ModifiersSection extends ConfigSection {

        public LoversSection loversSection = new LoversSection();
        public static class LoversSection extends ConfigSection {
            public boolean loversKnowImmediately = true;
            public boolean loversWinWithKillers = false;
            public boolean loversWinWithCivilians = true;
            public boolean loversGlowToEachother = false;
        }

    }

    @Override
    public int defaultPermLevel() {
        return 2;
    }

    @Override
    public @NotNull FileType fileType() {
        return FileType.JSON5;
    }

    @Override
    public @NotNull SaveType saveType() {
        return SaveType.SEPARATE;
    }
}
