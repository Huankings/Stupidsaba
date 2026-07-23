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

        public DualPersonalitySection dualPersonalitySection = new DualPersonalitySection();
        public static class DualPersonalitySection extends ConfigSection {
            /**
             * 是否允许双重人格在杀手阵营胜利时一起获胜。
             *
             * <p>默认 false，表示只要还有双重人格活着，普通杀手胜利会被延后；
             * 开启后，至少有一名存活双重人格不是 innocent 时，杀手胜利可以正常结算。</p>
             */
            public boolean dualPersonalityWinWithKillers = false;

            /**
             * 是否允许双重人格在乘客阵营胜利时一起获胜。
             *
             * <p>默认 false，表示双重人格作为独立胜利目标存在；
             * 开启后，乘客胜利不会因为仍有双重人格存活而被拦截。</p>
             */
            public boolean dualPersonalityWinWithCivilians = false;

            /**
             * 双重人格进入随机词条池所需的最少参局人数。
             *
             * <p>强制指定指令不受这个值影响；它只控制 Harpy 随机分配时是否可能抽到双重人格。</p>
             */
            public int dualPersonalityMinPlayerSpawn = 8;
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
