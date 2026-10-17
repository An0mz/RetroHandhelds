package dev.anomz.retrohandhelds.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.client.session.SessionDisplay;
import dev.anomz.retrohandhelds.item.HandheldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First-person view of a held handheld, posed like a map: two-handed when the off hand is empty,
 * one-handed otherwise. The transforms mirror vanilla's map rendering in ItemInHandRenderer
 * (whose methods are private), drawing the handheld's face and live screen instead of paper.
 */
public final class HandheldInHandRenderer {
    private static final ResourceLocation FACE =
            ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "textures/gui/handheld_face.png");
    // Face texture is 64x40 px; its screen cutout is x 17..46, y 6..25 (30x20 px, 3:2).
    private static final float FACE_PX_W = 64;
    private static final float FACE_PX_H = 40;
    private static final float SCREEN_PX_X = 17;
    private static final float SCREEN_PX_Y = 6;
    private static final float SCREEN_PX_W = 30;
    private static final float SCREEN_PX_H = 20;
    // Vanilla map space: -7..135 across, y down. The face spans the full width, centered vertically.
    private static final float MAP_MIN = -7;
    private static final float MAP_SIZE = 142;
    private static final float UNITS_PER_PX = MAP_SIZE / FACE_PX_W;
    private static final float FACE_TOP = 64 - FACE_PX_H * UNITS_PER_PX / 2;
    // In map space negative z faces the viewer; the screen sits just in front of the face.
    private static final float SCREEN_Z = -0.1f;

    /** How long after a right-click Minecraft's "item used" dip is ignored (it settles in about 3 ticks). */
    private static final long USE_DIP_TICKS = 10;

    private static InteractionHand usedHand;
    private static long usedAt = Long.MIN_VALUE;

    private HandheldInHandRenderer() {
    }

    /**
     * Minecraft lowers and re-raises an item after every use. That suits maps but not a handheld
     * the player just turned on, so the next few ticks ignore it (switching to the item still animates).
     */
    public static void noteUsed(InteractionHand hand, long gameTime) {
        usedHand = hand;
        usedAt = gameTime;
    }

    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (!stack.is(RetroRegistries.HANDHELD) || player == null || player.isScoping()) {
            return;
        }
        event.setCanceled(true);
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        boolean mainHand = event.getHand() == InteractionHand.MAIN_HAND;
        long now = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        float equipProgress = event.getHand() == usedHand && now - usedAt < USE_DIP_TICKS ? 0 : event.getEquipProgress();
        HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();

        poseStack.pushPose();
        if (mainHand && player.getOffhandItem().isEmpty()) {
            renderTwoHanded(minecraft, player, poseStack, buffers, light, event.getInterpolatedPitch(),
                    equipProgress, event.getSwingProgress(), stack);
        } else {
            renderOneHanded(minecraft, player, poseStack, buffers, light, equipProgress, arm,
                    event.getSwingProgress(), stack);
        }
        poseStack.popPose();
    }

    private static void renderTwoHanded(Minecraft minecraft, AbstractClientPlayer player, PoseStack poseStack,
            MultiBufferSource buffers, int light, float pitch, float equipProgress, float swingProgress, ItemStack stack) {
        float swingRoot = Mth.sqrt(swingProgress);
        float lift = -0.2f * Mth.sin(swingProgress * Mth.PI);
        float push = -0.4f * Mth.sin(swingRoot * Mth.PI);
        poseStack.translate(0, -lift / 2, push);
        float tilt = mapTilt(pitch);
        poseStack.translate(0, 0.04f + equipProgress * -1.2f + tilt * -0.5f, -0.72f);
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt * -85));
        if (!player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
            renderHoldingHand(minecraft, player, poseStack, buffers, light, HumanoidArm.RIGHT);
            renderHoldingHand(minecraft, player, poseStack, buffers, light, HumanoidArm.LEFT);
            poseStack.popPose();
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(swingRoot * Mth.PI) * 20));
        poseStack.scale(2, 2, 2);
        renderFace(poseStack, buffers, light, stack);
    }

    private static void renderOneHanded(Minecraft minecraft, AbstractClientPlayer player, PoseStack poseStack,
            MultiBufferSource buffers, int light, float equipProgress, HumanoidArm arm, float swingProgress, ItemStack stack) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(side * 0.125f, -0.125f, 0);
        if (!player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 10));
            renderArm(minecraft, player, poseStack, buffers, light, equipProgress, swingProgress, arm);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(side * 0.51f, -0.08f + equipProgress * -1.2f, -0.75f);
        float swingRoot = Mth.sqrt(swingProgress);
        float swing = Mth.sin(swingRoot * Mth.PI);
        poseStack.translate(side * -0.5f * swing, 0.4f * Mth.sin(swingRoot * Mth.TWO_PI) - 0.3f * swing,
                -0.3f * Mth.sin(swingProgress * Mth.PI));
        poseStack.mulPose(Axis.XP.rotationDegrees(swing * -45));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * swing * -30));
        renderFace(poseStack, buffers, light, stack);
        poseStack.popPose();
    }

    private static float mapTilt(float pitch) {
        float f = Mth.clamp(1 - pitch / 45 + 0.1f, 0, 1);
        return -Mth.cos(f * Mth.PI) * 0.5f + 0.5f;
    }

    /** A hand gripping the side of the handheld (two-handed pose). */
    private static void renderHoldingHand(Minecraft minecraft, AbstractClientPlayer player, PoseStack poseStack,
            MultiBufferSource buffers, int light, HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(92));
        poseStack.mulPose(Axis.XP.rotationDegrees(45));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * -41));
        poseStack.translate(side * 0.3f, -1.1f, 0.45f);
        drawArm(minecraft, player, poseStack, buffers, light, arm);
        poseStack.popPose();
    }

    /** The holding arm in the one-handed pose. */
    private static void renderArm(Minecraft minecraft, AbstractClientPlayer player, PoseStack poseStack,
            MultiBufferSource buffers, int light, float equipProgress, float swingProgress, HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float swingRoot = Mth.sqrt(swingProgress);
        poseStack.translate(side * (-0.3f * Mth.sin(swingRoot * Mth.PI) + 0.64f),
                0.4f * Mth.sin(swingRoot * Mth.TWO_PI) - 0.6f + equipProgress * -0.6f,
                -0.4f * Mth.sin(swingProgress * Mth.PI) - 0.72f);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * Mth.sin(swingRoot * Mth.PI) * 70));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(swingProgress * swingProgress * Mth.PI) * -20));
        poseStack.translate(side * -1, 3.6f, 3.5f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120));
        poseStack.mulPose(Axis.XP.rotationDegrees(200));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -135));
        poseStack.translate(side * 5.6f, 0, 0);
        drawArm(minecraft, player, poseStack, buffers, light, arm);
    }

    private static void drawArm(Minecraft minecraft, AbstractClientPlayer player, PoseStack poseStack,
            MultiBufferSource buffers, int light, HumanoidArm arm) {
        if (!(minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, buffers, light, player);
        } else {
            renderer.renderLeftHand(poseStack, buffers, light, player);
        }
    }

    /** The handheld's face in map space, with the live game on its screen if it is running. */
    private static void renderFace(PoseStack poseStack, MultiBufferSource buffers, int light, ItemStack stack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.scale(0.38f, 0.38f, 0.38f);
        poseStack.translate(-0.5f, -0.5f, 0);
        poseStack.scale(1 / 128f, 1 / 128f, 1 / 128f);
        Matrix4f pose = poseStack.last().pose();

        float faceBottom = FACE_TOP + FACE_PX_H * UNITS_PER_PX;
        quad(buffers.getBuffer(RenderType.text(FACE)), pose, MAP_MIN, FACE_TOP, MAP_MIN + MAP_SIZE, faceBottom,
                0, 0, 1, 1, 0, light);

        SessionDisplay display = ConsoleSessions.displayForHandheld(HandheldItem.cartridgeData(stack));
        if (display == null) {
            return;
        }
        display.update();
        // Fit the picture in the screen cutout: GBA fills it, Game Boy gets side bars.
        float screenW = SCREEN_PX_W * UNITS_PER_PX;
        float screenH = SCREEN_PX_H * UNITS_PER_PX;
        float centerX = MAP_MIN + (SCREEN_PX_X + SCREEN_PX_W / 2) * UNITS_PER_PX;
        float centerY = FACE_TOP + (SCREEN_PX_Y + SCREEN_PX_H / 2) * UNITS_PER_PX;
        float aspect = display.aspect();
        float w = Math.min(screenW, screenH * aspect);
        float h = w / aspect;
        quad(buffers.getBuffer(RenderType.text(display.textureId())), pose,
                centerX - w / 2, centerY - h / 2, centerX + w / 2, centerY + h / 2,
                0, 0, display.u1(), display.v1(),
                SCREEN_Z, LightTexture.FULL_BRIGHT);
    }

    /** An axis-aligned quad in map space (y down), wound like vanilla's map quad. */
    private static void quad(VertexConsumer consumer, Matrix4f pose, float x0, float y0, float x1, float y1,
            float u0, float v0, float u1, float v1, float z, int light) {
        consumer.addVertex(pose, x0, y1, z).setColor(-1).setUv(u0, v1).setLight(light);
        consumer.addVertex(pose, x1, y1, z).setColor(-1).setUv(u1, v1).setLight(light);
        consumer.addVertex(pose, x1, y0, z).setColor(-1).setUv(u1, v0).setLight(light);
        consumer.addVertex(pose, x0, y0, z).setColor(-1).setUv(u0, v0).setLight(light);
    }
}
