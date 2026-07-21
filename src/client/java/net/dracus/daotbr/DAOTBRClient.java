package net.dracus.daotbr;

import net.dracus.daotbr.network.ZoneUpdatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.dracus.daotbr.client.DaotbrJourneyMapPlugin;

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

    }
}
