package xyz.taskov1ch.auction.util;

import cn.nukkit.Player;
import cn.nukkit.item.Item;

public final class InventoryUtil {
	private InventoryUtil() {
	}

	public static void giveItem(Player player, Item item) {
		if (item == null || item.isNull()) {
			return;
		}
		if (player.getInventory().canAddItem(item)) {
			player.getInventory().addItem(item);
		} else {
			player.getLevel().dropItem(player.getPosition(), item);
		}
	}
}
