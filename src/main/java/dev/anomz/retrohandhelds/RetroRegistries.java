package dev.anomz.retrohandhelds;

import dev.anomz.retrohandhelds.block.ConsoleBlock;
import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import dev.anomz.retrohandhelds.block.TvBlock;
import dev.anomz.retrohandhelds.block.TvBlockEntity;
import dev.anomz.retrohandhelds.item.CartridgeData;
import dev.anomz.retrohandhelds.item.CartridgeItem;
import dev.anomz.retrohandhelds.item.HandheldItem;
import dev.anomz.retrohandhelds.item.RemoteItem;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RetroRegistries {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RetroHandhelds.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RetroHandhelds.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RetroHandhelds.MODID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RetroHandhelds.MODID);

    public static final DeferredBlock<ConsoleBlock> CONSOLE = BLOCKS.register("console", () -> new ConsoleBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.5f).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> CONSOLE_ITEM = ITEMS.registerSimpleBlockItem("console", CONSOLE);
    public static final DeferredBlock<TvBlock> TV = BLOCKS.register("tv", () -> new TvBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> TV_ITEM = ITEMS.registerSimpleBlockItem("tv", TV);
    public static final DeferredItem<CartridgeItem> CARTRIDGE = ITEMS.register("cartridge",
            () -> new CartridgeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<HandheldItem> HANDHELD = ITEMS.register("handheld",
            () -> new HandheldItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<RemoteItem> REMOTE = ITEMS.register("remote",
            () -> new RemoteItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConsoleBlockEntity>> CONSOLE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("console",
                    () -> BlockEntityType.Builder.of(ConsoleBlockEntity::new, CONSOLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TvBlockEntity>> TV_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("tv",
                    () -> BlockEntityType.Builder.of(TvBlockEntity::new, TV.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CartridgeData>> CARTRIDGE_DATA =
            DATA_COMPONENTS.registerComponentType("cartridge",
                    builder -> builder.persistent(CartridgeData.CODEC).networkSynchronized(CartridgeData.STREAM_CODEC));
    /** The console a remote plays. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> REMOTE_LINK =
            DATA_COMPONENTS.registerComponentType("remote_link",
                    builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    private RetroRegistries() {
    }

    static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
    }
}
