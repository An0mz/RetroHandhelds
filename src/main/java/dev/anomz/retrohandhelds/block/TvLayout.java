package dev.anomz.retrohandhelds.block;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

    /** Receives the layout of another TV of the same screen; {@code pos} is only valid during the call. */
    @FunctionalInterface
    public interface CellLayouts {
        void accept(BlockPos pos, TvLayout layout);
    }

    /** The screen {@code pos} belongs to. */
    public static TvLayout of(BlockGetter level, BlockPos pos, Direction facing) {
        return of(level, pos, facing, null);
    }

    /**
     * The screen {@code pos} belongs to. The walk visits every TV of the screen anyway, so their
     * layouts are handed to {@code others} too: one walk then serves a whole wall of TVs.
     */
    public static TvLayout of(BlockGetter level, BlockPos pos, Direction facing, @Nullable CellLayouts others) {
        // The viewer stands in front of the TV, looking at it; their right is the TV's counter-clockwise side.
        Direction right = facing.getCounterClockWise();
        Direction[] plane = { right, right.getOpposite(), Direction.UP, Direction.DOWN };
        // Walk the whole group, in screen coordinates relative to pos. Positions stay packed longs,
        // so the walk allocates nothing per cell.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        LongOpenHashSet seen = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        long start = pos.asLong();
        seen.add(start);
        queue.enqueue(start);
        int minCol = 0;
        int maxCol = 0;
        int minRow = 0;
        int maxRow = 0;
        boolean tooBig = false;
        while (!queue.isEmpty() && !tooBig) {
            long cell = queue.dequeueLong();
            int c = column(cell, pos, right);
            int r = BlockPos.getY(cell) - pos.getY();
            minCol = Math.min(minCol, c);
            maxCol = Math.max(maxCol, c);
            minRow = Math.min(minRow, r);
            maxRow = Math.max(maxRow, r);
            for (Direction step : plane) {
                long next = BlockPos.offset(cell, step);
                if (!seen.contains(next) && isTv(level.getBlockState(cursor.set(next)), facing)) {
                    if (seen.size() >= MAX_CELLS) {
                        tooBig = true;
                        break;
                    }
                    seen.add(next);
                    queue.enqueue(next);
                }
            }
        }
        if (tooBig) {
            if (others != null) {
                seen.forEach((long cell) -> {
                    cursor.set(cell);
                    others.accept(cursor, new TvLayout(cursor.immutable(), right, 1, 1, 0, 0, true));
                });
            }
            return new TvLayout(pos, right, 1, 1, 0, 0, true);
        }
        int cols = maxCol - minCol + 1;
        int rows = maxRow - minRow + 1;
        // Every cell of the bounding box was reached, so the group is a filled rectangle.
        boolean invalid = seen.size() != cols * rows || cols > MAX_SIZE || rows > MAX_SIZE;
        BlockPos origin = pos.relative(right, minCol).above(minRow);
        if (others != null) {
            int firstCol = minCol;
            int firstRow = minRow;
            seen.forEach((long cell) -> {
                if (cell != start) {
                    others.accept(cursor.set(cell), new TvLayout(origin, right, cols, rows,
                            column(cell, pos, right) - firstCol, BlockPos.getY(cell) - pos.getY() - firstRow, invalid));
                }
            });
        }
        return new TvLayout(origin, right, cols, rows, -minCol, -minRow, invalid);
    }

    /** Screen column of {@code cell}, counted from {@code pos}. */
    private static int column(long cell, BlockPos pos, Direction right) {
        return (BlockPos.getX(cell) - pos.getX()) * right.getStepX() + (BlockPos.getZ(cell) - pos.getZ()) * right.getStepZ();
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
                // Asked every tick for every playing console: use the TV's cached layout when there is one.
                TvLayout layout = level instanceof Level world && world.getBlockEntity(pos) instanceof TvBlockEntity tv
                        ? tv.layout(world.getGameTime())
                        : of(level, pos, facing);
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
