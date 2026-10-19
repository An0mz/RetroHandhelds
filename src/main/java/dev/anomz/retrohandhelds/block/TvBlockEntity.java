package dev.anomz.retrohandhelds.block;

import dev.anomz.retrohandhelds.RetroRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exists so the TV gets a block entity renderer. Nothing is saved or synced; the client caches the
 * TV's screen layout here between checks.
 */
public class TvBlockEntity extends BlockEntity {
    private static final long LAYOUT_CHECK_INTERVAL = 10;

    private TvLayout layout;
    private long layoutCheckedAt = Long.MIN_VALUE;

    public TvBlockEntity(BlockPos pos, BlockState state) {
        super(RetroRegistries.TV_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * This TV's screen layout, re-checked twice a second so placing or breaking TVs updates it. One
     * check walks the whole screen and refreshes every TV in it, so a big wall costs one walk.
     */
    public TvLayout layout(long gameTime) {
        if (layout == null || gameTime - layoutCheckedAt >= LAYOUT_CHECK_INTERVAL || gameTime < layoutCheckedAt) {
            Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
            TvLayout mine = TvLayout.of(level, worldPosition, facing, (pos, cellLayout) -> {
                if (level.getBlockEntity(pos) instanceof TvBlockEntity other) {
                    other.layout = cellLayout;
                    other.layoutCheckedAt = gameTime;
                }
            });
            layout = mine;
            layoutCheckedAt = gameTime;
        }
        return layout;
    }
}
