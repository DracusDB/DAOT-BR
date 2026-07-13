package net.dracus.daotbr.mixin;//package net.dracus.daotbr.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)


public class ItemEntityMixin {
    @Unique
    private static final TagKey<net.minecraft.item.Item> EXPLOSION_IMMUNE =
            TagKey.of(RegistryKeys.ITEM, Identifier.of("daotbr", "explosion_immune"));


    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void daotbr$blockExplosionDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;

        if (source.isIn(DamageTypeTags.IS_EXPLOSION) && self.getStack().isIn(EXPLOSION_IMMUNE)) {
            cir.setReturnValue(false);
        }
    }
}
