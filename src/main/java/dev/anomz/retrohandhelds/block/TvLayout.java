package dev.anomz.retrohandhelds.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Where one TV sits in its screen: a filled rectangle of TVs facing the same way, seen from the
 * front. {@code col} counts from the viewer's left, {@code row} from the bottom.
 */
public record TvLayout(BlockPos origin, Direction right, int cols, int rows, int col, int row) {
    /** Biggest screen in blocks per side; also bounds the work done per check. */
    public static final int MAX_SIZE = 8;
    private static final Direction[] DIRECTIONS = Direction.values();

    /** The screen {@code pos} belongs to; a single 1x1 screen if its neighbours don't form a filled rectangle. */
    public static TvLayout of(BlockGetter level, BlockPos pos, Direction facing) {
        // The viewer stands in front of the TV, looking at it; their right is the TV's counter-clockwise side.
        Direction right = facing.getCounterClockWise();
        int left = extent(level, pos, right.getOpposite(), facing);
        int toRight = extent(level, pos, right, facing);
        int down = extent(level, pos, Direction.DOWN, facing);
        int up = extent(level, pos, Direction.UP, facing);
        int cols = Math.min(left + toRight + 1, MAX_SIZE);
        int rows = Math.min(down + up + 1, MAX_SIZE);
        BlockPos origin = pos.relative(right, -left).below(down);
        TvLayout layout = new TvLayout(origin, right, cols, rows, left, down);
        return layout.isFilled(level, facing) ? layout : new TvLayout(pos, right, 1, 1, 0, 0);
    }

    /**
     * Where the sound of the console at {@code console} comes from: the middle of the front of a
     * TV screen touching it, or the console itself when no TV does.
     */
    public static Vec3 soundCenter(BlockGetter level, BlockPos console) {
        for (Direction direction : DIRECTIONS) {
            BlockPos pos = console.relative(direction);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TvBlock) {
                Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                return of(level, pos, facing).frontCenter(facing);
            }
        }
        return Vec3.atCenterOf(console);
    }

    /** The middle of this screen's front face. */
    public Vec3 frontCenter(Direction facing) {
        double across = (cols - 1) / 2.0;
        return Vec3.atCenterOf(origin).add(
                right.getStepX() * across + facing.getStepX() * 0.5,
                (rows - 1) / 2.0,
                right.getStepZ() * across + facing.getStepZ() * 0.5);
    }

    /** Whether {@code target} touches any TV of this screen (shares a face with it). */
    public boolean touches(BlockPos target) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (cell(c, r).distManhattan(target) == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private BlockPos cell(int c, int r) {
        return origin.relative(right, c).above(r);
    }

    private boolean isFilled(BlockGetter level, Direction facing) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!isTv(level.getBlockState(cell(c, r)), facing)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int extent(BlockGetter level, BlockPos pos, Direction step, Direction facing) {
        int n = 0;
        while (n < MAX_SIZE - 1 && isTv(level.getBlockState(pos.relative(step, n + 1)), facing)) {
            n++;
        }
        return n;
    }

    private static boolean isTv(BlockState state, Direction facing) {
        return state.getBlock() instanceof TvBlock && state.getValue(HorizontalDirectionalBlock.FACING) == facing;
    }
}
