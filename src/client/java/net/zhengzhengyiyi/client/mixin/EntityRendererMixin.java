package net.zhengzhengyiyi.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backports 1.21.11's leash-aware frustum culling to 1.20.4's MobEntityRenderer.
 *
 * <p><b>Problem in 1.20.4:</b> {@code EntityRenderer.shouldRender()} only tests the
 * entity's own {@code getVisibilityBoundingBox()} against the frustum. When a leashed
 * mob is just off-screen but its holder (another entity or a fence post) is still
 * visible, the mob is culled — causing the leash rope to render with one endpoint
 * missing, producing a "dangling rope" visual artefact.
 *
 * <p><b>1.21.11 fix:</b> After the entity's own box fails the frustum test,
 * {@code EntityRenderer.shouldRender()} additionally checks:
 * <ol>
 *   <li>The leash holder's bounding box alone.</li>
 *   <li>The union of the entity's box and the holder's box.</li>
 * </ol>
 * If either passes, the entity stays visible.
 *
 * <p><b>Why MobEntityRenderer, not EntityRenderer:</b> In 1.20.4 leash support lives
 * on {@code MobEntity.getHoldingEntity()} — there is no {@code Leashable} interface
 * (that was added in 1.21). Targeting {@code MobEntityRenderer} lets us access the
 * {@code MobEntity} type directly without reflection or unsafe casts.
 */
@Environment(EnvType.CLIENT)
@Mixin(MobEntityRenderer.class)
public abstract class EntityRendererMixin<T extends MobEntity, M extends EntityModel<T>>
    extends LivingEntityRenderer<T, M> {

    protected EntityRendererMixin(
        net.minecraft.client.render.entity.EntityRendererFactory.Context ctx,
        M model,
        float shadowRadius
    ) {
        super(ctx, model, shadowRadius);
    }

    /**
     * Extends the vanilla frustum-cull logic with a leash-holder check, matching
     * the behaviour added in 1.21.11's {@code EntityRenderer.shouldRender()}.
     *
     * <p>We inject at HEAD with cancellable=true and always set the return value
     * ourselves so we can insert the extra leash path cleanly without needing to
     * duplicate the vanilla method.
     */
    @Inject(
        method = "shouldRender",
        at = @At("HEAD"),
        cancellable = true
    )
    private void renderer$leashAwareFrustumCull(
        T entity,
        Frustum frustum,
        double x,
        double y,
        double z,
        CallbackInfoReturnable<Boolean> cir
    ) {
        // --- Step 1: entity's own distance-based render check (vanilla) ---
        if (!entity.shouldRender(x, y, z)) {
            cir.setReturnValue(false);
            return;
        }

        // --- Step 2: ignoreCameraFrustum guard (vanilla) ---
        // 1.21.11 surfaces this as canBeCulled(), but the semantics are identical.
        if (entity.ignoreCameraFrustum) {
            cir.setReturnValue(true);
            return;
        }

        // --- Step 3: entity's visibility box vs frustum (vanilla) ---
        Box entityBox = entity.getVisibilityBoundingBox().expand(0.5);
        if (entityBox.isNaN() || entityBox.getAverageSideLength() == 0.0) {
            entityBox = new Box(
                entity.getX() - 2.0, entity.getY() - 2.0, entity.getZ() - 2.0,
                entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0
            );
        }

        if (frustum.isVisible(entityBox)) {
            cir.setReturnValue(true);
            return;
        }

        // --- Step 4 (NEW — ported from 1.21.11): leash-holder frustum test ---
        // In 1.20.4, leash is on MobEntity as getHoldingEntity().
        Entity holder = entity.getHoldingEntity();
        if (holder != null) {
            Box holderBox = holder.getVisibilityBoundingBox().expand(0.5);
            if (holderBox.isNaN() || holderBox.getAverageSideLength() == 0.0) {
                holderBox = new Box(
                    holder.getX() - 2.0, holder.getY() - 2.0, holder.getZ() - 2.0,
                    holder.getX() + 2.0, holder.getY() + 2.0, holder.getZ() + 2.0
                );
            }
            // Keep the mob visible if the holder is on-screen, or if the combined
            // bounding region of mob + holder intersects the frustum.
            if (frustum.isVisible(holderBox) || frustum.isVisible(entityBox.union(holderBox))) {
                cir.setReturnValue(true);
                return;
            }
        }

        cir.setReturnValue(false);
    }
}
