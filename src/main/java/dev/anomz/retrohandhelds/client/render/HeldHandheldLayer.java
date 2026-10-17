package dev.anomz.retrohandhelds.client.render;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.client.session.ScreenImage;
import dev.anomz.retrohandhelds.client.share.RemoteScreens;
import dev.anomz.retrohandhelds.item.HandheldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

/**
 * Draws the game on the screen of a handheld held by a player seen from outside (other players, or
 * this player in third person). The item itself is drawn by vanilla; this repeats its hand and item
 * transforms (ItemInHandLayer, ItemRenderer) and adds the picture over the icon's screen pixels.
 */
public final class HeldHandheldLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    // The screen in the item icon: columns 5..10, rows 6..9 of 16 (y up in item space).
    private static final float SCREEN_X0 = 5 / 16f;
    private static final float SCREEN_X1 = 11 / 16f;
    private static final float SCREEN_Y0 = 6 / 16f;
    private static final float SCREEN_Y1 = 10 / 16f;
    // The flat item spans z 7.5..8.5 px; the picture sits just outside both faces.
    private static final float FRONT_Z = 8.5f / 16 + 0.002f;
    private static final float BACK_Z = 7.5f / 16 - 0.002f;

    public HeldHandheldLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        HumanoidArm mainArm = player.getMainArm();
        // The shared picture belongs to one handheld; draw it on the main hand's if both hold one.
        if (!renderArm(poseStack, buffers, player, player.getMainHandItem(), mainArm)) {
            renderArm(poseStack, buffers, player, player.getOffhandItem(), mainArm.getOpposite());
        }
    }

    private boolean renderArm(PoseStack poseStack, MultiBufferSource buffers, AbstractClientPlayer player,
            ItemStack stack, HumanoidArm arm) {
        if (!stack.is(RetroRegistries.HANDHELD) || HandheldItem.cartridgeData(stack) == null) {
            return false;
        }
        ScreenImage image = imageFor(player, stack);
        if (image == null) {
            return false;
        }
        boolean left = arm == HumanoidArm.LEFT;
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(stack, player.level(), player, player.getId());
        poseStack.pushPose();
        // ItemInHandLayer.renderArmWithItem
        getParentModel().translateToHand(arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate((left ? -1 : 1) / 16f, 0.125f, -0.625f);
        // ItemRenderer.render
        ClientHooks.handleCameraTransforms(poseStack, model,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left);
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        drawScreen(poseStack, buffers, image);
        poseStack.popPose();
        return true;
    }

    @Nullable
    private static ScreenImage imageFor(AbstractClientPlayer player, ItemStack stack) {
        if (player == Minecraft.getInstance().player) {
            return ConsoleSessions.displayForHandheld(HandheldItem.cartridgeData(stack));
        }
        return RemoteScreens.forPlayer(player.getId());
    }

    /** The picture fitted into the screen pixels (Game Boy games get side bars), readable from both sides. */
    private static void drawScreen(PoseStack poseStack, MultiBufferSource buffers, ScreenImage image) {
        image.update();
        float aspect = image.aspect();
        float areaW = SCREEN_X1 - SCREEN_X0;
        float areaH = SCREEN_Y1 - SCREEN_Y0;
        float w = Math.min(areaW, areaH * aspect);
        float h = w / aspect;
        float x0 = (SCREEN_X0 + SCREEN_X1 - w) / 2;
        float x1 = x0 + w;
        float y0 = (SCREEN_Y0 + SCREEN_Y1 - h) / 2;
        float y1 = y0 + h;
        float u1 = image.u1();
        float v1 = image.v1();
        int light = LightTexture.FULL_BRIGHT;
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.text(image.textureId()));
        // Front, seen from +z: left edge is x0.
        consumer.addVertex(pose, x0, y0, FRONT_Z).setColor(-1).setUv(0, v1).setLight(light);
        consumer.addVertex(pose, x1, y0, FRONT_Z).setColor(-1).setUv(u1, v1).setLight(light);
        consumer.addVertex(pose, x1, y1, FRONT_Z).setColor(-1).setUv(u1, 0).setLight(light);
        consumer.addVertex(pose, x0, y1, FRONT_Z).setColor(-1).setUv(0, 0).setLight(light);
        // Back, seen from -z: left edge is x1.
        consumer.addVertex(pose, x1, y0, BACK_Z).setColor(-1).setUv(0, v1).setLight(light);
        consumer.addVertex(pose, x0, y0, BACK_Z).setColor(-1).setUv(u1, v1).setLight(light);
        consumer.addVertex(pose, x0, y1, BACK_Z).setColor(-1).setUv(u1, 0).setLight(light);
        consumer.addVertex(pose, x1, y1, BACK_Z).setColor(-1).setUv(0, 0).setLight(light);
    }
}
