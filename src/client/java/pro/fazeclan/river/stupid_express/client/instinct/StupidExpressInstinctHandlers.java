package pro.fazeclan.river.stupid_express.client.instinct;

import pro.fazeclan.river.stupid_express.client.instinct.modifier.dual_personality.DualPersonalityInstinctHandler;
import pro.fazeclan.river.stupid_express.client.instinct.modifier.lovers.LoversInstinctHandler;
import pro.fazeclan.river.stupid_express.client.instinct.role.initiate.InitiateInstinctHandler;
import pro.fazeclan.river.stupid_express.client.instinct.role.thief.ThiefInstinctHandler;

public final class StupidExpressInstinctHandlers {
    public static final int PRIORITY_DUAL_PERSONALITY = 10000;
    public static final int PRIORITY_ROLE_INSTINCT_COLOR = 100;
    public static final int PRIORITY_MARK_COLOR = 100;

    private StupidExpressInstinctHandlers() {
    }

    public static void register() {
        DualPersonalityInstinctHandler.register();
        ThiefInstinctHandler.register();
        LoversInstinctHandler.register();
        InitiateInstinctHandler.register();
    }
}
