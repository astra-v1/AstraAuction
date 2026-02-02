package xyz.taskov1ch.auction.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandEnum;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.tree.ParamList;
import cn.nukkit.command.utils.CommandLogger;
import cn.nukkit.item.Item;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.Server;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Map;
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
		this.commandParameters.clear();
		addCommandParameters("default", new CommandParameter[] {});
		addCommandParameters("open", new CommandParameter[] {
				CommandParameter.newEnum("subcommand", false, new CommandEnum("AuctionOpen", "open", "gui")),
				CommandParameter.newType("page", true, CommandParamType.INT)
		});
		addCommandParameters("sell", new CommandParameter[] {
				CommandParameter.newEnum("subcommand", false, new CommandEnum("AuctionSell", "sell")),
				CommandParameter.newType("price", false, CommandParamType.FLOAT)
		});
		addCommandParameters("view", new CommandParameter[] {
				CommandParameter.newEnum("subcommand", false, new CommandEnum("AuctionView", "view")),
				CommandParameter.newType("player", false, CommandParamType.STRING),
				CommandParameter.newType("page", true, CommandParamType.INT)
		});
		addCommandParameters("search", new CommandParameter[] {
				CommandParameter.newEnum("subcommand", false, new CommandEnum("AuctionSearch", "search")),
				CommandParameter.newType("query", false, CommandParamType.STRING),
				CommandParameter.newType("page", true, CommandParamType.INT)
		});
		addCommandParameters("force_buy", new CommandParameter[] {
				CommandParameter.newEnum("subcommand", false, new CommandEnum("AuctionForceBuy", "force_buy")),
				CommandParameter.newType("id", false, CommandParamType.INT)
		});
		addCommandParameters("force_expire", new CommandParameter[] {
				CommandParameter.newEnum("subcommand", false, new CommandEnum("AuctionForceExpire", "force_expire")),
				CommandParameter.newType("id", false, CommandParamType.INT)
		});
		addCommandParameters("help", new CommandParameter[] {
				CommandParameter.newEnum("subcommand", false, new CommandEnum("AuctionHelp", "help"))
		});
		this.enableParamTree();
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
				Item itemSnapshot = item.clone();
				runDbAction(player,
						() -> auctionService.createAuction(player, itemSnapshot, price, price),
						id -> {
							if (id <= 0) {
								sender.sendMessage(MessageUtil.error(Lang.t("messages.slots-limit")));
								return;
							}
							Item hand = player.getInventory().getItemInHand();
							if (hand != null
									&& hand.getId() == itemSnapshot.getId()
									&& hand.getDamage() == itemSnapshot.getDamage()
									&& hand.getCount() == itemSnapshot.getCount()) {
								player.getInventory().setItemInHand(Item.get(0));
							}
							sender.sendMessage(MessageUtil.success(Lang.t("messages.lot-listed", "id", id)));
						});
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
				runDbAction(player,
						() -> auctionService.forceBuy(player, id),
						ok -> {
							if (ok) {
								sender.sendMessage(MessageUtil.success(Lang.t("messages.force.buy.ok")));
							} else {
								sender.sendMessage(MessageUtil.error(Lang.t("messages.force.buy.fail")));
							}
						});
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
				runDbAction(sender,
						() -> auctionService.forceExpire(id),
						ok -> {
							if (ok) {
								sender.sendMessage(MessageUtil.success(Lang.t("messages.force.expire.ok")));
							} else {
								sender.sendMessage(MessageUtil.error(Lang.t("messages.force.expire.fail")));
							}
						});
			}
			case "help" -> sendHelp(sender);
			default -> sendHelp(sender);
		}

		return true;
	}

	@Override
	public int execute(CommandSender sender, String commandLabel, Map.Entry<String, ParamList> result,
			CommandLogger log) {
		if (!testPermission(sender)) {
			return 0;
		}
		ParamList list = result.getValue();
		switch (result.getKey()) {
			case "default" -> {
				if (sender instanceof Player player) {
					plugin.getAuctionGui().openMain(player, 1);
				} else {
					sendHelp(sender);
				}
				return 1;
			}
			case "open" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return 0;
				}
				int page = list.hasResult(1) ? normalizePage(list.getResult(1)) : 1;
				plugin.getAuctionGui().openMain(player, page);
				return 1;
			}
			case "sell" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return 0;
				}
				double price = readPrice(list.getResult(1));
				if (price <= 0) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.price-positive")));
					return 0;
				}
				Item item = player.getInventory().getItemInHand();
				if (item == null || item.isNull()) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.item-in-hand")));
					return 0;
				}
				Item itemSnapshot = item.clone();
				runDbAction(player,
						() -> auctionService.createAuction(player, itemSnapshot, price, price),
						id -> {
							if (id <= 0) {
								sender.sendMessage(MessageUtil.error(Lang.t("messages.slots-limit")));
								return;
							}
							Item hand = player.getInventory().getItemInHand();
							if (hand != null
									&& hand.getId() == itemSnapshot.getId()
									&& hand.getDamage() == itemSnapshot.getDamage()
									&& hand.getCount() == itemSnapshot.getCount()) {
								player.getInventory().setItemInHand(Item.get(0));
							}
							sender.sendMessage(MessageUtil.success(Lang.t("messages.lot-listed", "id", id)));
						});
				return 1;
			}
			case "view" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return 0;
				}
				String target = list.getResult(1);
				int page = list.hasResult(2) ? normalizePage(list.getResult(2)) : 1;
				plugin.getAuctionGui().openViewPlayer(player, target, page);
				return 1;
			}
			case "search" -> {
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return 0;
				}
				String query = list.getResult(1);
				int page = list.hasResult(2) ? normalizePage(list.getResult(2)) : 1;
				plugin.getAuctionGui().openSearch(player, query, page);
				return 1;
			}
			case "force_buy" -> {
				if (!sender.hasPermission("astraauction.force")) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.no-permission")));
					return 0;
				}
				if (!(sender instanceof Player player)) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.only-player")));
					return 0;
				}
				long id = normalizeId(list.getResult(1));
				if (id <= 0) {
					sender.sendMessage(MessageUtil.info(Lang.t("messages.usage.force_buy")));
					return 0;
				}
				runDbAction(player,
						() -> auctionService.forceBuy(player, id),
						ok -> {
							if (ok) {
								sender.sendMessage(MessageUtil.success(Lang.t("messages.force.buy.ok")));
							} else {
								sender.sendMessage(MessageUtil.error(Lang.t("messages.force.buy.fail")));
							}
						});
				return 1;
			}
			case "force_expire" -> {
				if (!sender.hasPermission("astraauction.force")) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.no-permission")));
					return 0;
				}
				long id = normalizeId(list.getResult(1));
				if (id <= 0) {
					sender.sendMessage(MessageUtil.info(Lang.t("messages.usage.force_expire")));
					return 0;
				}
				runDbAction(sender,
						() -> auctionService.forceExpire(id),
						ok -> {
							if (ok) {
								sender.sendMessage(MessageUtil.success(Lang.t("messages.force.expire.ok")));
							} else {
								sender.sendMessage(MessageUtil.error(Lang.t("messages.force.expire.fail")));
							}
						});
				return 1;
			}
			case "help" -> {
				sendHelp(sender);
				return 1;
			}
			default -> {
				sendHelp(sender);
				return 0;
			}
		}
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

	private double readPrice(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		return parseDouble(String.valueOf(value));
	}

	private int parseInt(String value, int def) {
		try {
			int v = Integer.parseInt(value);
			return v > 0 ? v : def;
		} catch (Exception e) {
			return def;
		}
	}

	private int normalizePage(Object value) {
		if (value instanceof Number number) {
			return Math.max(1, number.intValue());
		}
		return parseInt(String.valueOf(value), 1);
	}

	private long parseLong(String value, long def) {
		try {
			long v = Long.parseLong(value);
			return v > 0 ? v : def;
		} catch (Exception e) {
			return def;
		}
	}

	private long normalizeId(Object value) {
		if (value instanceof Number number) {
			long id = number.longValue();
			return id > 0 ? id : -1;
		}
		return parseLong(String.valueOf(value), -1);
	}

	private <T> void runDbAction(CommandSender sender, Supplier<T> query, Consumer<T> onSuccess) {
		AstraAuction plugin = this.plugin;
		if (plugin == null) {
			T result = query.get();
			onSuccess.accept(result);
			return;
		}
		plugin.getServer().getScheduler().scheduleAsyncTask(plugin, new AsyncTask() {
			@Override
			public void onRun() {
				try {
					setResult(query.get());
				} catch (Exception e) {
					setResult(e);
				}
			}

			@Override
			public void onCompletion(Server server) {
				if (sender instanceof Player player && !player.isOnline()) {
					return;
				}
				Object result = getResult();
				if (result instanceof Exception e) {
					sender.sendMessage(MessageUtil.error(Lang.t("messages.db.error")));
					plugin.getLogger().warning("DB query failed: " + e.getMessage());
					return;
				}
				@SuppressWarnings("unchecked")
				T data = (T) result;
				onSuccess.accept(data);
			}
		});
	}
}
