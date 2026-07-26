package net.dracus.daotbr;

import net.dracus.daotbr.effect.ModEffects;
import net.dracus.daotbr.network.ZoneUpdatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.dracus.daotbr.client.DaotbrJourneyMapPlugin;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class DAOTBRClient implements ClientModInitializer {
    @java.lang.Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ZoneUpdatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                DaotbrJourneyMapPlugin plugin = DaotbrJourneyMapPlugin.getInstance();
                if (plugin != null) {
                    plugin.updateZoneRing(payload.centerX(), payload.centerZ(), payload.radius());
                }
            });
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (!client.player.hasStatusEffect(ModEffects.SHIFTER_INCAPACITATED)) return;

            client.options.attackKey.setPressed(false);
            client.options.useKey.setPressed(false);
        });
    }
}
