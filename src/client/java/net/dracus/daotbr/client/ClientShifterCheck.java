package net.dracus.daotbr.client;

import net.minecraft.entity.player.PlayerEntity;

public class ClientShifterCheck {
    private static Class<?> shifterTitanClass;
    private static boolean lookupAttempted = false;

    private static Class<?> resolveShifterTitanClass() {
        if (!lookupAttempted) {
            lookupAttempted = true;
            try {
                shifterTitanClass = Class.forName("daot.ShifterTitan");
            } catch (ClassNotFoundException e) {
                shifterTitanClass = null;
            }
        }
        return shifterTitanClass;
    }

    public static boolean isRidingShifterTitan(PlayerEntity player) {
        Class<?> titanClass = resolveShifterTitanClass();
        if (titanClass == null) return false;
        return player.hasVehicle() && titanClass.isInstance(player.getVehicle());
    }
}
