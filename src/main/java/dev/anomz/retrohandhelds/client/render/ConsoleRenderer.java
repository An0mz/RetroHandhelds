package dev.anomz.retrohandhelds.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.client.session.SessionDisplay;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Draws the running game on the console's screen area while this client is playing it. */
public final class ConsoleRenderer implements BlockEntityRenderer<ConsoleBlockEntity> {
    // Screen area of the front texture, in block pixels: x 2..14, y 5..13 (3:2 like the GBA),
    // as half-sizes around its center, relative to the block center.
    private static final float AREA_HALF_WIDTH = 6 / 16f;
    private static final float AREA_HALF_HEIGHT = 4 / 16f;
    private static final float AREA_CENTER_Y = 1 / 16f;
    // Just in front of the block face, to avoid z-fighting.
    private static final float FRONT = -0.5f - 0.002f;

    public ConsoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ConsoleBlockEntity console, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        SessionDisplay display = ConsoleSessions.displayAt(console.getBlockPos());
        BlockState state = console.getBlockState();
        if (display == null || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }
        display.update();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        // Turn so the front face is at -Z, matching the model's north-facing front.
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.text(display.textureId()));
        // Fit the picture inside the area: GBA fills it, Game Boy (10:9) gets black bars at the sides.
        float aspect = display.width() / (float) display.height();
        float halfWidth = Math.min(AREA_HALF_WIDTH, AREA_HALF_HEIGHT * aspect);
        float halfHeight = halfWidth / aspect;
        float left = halfWidth;
        float right = -halfWidth;
        float top = AREA_CENTER_Y + halfHeight;
        float bottom = AREA_CENTER_Y - halfHeight;
        float u1 = display.width() / (float) FrameBuffer.WIDTH;
        float v1 = display.height() / (float) FrameBuffer.HEIGHT;
        // Counter-clockwise as seen from the front (the viewer's left is +X here).
        // The screen glows, so it ignores world light.
        vertex(consumer, pose, left, top, 0, 0);
        vertex(consumer, pose, left, bottom, 0, v1);
        vertex(consumer, pose, right, bottom, u1, v1);
        vertex(consumer, pose, right, top, u1, 0);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float u, float v) {
        consumer.addVertex(pose, x, y, FRONT).setColor(255, 255, 255, 255).setUv(u, v).setLight(LightTexture.FULL_BRIGHT);
    }
}
