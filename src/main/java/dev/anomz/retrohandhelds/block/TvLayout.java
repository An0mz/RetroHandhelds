package dev.anomz.retrohandhelds.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Where one TV sits in its screen: a filled rectangle of TVs facing the same way, seen from the
 * front. {@code col} counts from the viewer's left, {@code row} from the bottom. A wall bigger than
 * {@link #MAX_SIZE} on a side is {@code tooBig} and shows nothing: each TV would only see part of it.
 */
public record TvLayout(BlockPos origin, Direction right, int cols, int rows, int col, int row, boolean tooBig) {
    /** Biggest screen in blocks per side; also bounds the work done per check. */
    public static final int MAX_SIZE = 16;
    private static final Direction[] DIRECTIONS = Direction.values();

    /** The screen {@code pos} belongs to; a single 1x1 screen if its neighbours don't form a filled rectangle. */
    public static TvLayout of(BlockGetter level, BlockPos pos, Direction facing) {
        // The viewer stands in front of the TV, looking at it; their right is the TV's counter-clockwise side.
        Direction right = facing.getCounterClockWise();
        int left = extent(level, pos, right.getOpposite(), facing);
        int toRight = extent(level, pos, right, facing);
        int down = extent(level, pos, Direction.DOWN, facing);
        int up = extent(level, pos, Direction.UP, facing);
        int cols = left + toRight + 1;
        int rows = down + up + 1;
        BlockPos origin = pos.relative(right, -left).below(down);
        if (cols > MAX_SIZE || rows > MAX_SIZE) {
            return new TvLayout(origin, right, Math.min(cols, MAX_SIZE), Math.min(rows, MAX_SIZE), left, down, true);
        }
        TvLayout layout = new TvLayout(origin, right, cols, rows, left, down, false);
        return layout.isFilled(level, facing) ? layout : new TvLayout(pos, right, 1, 1, 0, 0, false);
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
                TvLayout layout = of(level, pos, facing);
                if (!layout.tooBig()) {
                    return layout.frontCenter(facing);
                }
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
        Direction facing = right.getClockWise();
        int dx = target.getX() - origin.getX();
        int dz = target.getZ() - origin.getZ();
        int c = dx * right.getStepX() + dz * right.getStepZ();
        int r = target.getY() - origin.getY();
        int depth = dx * facing.getStepX() + dz * facing.getStepZ();
        boolean inCols = c >= 0 && c < cols;
        boolean inRows = r >= 0 && r < rows;
        if (depth == 1 || depth == -1) {
            // In front of or behind a TV.
            return inCols && inRows;
        }
        // Beside the screen's edge, in its plane.
        return depth == 0 && (inRows && (c == -1 || c == cols) || inCols && (r == -1 || r == rows));
    }

    /** A console block touching this screen, or null. */
    @Nullable
    public BlockPos findConsole(BlockGetter level) {
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                BlockPos cell = cell(c, r);
                for (Direction direction : DIRECTIONS) {
                    neighbor.setWithOffset(cell, direction);
                    if (level.getBlockState(neighbor).getBlock() instanceof ConsoleBlock) {
                        return neighbor.immutable();
                    }
                }
            }
        }
        return null;
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

    /** TVs in a row from {@code pos}; stops one past {@link #MAX_SIZE} so too-big walls can be told apart. */
    private static int extent(BlockGetter level, BlockPos pos, Direction step, Direction facing) {
        int n = 0;
        while (n < MAX_SIZE && isTv(level.getBlockState(pos.relative(step, n + 1)), facing)) {
            n++;
        }
        return n;
    }

    private static boolean isTv(BlockState state, Direction facing) {
        return state.getBlock() instanceof TvBlock && state.getValue(HorizontalDirectionalBlock.FACING) == facing;
    }
}
