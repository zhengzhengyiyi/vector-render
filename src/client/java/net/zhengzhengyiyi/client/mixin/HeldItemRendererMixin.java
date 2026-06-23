package net.zhengzhengyiyi.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ports 1.21.11's blocking-pose item transform to 1.20.4, fixing the model gap
 * that appears when holding a non-shield item in the {@code UseAction.BLOCK} pose.
 *
 * <p><b>Problem in 1.20.4:</b> When a player raises their arm to block while
 * holding a non-shield item (sword, axe, etc.), {@code renderFirstPersonItem()}
 * only calls {@code applyEquipOffset()} in the {@code BLOCK} switch case. The item
 * stays in its default held transform while the arm rotates into the blocking pose,
 * leaving a visible gap between the arm mesh and the item model.
 *
 * <p><b>1.21.11 fix:</b> A dedicated transform is applied after the equip offset
 * for non-shield blocking items:
 * <pre>
 *   matrices.translate(side * -0.14142136F,  0.08F, 0.14142136F);
 *   matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25F));
 *   matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side *  13.365F));
 *   matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side *  78.05F));
 * </pre>
 * This rotates the item ~102° around X so it aligns flush against the raised arm.
 * Shields are excluded because they have their own model-defined arm offset.
 *
 * <p><b>Implementation:</b> We {@code @Redirect} the {@code applyEquipOffset} call
 * that is unique to the {@code BLOCK} branch (ordinal 4 among all
 * {@code applyEquipOffset} invocations inside {@code renderFirstPersonItem}), then
 * conditionally append the extra rotation for non-shield items.
 *
 * <p>Ordinal breakdown of {@code applyEquipOffset} calls in
 * {@code renderFirstPersonItem}:
 * <ol start="0">
 *   <li>0 — crossbow, actively pulling</li>
 *   <li>1 — crossbow, idle / swinging</li>
 *   <li>2 — {@code UseAction.NONE}</li>
 *   <li>3 — {@code UseAction.EAT} / {@code DRINK}</li>
 *   <li><b>4 — {@code UseAction.BLOCK}  ← patched here</b></li>
 *   <li>5 — {@code UseAction.BOW}</li>
 *   <li>6 — {@code UseAction.SPEAR}</li>
 *   <li>7 — riptide active</li>
 *   <li>8 — idle / normal swing</li>
 * </ol>
 */
@Environment(EnvType.CLIENT)
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    /** Exposed so the redirect body can call back into the real method. */
    @Shadow
    protected abstract void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);

    /**
     * Redirects the {@code applyEquipOffset} call inside the {@code BLOCK} switch
     * case (ordinal 4) of {@code renderFirstPersonItem}.
     *
     * <p>The redirect calls the real {@code applyEquipOffset} via {@code @Shadow},
     * then — for non-shield blocking items — appends the 1.21.11 rotation that
     * closes the arm/item gap.
     *
     * <p>Because this is a {@code @Redirect} on the {@code this.applyEquipOffset}
     * INVOKE, the parameters present at the call site (matrices, arm, equipProgress)
     * are passed directly. We also capture the {@code item} and {@code arm} locals
     * that are live at that bytecode position to condition the extra transform.
     */
    @Redirect(
        method = "renderFirstPersonItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
            ordinal = 4
        )
    )
    private void renderer$blockingItemTransform(
        HeldItemRenderer self,
        MatrixStack matrices,
        Arm arm,
        float equipProgress,
        // Locals captured from the enclosing renderFirstPersonItem frame at the
        // call site of the ordinal-4 applyEquipOffset:
        AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        Hand hand,
        float swingProgress,
        ItemStack item
    ) {
        // Always apply the vanilla equip offset first.
        this.applyEquipOffset(matrices, arm, equipProgress);

        // Only add the gap-fix rotation for non-shield blocking items.
        // Shields have their own model/arm offset and must not be altered.
        if (item.getUseAction() != UseAction.BLOCK || item.getItem() instanceof ShieldItem) {
            return;
        }

        int side = arm == Arm.RIGHT ? 1 : -1;

        // 1.21.11 blocking transform — values taken verbatim from the 1.21.11
        // renderFirstPersonItem BLOCK case. Rotates the item flush against the
        // raised forearm to eliminate the visible model gap.
        matrices.translate(side * -0.14142136F, 0.08F, 0.14142136F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 13.365F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 78.05F));
    }
}
