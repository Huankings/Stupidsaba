package pro.fazeclan.river.stupid_express.mixin.role.initiate;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SERoles;

import java.util.ArrayList;
import java.util.Collections;

@Mixin(GameFunctions.class)
public abstract class InitiateKillMixin {
    private static final ResourceLocation NOELLES_AMNESIAC_ID = ResourceLocation.fromNamespaceAndPath("noellesroles", "amnesiac");

    @Inject(
            method = "killPlayer(Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;)V",
            at = @At("HEAD")
    )
    private static void initiateKill(Player victim, boolean spawnBody, Player killer, ResourceLocation deathReason, CallbackInfo ci) {
        if (!(victim instanceof ServerPlayer)) {
            return;
        }

        var level = (ServerLevel) victim.level();
        var gameWorldComponent = GameWorldComponent.KEY.get(level);

        if (!gameWorldComponent.isRole(victim, SERoles.INITIATE)) {
            return;
        }
        if (killer != null && gameWorldComponent.isRole(killer, SERoles.INITIATE)) {
            killer.getInventory().removeItem(WatheItems.KNIFE.getDefaultInstance());
            var shuffledKillerRoles = new ArrayList<>(WatheRoles.ROLES);
            shuffledKillerRoles.removeIf(role -> Harpymodloader.VANNILA_ROLES.contains(role) || !role.canUseKiller() || HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().getPath()));
            if (shuffledKillerRoles.isEmpty()) shuffledKillerRoles.add(WatheRoles.KILLER);
            Collections.shuffle(shuffledKillerRoles);

            var role = shuffledKillerRoles.getFirst();
            gameWorldComponent.addRole(killer, role);
            ModdedRoleAssigned.EVENT.invoker().assignModdedRole(killer, role);
            sendRoleAnnouncement((ServerPlayer) killer, gameWorldComponent, role);
        }
    }

    @Inject(
            method = "killPlayer(Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void initiateKillNonInitiate(
            Player victim,
            boolean spawnBody,
            Player killer,
            ResourceLocation deathReason,
            CallbackInfo ci
    ) {
        if (!(victim instanceof ServerPlayer)) {
            return;
        }

        var level = (ServerLevel) victim.level();
        var gameWorldComponent = GameWorldComponent.KEY.get(level);
        if (!gameWorldComponent.isRole(victim, SERoles.INITIATE) && killer != null && gameWorldComponent.isRole(killer, SERoles.INITIATE)) {
            Role newInitiateRole = selectConfiguredFallbackRole();
            for (ServerPlayer player : level.getPlayers(p -> gameWorldComponent.isRole(p, SERoles.INITIATE))) {
                player.getInventory().removeItem(WatheItems.KNIFE.getDefaultInstance());
                gameWorldComponent.addRole(player, newInitiateRole);
                ModdedRoleAssigned.EVENT.invoker().assignModdedRole(player, newInitiateRole);
                sendRoleAnnouncement(player, gameWorldComponent, newInitiateRole);
            }
            if (!spawnBody) {
                victim.teleportTo(killer.getX(), killer.getY(), killer.getZ());
                victim.fallDistance = 0.0f;
            }
            GameFunctions.killPlayer(killer, true, null, StupidExpress.id("failed_initiation"));
            ci.cancel();
        } else if (gameWorldComponent.isRole(victim, SERoles.INITIATE) && killer != null && !gameWorldComponent.isRole(killer, SERoles.INITIATE)) {
            Role newInitiateRole = selectConfiguredFallbackRole();
            for (ServerPlayer player : level.getPlayers(p -> gameWorldComponent.isRole(p, SERoles.INITIATE))) {
                player.getInventory().removeItem(WatheItems.KNIFE.getDefaultInstance());
                gameWorldComponent.addRole(player, newInitiateRole);
                ModdedRoleAssigned.EVENT.invoker().assignModdedRole(player, newInitiateRole);
                sendRoleAnnouncement(player, gameWorldComponent, newInitiateRole);
            }
        }
    }

    private static Role selectConfiguredFallbackRole() {
        return switch (StupidExpress.CONFIG.rolesSection.initiateSection.initiateFallbackRole) {
            case KILLER -> selectRandomKillerFallback();
            case NEUTRAL -> selectRandomNeutralFallback();
            case null, default -> fallbackAmnesiacRole();
        };
    }

    private static Role selectRandomKillerFallback() {
        var shuffledKillerRoles = new ArrayList<>(WatheRoles.ROLES);
        shuffledKillerRoles.removeIf(role -> Harpymodloader.VANNILA_ROLES.contains(role)
                || !role.canUseKiller()
                || HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().getPath()));
        if (shuffledKillerRoles.isEmpty()) {
            shuffledKillerRoles.add(WatheRoles.KILLER);
        }
        Collections.shuffle(shuffledKillerRoles);
        return shuffledKillerRoles.getFirst();
    }

    private static Role selectRandomNeutralFallback() {
        Role amnesiac = resolveNoellesAmnesiacRole();
        var shuffledNeutralRoles = new ArrayList<>(WatheRoles.ROLES);
        shuffledNeutralRoles.removeIf(role -> Harpymodloader.VANNILA_ROLES.contains(role)
                || role.canUseKiller()
                || role.isInnocent()
                || role.equals(amnesiac)
                || HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().getPath()));
        if (shuffledNeutralRoles.isEmpty()) {
            shuffledNeutralRoles.add(fallbackAmnesiacRole());
        }
        Collections.shuffle(shuffledNeutralRoles);
        return shuffledNeutralRoles.getFirst();
    }

    private static Role fallbackAmnesiacRole() {
        Role noellesAmnesiac = resolveNoellesAmnesiacRole();
        /*
         * 失忆患者已经搬到 NoellesRoles，这里只通过 noellesroles:amnesiac 软查注册表。
         * 当 NoellesRoles 没安装、被禁用或初始化顺序导致暂时不可见时，初学者仍要安全落到 Wathe 原生中立。
         */
        return noellesAmnesiac != null ? noellesAmnesiac : WatheRoles.LOOSE_END;
    }

    private static Role resolveNoellesAmnesiacRole() {
        return WatheRoles.getRole(NOELLES_AMNESIAC_ID);
    }

    private static void sendRoleAnnouncement(ServerPlayer player, GameWorldComponent gameWorldComponent, Role role) {
        RoleAnnouncementTexts.RoleAnnouncementText announcement = Harpymodloader.VANNILA_ROLES.contains(role)
                ? vanillaAnnouncementFor(role)
                : Harpymodloader.autogeneratedAnnouncements.get(role);
        if (announcement == null) {
            /*
             * 软引用职业在极端加载顺序下可能还没有自动公告。
             * 用 Loose End 公告兜底，避免因为缺少 Noelles 生成公告而让击杀流程崩溃。
             */
            announcement = RoleAnnouncementTexts.LOOSE_END;
        }
        ServerPlayNetworking.send(
                player,
                new AnnounceWelcomePayload(
                        RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(announcement),
                        gameWorldComponent.getAllKillerTeamPlayers().size(),
                        0
                )
        );
    }

    private static RoleAnnouncementTexts.RoleAnnouncementText vanillaAnnouncementFor(Role role) {
        if (role.equals(WatheRoles.KILLER)) {
            return RoleAnnouncementTexts.KILLER;
        }
        if (role.equals(WatheRoles.VIGILANTE)) {
            return RoleAnnouncementTexts.VIGILANTE;
        }
        if (role.equals(WatheRoles.LOOSE_END)) {
            return RoleAnnouncementTexts.LOOSE_END;
        }
        return RoleAnnouncementTexts.CIVILIAN;
    }

}
