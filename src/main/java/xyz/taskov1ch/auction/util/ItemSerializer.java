package xyz.taskov1ch.auction.util;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;

import java.util.Base64;

public final class ItemSerializer {
	private ItemSerializer() {
	}

	public static String toBase64(Item item) {
		try {
			CompoundTag tag = NBTIO.putItemHelper(item);
			byte[] bytes = NBTIO.write(tag);
			return Base64.getEncoder().encodeToString(bytes);
		} catch (Exception e) {
			return "";
		}
	}

	public static Item fromBase64(String data, Item fallback) {
		if (data == null || data.isEmpty()) {
			return fallback;
		}
		try {
			if (data.startsWith("v2|")) {
				String payload = data.substring(3);
				String[] parts = payload.split("\\|", 4);
				if (parts.length < 3) {
					return fallback;
				}
				String namespaceId = parts[0];
				int damage = Integer.parseInt(parts[1]);
				int count = Integer.parseInt(parts[2]);
				String tagPart = parts.length >= 4 ? parts[3] : "";
				Item item = createItem(namespaceId, damage, count);
				if (item == null || item.isNull()) {
					return fallback;
				}
				if (tagPart != null && !tagPart.isEmpty()) {
					byte[] bytes = Base64.getDecoder().decode(tagPart);
					item.setCompoundTag(bytes);
				}
				return item;
			}
			if (!data.contains(":")) {
				byte[] bytes = Base64.getDecoder().decode(data);
				CompoundTag tag = NBTIO.read(bytes);
				Item item = NBTIO.getItemHelper(tag);
				return item == null || item.isNull() ? fallback : item;
			}
			String[] parts = data.split(":", 4);
			if (parts.length < 4) {
				return fallback;
			}
			int id = Integer.parseInt(parts[0]);
			int damage = Integer.parseInt(parts[1]);
			int count = Integer.parseInt(parts[2]);
			Item item = Item.get(id, damage, count);
			if (parts[3] != null && !parts[3].isEmpty()) {
				byte[] bytes = Base64.getDecoder().decode(parts[3]);
				CompoundTag tag = NBTIO.read(bytes);
				item.setNamedTag(tag);
			}
			return item;
		} catch (Exception e) {
			return fallback;
		}
	}

	@SuppressWarnings("deprecation")
	private static Item createItem(String namespaceId, int damage, int count) {
		try {
			int id = Integer.parseInt(namespaceId);
			return Item.get(id, damage, count);
		} catch (Exception ignored) {
		}
		Item item = Item.fromString(namespaceId);
		if (item == null || item.isNull()) {
			return item;
		}
		item.setDamage(damage);
		item.setCount(count);
		return item;
	}
}
