package dev.anomz.retrohandhelds.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import dev.anomz.retrohandhelds.block.TvBlockEntity;
import dev.anomz.retrohandhelds.client.session.ScreenImage;
import dev.anomz.retrohandhelds.client.share.RemoteScreens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the game on the console's screen: this client's own game, or one another player shares.
 * When a TV screen touching the console shows the game, the console only says so.
 */
public final class ConsoleRenderer implements BlockEntityRenderer<ConsoleBlockEntity> {
    // Screen area of the front texture, in block pixels: x 2..14, y 5..13 (3:2 like the GBA),
    // as half-sizes around its center, relative to the block center.
    private static final float AREA_HALF_WIDTH = 6 / 16f;
    private static final float AREA_HALF_HEIGHT = 4 / 16f;
    private static final float AREA_CENTER_Y = 1 / 16f;
    // Just in front of the block face, to avoid z-fighting.
    private static final float FRONT = -0.5f - 0.002f;
    private static final Component ON_TV = Component.translatable("screen.retrohandhelds.playing_on_tv");
    /** The text fills this much of the screen area's width. */
    private static final float TEXT_WIDTH = 2 * AREA_HALF_WIDTH * 0.8f;
    private static final int TEXT_COLOR = 0xFF8CE08C;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final Font font;
    private final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

    public ConsoleRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ConsoleBlockEntity console, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        ScreenImage display = RemoteScreens.imageFor(console.getBlockPos());
        BlockState state = console.getBlockState();
        if (display == null || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        // Turn so the front face is at -Z, matching the model's north-facing front.
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        if (onTv(console)) {
            drawOnTv(poseStack, buffers);
            poseStack.popPose();
            return;
        }
        display.update();
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.text(display.textureId()));
        // Fit the picture inside the area: GBA fills it, Game Boy (10:9) gets black bars at the sides.
        float aspect = display.aspect();
        float halfWidth = Math.min(AREA_HALF_WIDTH, AREA_HALF_HEIGHT * aspect);
        float halfHeight = halfWidth / aspect;
        float left = halfWidth;
        float right = -halfWidth;
        float top = AREA_CENTER_Y + halfHeight;
        float bottom = AREA_CENTER_Y - halfHeight;
        float u1 = display.u1();
        float v1 = display.v1();
        // Counter-clockwise as seen from the front (the viewer's left is +X here).
        // The screen glows, so it ignores world light.
        vertex(consumer, pose, left, top, 0, 0);
        vertex(consumer, pose, left, bottom, 0, v1);
        vertex(consumer, pose, right, bottom, u1, v1);
        vertex(consumer, pose, right, top, u1, 0);
        poseStack.popPose();
    }

    /** Whether a TV screen touching this console shows its game. */
    private boolean onTv(ConsoleBlockEntity console) {
        Level level = console.getLevel();
        if (level == null) {
            return false;
        }
        for (Direction direction : DIRECTIONS) {
            neighbor.setWithOffset(console.getBlockPos(), direction);
            if (level.getBlockEntity(neighbor) instanceof TvBlockEntity tv && !tv.layout(level.getGameTime()).invalid()) {
                return true;
            }
        }
        return false;
    }

    /** "Playing on TV", centered in the screen area. Font space is x right, y down; here the viewer's right is -X. */
    private void drawOnTv(PoseStack poseStack, MultiBufferSource buffers) {
        int width = font.width(ON_TV);
        float scale = TEXT_WIDTH / width;
        poseStack.translate(0, AREA_CENTER_Y, FRONT);
        poseStack.scale(-scale, -scale, scale);
        font.drawInBatch(ON_TV, -width / 2f, -font.lineHeight / 2f, TEXT_COLOR, false, poseStack.last().pose(),
                buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float u, float v) {
        consumer.addVertex(pose, x, y, FRONT).setColor(255, 255, 255, 255).setUv(u, v).setLight(LightTexture.FULL_BRIGHT);
    }
}
