package dev.anomz.retrohandhelds.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component on a cartridge: which ROM file it plays and the ID its saves are stored under.
 * The ROM itself never leaves the player's machine; only its file name is stored.
 */
public record CartridgeData(String romFile, String saveId) {
    public static final Codec<CartridgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("rom").forGetter(CartridgeData::romFile),
            Codec.STRING.fieldOf("save_id").forGetter(CartridgeData::saveId)
    ).apply(instance, CartridgeData::new));

    public static final StreamCodec<ByteBuf, CartridgeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CartridgeData::romFile,
            ByteBufCodecs.STRING_UTF8, CartridgeData::saveId,
            CartridgeData::new);
}
