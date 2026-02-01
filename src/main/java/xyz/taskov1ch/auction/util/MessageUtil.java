package xyz.taskov1ch.auction.util;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;

public final class MessageUtil {
	private MessageUtil() {
	}

	public static String prefix() {
		return Lang.colorize(Lang.t("prefix")) + TextFormat.RESET;
	}

	public static String info(String message) {
		return prefix() + TextFormat.GRAY + message;
	}

	public static String success(String message) {
		return prefix() + TextFormat.GREEN + message;
	}

	public static String error(String message) {
		return prefix() + TextFormat.RED + message;
	}

	public static void title(Player player, String title, String subtitle) {
		if (player == null) {
			return;
		}
		String safeTitle = title == null ? "" : Lang.colorize(title);
		String safeSubtitle = subtitle == null ? "" : Lang.colorize(subtitle);
		player.sendTitle(safeTitle, safeSubtitle, 5, 40, 5);
	}
}
