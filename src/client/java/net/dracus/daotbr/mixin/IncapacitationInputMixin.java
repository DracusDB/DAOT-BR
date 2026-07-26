package net.dracus.daotbr.mixin;

import net.dracus.daotbr.client.ClientShifterCheck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.dracus.daotbr.effect.ModEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(KeyboardInput.class)
public class IncapacitationInputMixin {

    @Inject(method = "tick(ZF)V", at = @At("TAIL"))
    private void daotbr$suppressWhileIncapacitated(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!client.player.hasStatusEffect(ModEffects.SHIFTER_INCAPACITATED)) return;
        if (ClientShifterCheck.isRidingShifterTitan(client.player)) return; // let titan controls through


        KeyboardInput self = (KeyboardInput) (Object) this;
//        self.movementForward = 0f;
//        self.movementSideways = 0f;
//        self.jumping = false;
        self.sneaking = false;
//        self.pressingForward = false;
//        self.pressingBack = false;
//        self.pressingLeft = false;
//        self.pressingRight = false;
    }
}