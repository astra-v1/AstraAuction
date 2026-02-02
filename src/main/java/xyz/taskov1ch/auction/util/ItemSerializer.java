package xyz.taskov1ch.auction.util;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.ProtocolInfo;

import java.io.IOException;
import java.util.Base64;

public final class ItemSerializer {
	private ItemSerializer() {
	}

	public static String toBase64(Item item) {
		try {
			CompoundTag tag = putItemHelper(item);
			byte[] bytes = NBTIO.write(tag);
			return Base64.getEncoder().encodeToString(bytes);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to serialize item", e);
		}
	}

	public static Item fromBase64(String data, Item fallback) {
		if (data == null || data.isEmpty()) {
			return fallback;
		}
		try {
			byte[] bytes = Base64.getDecoder().decode(data);
			CompoundTag tag = NBTIO.read(bytes);
			Item item = NBTIO.getItemHelper(tag);
			return item == null || item.isNull() ? fallback : item;
		} catch (Exception e) {
			return fallback;
		}
	}

	private static CompoundTag putItemHelper(Item item) {
		try {
			for (java.lang.reflect.Method method : NBTIO.class.getMethods()) {
				if (!"putItemHelper".equals(method.getName())) {
					continue;
				}
				Class<?>[] params = method.getParameterTypes();
				if (params.length == 2 && params[0] == Item.class && params[1] == boolean.class) {
					return (CompoundTag) method.invoke(null, item, true);
				}
				if (params.length == 4 && params[0] == Item.class && params[3] == boolean.class) {
					return (CompoundTag) method.invoke(null, item, null, ProtocolInfo.CURRENT_PROTOCOL, true);
				}
			}
		} catch (Exception ignored) {
		}
		return NBTIO.putItemHelper(item);
	}

}
