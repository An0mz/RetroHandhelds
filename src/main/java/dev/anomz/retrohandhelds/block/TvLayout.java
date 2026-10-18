package dev.anomz.retrohandhelds.block;

import java.util.ArrayDeque;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Where one TV sits in its screen, seen from the front: {@code col} counts from the viewer's left,
 * {@code row} from the bottom. A screen is a whole group of touching TVs facing the same way that
 * fills a rectangle of at most {@link #MAX_SIZE} a side. Every TV of the group works out the same
 * screen, so their slices of the picture line up. Any other group (a cross, an L, one with a gap,
 * or too big) is {@code invalid} and shows nothing: there is no single picture it could show.
 */
public record TvLayout(BlockPos origin, Direction right, int cols, int rows, int col, int row, boolean invalid) {
    /** Biggest screen in blocks per side; also bounds the work done per check. */
    public static final int MAX_SIZE = 16;
    private static final int MAX_CELLS = MAX_SIZE * MAX_SIZE;
    private static final Direction[] DIRECTIONS = Direction.values();

    /** The screen {@code pos} belongs to. */
    public static TvLayout of(BlockGetter level, BlockPos pos, Direction facing) {
        // The viewer stands in front of the TV, looking at it; their right is the TV's counter-clockwise side.
        Direction right = facing.getCounterClockWise();
        Direction[] plane = { right, right.getOpposite(), Direction.UP, Direction.DOWN };
        // Walk the whole group, in screen coordinates relative to pos.
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(pos.asLong());
        queue.add(pos);
        int minCol = 0;
        int maxCol = 0;
        int minRow = 0;
        int maxRow = 0;
        while (!queue.isEmpty()) {
            BlockPos cell = queue.poll();
            int c = (cell.getX() - pos.getX()) * right.getStepX() + (cell.getZ() - pos.getZ()) * right.getStepZ();
            int r = cell.getY() - pos.getY();
            minCol = Math.min(minCol, c);
            maxCol = Math.max(maxCol, c);
            minRow = Math.min(minRow, r);
            maxRow = Math.max(maxRow, r);
            for (Direction step : plane) {
                BlockPos next = cell.relative(step);
                if (!seen.contains(next.asLong()) && isTv(level.getBlockState(next), facing)) {
                    if (seen.size() >= MAX_CELLS) {
                        return new TvLayout(pos, right, 1, 1, 0, 0, true);
                    }
                    seen.add(next.asLong());
                    queue.add(next);
                }
            }
        }
        int cols = maxCol - minCol + 1;
        int rows = maxRow - minRow + 1;
        // Every cell of the bounding box was reached, so the group is a filled rectangle.
        boolean rectangle = seen.size() == cols * rows;
        BlockPos origin = pos.relative(right, minCol).above(minRow);
        return new TvLayout(origin, right, cols, rows, -minCol, -minRow,
                !rectangle || cols > MAX_SIZE || rows > MAX_SIZE);
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
                if (!layout.invalid()) {
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



    private static boolean isTv(BlockState state, Direction facing) {
        return state.getBlock() instanceof TvBlock && state.getValue(HorizontalDirectionalBlock.FACING) == facing;
    }
}
