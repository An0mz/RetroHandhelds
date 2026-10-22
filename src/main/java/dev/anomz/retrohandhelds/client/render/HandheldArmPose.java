package dev.anomz.retrohandhelds.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Players seen from outside hold a handheld in front of their chest with both hands, like the
 * first-person view. The item's thirdperson display transform (models/item/handheld.json) is solved
 * for these arm angles, so change both together.
 */
public final class HandheldArmPose {
    /** Arms raised forward (negative is up). */
    private static final float ARM_PITCH = -0.9f;
    /** Arms turned in so the hands meet in the middle. */
    private static final float ARM_YAW = 0.5f;

    private static final IArmPoseTransformer TRANSFORM = (model, entity, arm) -> {
        model.rightArm.xRot = ARM_PITCH;
        model.rightArm.yRot = -ARM_YAW;
        model.rightArm.zRot = 0;
        model.leftArm.xRot = ARM_PITCH;
        model.leftArm.yRot = ARM_YAW;
        model.leftArm.zRot = 0;
    };

    /** Constructor arguments of the RETROHANDHELDS_HANDHELD arm pose (META-INF/enumextensions.json). */
    public static final EnumProxy<HumanoidModel.ArmPose> POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, true, TRANSFORM);

    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return POSE.getValue();
        }
    };

    private HandheldArmPose() {
    }
}
