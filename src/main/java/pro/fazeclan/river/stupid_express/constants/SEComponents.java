package pro.fazeclan.river.stupid_express.constants;

import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;
import pro.fazeclan.river.stupid_express.cca.AbilityCooldownComponent;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversPairComponent;
import pro.fazeclan.river.stupid_express.role.arsonist.cca.DousedPlayerComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerMomentumComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;

public class SEComponents implements EntityComponentInitializer, WorldComponentInitializer {

    public SEComponents() {}

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(Player.class, DousedPlayerComponent.KEY)
                .respawnStrategy(RespawnCopyStrategy.NEVER_COPY)
                .end(DousedPlayerComponent::new);
        registry.beginRegistration(Player.class, AbilityCooldownComponent.KEY)
                .respawnStrategy(RespawnCopyStrategy.NEVER_COPY)
                .end(AbilityCooldownComponent::new);
        registry.beginRegistration(Player.class, ConvenerPlayerComponent.KEY)
                .respawnStrategy(RespawnCopyStrategy.NEVER_COPY)
                .end(ConvenerPlayerComponent::new);
        registry.beginRegistration(Player.class, ConvenerDisguiseComponent.KEY)
                .respawnStrategy(RespawnCopyStrategy.NEVER_COPY)
                .end(ConvenerDisguiseComponent::new);
        registry.beginRegistration(Player.class, ConvenerMomentumComponent.KEY)
                .respawnStrategy(RespawnCopyStrategy.NEVER_COPY)
                .end(ConvenerMomentumComponent::new);
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
        registry.register(LoversPairComponent.KEY, LoversPairComponent::new);
        // 双重人格配对和倒计时是“整局共享状态”，所以注册为世界组件，而不是玩家组件。
        registry.register(DualPersonalityComponent.KEY, DualPersonalityComponent::new);
    }
}
