package xyz.taskov1ch.auction.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.item.Item;
import xyz.taskov1ch.auction.AstraAuction;
import xyz.taskov1ch.auction.service.AuctionService;
import xyz.taskov1ch.auction.util.Lang;
import xyz.taskov1ch.auction.util.MessageUtil;

public class AuctionCommand extends Command {
	private final AstraAuction plugin;
	private final AuctionService auctionService;

	public AuctionCommand(AstraAuction plugin, AuctionService auctionService) {
		super("ah", "Auction commands", "/ah help", new String[] { "auction", "auc" });
		this.plugin = plugin;
		this.auctionService = auctionService;
		this.setPermission("astraauction.use");
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		if (!testPermission(sender)) {
			return true;
		}

		if (args.length == 0) {
			if (sender instanceof Player player) {
				plugin.getAuctionGui().openMain(player, 1);
			} else {
				sendHelp(sender);
			}
			return true;
		}

		switch (args[0].toLowerCase()) {
			case "open", "gui" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return true;
				}
				int page = args.length >= 2 ? parseInt(args[1], 1) : 1;
				plugin.getAuctionGui().openMain(player, page);
			}
			case "sell" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage(MessageUtil.info(Lang.t("messages.usage.sell")));
					return true;
				}
				double price = parseDouble(args[1]);
				if (price <= 0) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.price-positive")));
					return true;
				}
				Item item = player.getInventory().getItemInHand();
				if (item == null || item.isNull()) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.item-in-hand")));
					return true;
				}
				long id = auctionService.createAuction(player, item, price, price);
				if (id <= 0) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.slots-limit")));
					return true;
				}
				player.getInventory().setItemInHand(Item.get(0));
				sender.sendMessage(MessageUtil.success(Lang.t("messages.lot-listed", "id", id)));
			}
			case "view" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage(MessageUtil.info(Lang.t("messages.usage.view")));
					return true;
				}
				int page = args.length >= 3 ? parseInt(args[2], 1) : 1;
				plugin.getAuctionGui().openViewPlayer(player, args[1], page);
			}
			case "search" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage(MessageUtil.info(Lang.t("messages.usage.search")));
					return true;
				}
				int page = args.length >= 3 ? parseInt(args[2], 1) : 1;
				plugin.getAuctionGui().openSearch(player, args[1], page);
			}
			case "force_buy" -> {
				if (!sender.hasPermission("astraauction.force")) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.no-permission")));
					return true;
				}
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage(MessageUtil.info(Lang.t("messages.usage.force_buy")));
					return true;
				}
				long id = parseLong(args[1], -1);
				if (id <= 0) {
					sender.sendMessage(MessageUtil.info(Lang.t("messages.usage.force_buy")));
					return true;
				}
				boolean ok = auctionService.forceBuy(player, id);
				if (ok) {
					sender.sendMessage(MessageUtil.success(Lang.t("messages.force.buy.ok")));
				} else {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.force.buy.fail")));
				}
			}
			case "force_expire" -> {
				if (!sender.hasPermission("astraauction.force")) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.no-permission")));
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage(MessageUtil.info(Lang.t("messages.usage.force_expire")));
					return true;
				}
				long id = parseLong(args[1], -1);
				if (id <= 0) {
					sender.sendMessage(MessageUtil.info(Lang.t("messages.usage.force_expire")));
					return true;
				}
				boolean ok = auctionService.forceExpire(id);
				if (ok) {
					sender.sendMessage(MessageUtil.success(Lang.t("messages.force.expire.ok")));
				} else {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.force.expire.fail")));
				}
			}
			case "help" -> sendHelp(sender);
			default -> sendHelp(sender);
		}

		return true;
	}

	private void sendHelp(CommandSender sender) {
		sender.sendMessage(MessageUtil.info(Lang.t("messages.help.open")));
		sender.sendMessage(MessageUtil.info(Lang.t("messages.help.sell")));
		sender.sendMessage(MessageUtil.info(Lang.t("messages.help.view")));
		sender.sendMessage(MessageUtil.info(Lang.t("messages.help.search")));
	}

	private double parseDouble(String value) {
		try {
			return Double.parseDouble(value);
		} catch (Exception e) {
			return -1;
		}
	}

	private int parseInt(String value, int def) {
		try {
			int v = Integer.parseInt(value);
			return v > 0 ? v : def;
		} catch (Exception e) {
			return def;
		}
	}

	private long parseLong(String value, long def) {
		try {
			long v = Long.parseLong(value);
			return v > 0 ? v : def;
		} catch (Exception e) {
			return def;
		}
	}
}
