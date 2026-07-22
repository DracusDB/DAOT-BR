package net.dracus.daotbr.client;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.common.JourneyMapPlugin;
import net.dracus.daotbr.util.BRFeatures.GameStageManager;
import net.minecraft.util.math.BlockPos;
import journeymap.api.v2.client.util.UIState;


import java.util.ArrayList;
import java.util.List;

@JourneyMapPlugin(apiVersion = "2.0.0")
public class DaotbrJourneyMapPlugin implements IClientPlugin {
    private static DaotbrJourneyMapPlugin INSTANCE;
    private IClientAPI jmAPI;
    private PolygonOverlay zoneOverlay;

    public DaotbrJourneyMapPlugin() {
        INSTANCE = this;
    }

    public static DaotbrJourneyMapPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    public void initialize(final IClientAPI jmClientApi) {
        this.jmAPI = jmClientApi;
    }

    @Override
    public String getModId() {
        return "daotbr";
    }

    public void updateZoneRing(double centerX, double centerZ, double radius) {
        if (jmAPI == null) return;
        if (!jmAPI.playerAccepts("daotbr", DisplayType.Polygon)) return;

        List<BlockPos> points = new ArrayList<>();
        int segments = 128;
        double startAngle = -Math.PI / 4;
        for (int i = 0; i < segments; i++) {
            double angle = startAngle + (2 * Math.PI * i / segments);
            int x = (int) (centerX + radius * Math.cos(angle));
            int z = (int) (centerZ + radius * Math.sin(angle));
            points.add(new BlockPos(x, 64, z));
        }
        MapPolygon polygon = new MapPolygon(points);

        try {
            if (zoneOverlay == null) {
                ShapeProperties props = new ShapeProperties()
                        .setStrokeColor(0xFF0000)
                        .setStrokeWidth(3)
                        .setStrokeOpacity(0.9f)
                        .setFillOpacity(0f);
                zoneOverlay = new PolygonOverlay("daotbr", GameStageManager.ARENA_DIMENSION, props, polygon);
//                zoneOverlay.setMinZoom(2);
//                zoneOverlay.setMaxZoom(UIState.ZOOM_IN_MAX);
            } else {
                zoneOverlay.setOuterArea(polygon);
            }
            jmAPI.show(zoneOverlay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeZoneRing() {
        if (jmAPI != null && zoneOverlay != null) {
            jmAPI.remove(zoneOverlay);
            zoneOverlay = null;
        }
    }
}