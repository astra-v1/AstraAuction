package xyz.taskov1ch.auction.gui;

import cn.nukkit.Player;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import cn.nukkit.level.Position;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.Server;
import cn.nukkit.utils.TextFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.iwareq.fakeinventories.FakeInventory;
import me.iwareq.fakeinventories.util.ItemHandler;
import xyz.taskov1ch.auction.AstraAuction;
import xyz.taskov1ch.auction.config.ConfigManager;
import xyz.taskov1ch.auction.model.AuctionClaim;
import xyz.taskov1ch.auction.model.AuctionItem;
import xyz.taskov1ch.auction.model.AuctionSortMode;
import xyz.taskov1ch.auction.service.AuctionService;
import xyz.taskov1ch.auction.util.ItemSerializer;
import xyz.taskov1ch.auction.util.Lang;
import xyz.taskov1ch.auction.util.MessageUtil;

public class AuctionGui {
	private static final String LANG_DB_STAY_STILL = "messages.db.stay_still";
	private static final String LANG_DB_FETCHING = "messages.db.fetching";
	private static final String LANG_DB_CANCEL_MOVED = "messages.db.cancel_moved";
	private static final String LANG_DB_ERROR = "messages.db.error";

	private final AuctionService auctionService;
	private final ConfigManager configManager;
	private final Map<UUID, AuctionSortMode> sortModeByPlayer = new HashMap<>();
	private final Map<UUID, Long> dbNoticeByPlayer = new HashMap<>();
	private final Map<UUID, ClickContext> clickContextByPlayer = new HashMap<>();
	private final Map<UUID, LastAction> lastActionByPlayer = new HashMap<>();

	public AuctionGui(AuctionService auctionService, ConfigManager configManager) {
		this.auctionService = auctionService;
		this.configManager = configManager;
	}

	public void openMain(Player player, int page) {
		if (!canOpen(player)) {
			return;
		}
		AuctionSortMode sortMode = getSortMode(player);
		int pageSize = getPageSize();
		int safePage = Math.max(1, page);
		runDbQuery(player,
				() -> auctionService.listActive(safePage, pageSize, sortMode),
				items -> openMarket(player, safePage, TextFormat.GOLD + t(player, "gui.market.title"), items, pageSize,
						() -> openMain(player, safePage), sortMode, null, ViewType.MAIN));
	}

	public void openViewPlayer(Player player, String sellerName, int page) {
		if (!canOpen(player)) {
			return;
		}
		AuctionSortMode sortMode = getSortMode(player);
		int pageSize = getPageSize();
		int safePage = Math.max(1, page);
		String title = TextFormat.GOLD + t(player, "gui.view.title", "player", sellerName);
		runDbQuery(player,
				() -> auctionService.listActiveBySellerName(sellerName, safePage, pageSize, sortMode),
				items -> openMarket(player, safePage, title, items, pageSize,
						() -> openViewPlayer(player, sellerName, safePage), sortMode, sellerName,
						ViewType.VIEW_PLAYER));
	}

	public void openSearch(Player player, String keyword, int page) {
		if (!canOpen(player)) {
			return;
		}
		AuctionSortMode sortMode = getSortMode(player);
		int pageSize = getPageSize();
		int safePage = Math.max(1, page);
		String title = TextFormat.GOLD + t(player, "gui.search.title", "query", keyword);
		runDbQuery(player,
				() -> auctionService.searchActive(keyword, safePage, pageSize, sortMode),
				items -> openMarket(player, safePage, title, items, pageSize,
						() -> openSearch(player, keyword, safePage), sortMode, keyword, ViewType.SEARCH));
	}

	public void openMyItems(Player player, int page) {
		if (!canOpen(player)) {
			return;
		}
		int pageSize = getPageSize();
		int safePage = Math.max(1, page);
		String title = TextFormat.GOLD + t(player, "gui.my.title");
		String sellerUuid = player.getUniqueId().toString();
		runDbQuery(player,
				() -> auctionService.listActiveBySeller(sellerUuid, safePage, pageSize),
				items -> openMarket(player, safePage, title, items, pageSize,
						() -> openMyItems(player, safePage), getSortMode(player), null, ViewType.MY_ITEMS));
	}

	public void openClaims(Player player, int page) {
		if (!canOpen(player)) {
			return;
		}
		int pageSize = getPageSize();
		int safePage = Math.max(1, page);
		String playerUuid = player.getUniqueId().toString();
		runDbQuery(player,
				() -> {
					int total = auctionService.countClaimsByUuid(playerUuid);
					List<AuctionClaim> claims = total == 0
							? Collections.emptyList()
							: auctionService.listClaimsByUuid(playerUuid, safePage, pageSize);
					return new ClaimsPage(total, safePage, claims);
				},
				pageResult -> {
					int total = pageResult.total;
					int pageNumber = pageResult.page;
					List<AuctionClaim> claims = pageResult.claims;
					if (total > 0 && claims.isEmpty() && pageNumber > 1) {
						openClaims(player, pageNumber - 1);
						return;
					}
					FakeInventory inventory = new FakeInventory(InventoryType.DOUBLE_CHEST,
							TextFormat.GOLD + t(player, "gui.claims.title") + TextFormat.RESET + " | "
									+ t(player, "gui.page", "page", pageNumber));
					inventory.setDefaultItemHandler(cancelHandler());
					int slot = 0;
					for (AuctionClaim claim : claims) {
						Item view = buildClaimItem(player, claim);
						int claimSlot = slot;
						bindItem(inventory, player, claimSlot, view, clicker -> {
							runDbAction(clicker,
									() -> auctionService.claimSingle(clicker, claim.getId()),
									ok -> {
										if (ok) {
											clicker.sendMessage(
													MessageUtil.success(t(clicker, "messages.claim.single.ok")));
										} else {
											clicker.sendMessage(
													MessageUtil.error(t(clicker, "messages.claim.single.fail")));
										}
										reopenAfterClose(clicker, inventory, () -> openClaims(clicker, pageNumber));
									});
						});
						slot++;
					}

					boolean hasNext = total > (pageNumber * pageSize);
					addBottomBar(inventory, player, pageNumber, hasNext,
							() -> openClaims(player, pageNumber - 1),
							() -> player.removeWindow(inventory),
							() -> openClaims(player, pageNumber + 1));
					bindItem(inventory, player, 48, navItem(54, t(player, "gui.nav.claim_all")), clicker -> {
						runDbAction(clicker,
								() -> auctionService.claim(clicker),
								count -> {
									clicker.sendMessage(MessageUtil.success(
											t(clicker, "messages.claim.received", "count", count)));
									reopenAfterClose(clicker, inventory, () -> openClaims(clicker, pageNumber));
								});
					});
					bindItem(inventory, player, 47, navItem(54, t(player, "gui.nav.my_items")), clicker -> {
						reopenAfterClose(clicker, inventory, () -> openMyItems(clicker, 1));
					});
					bindItem(inventory, player, 51, navItem(54, t(player, "gui.nav.market")), clicker -> {
						reopenAfterClose(clicker, inventory, () -> openMain(clicker, 1));
					});

					openWindow(player, inventory);
				});
	}

	private <T> void runDbQuery(Player player, Supplier<T> query, Consumer<T> onSuccess) {
		AstraAuction plugin = AstraAuction.getInstance();
		if (plugin == null) {
			T result = query.get();
			onSuccess.accept(result);
			return;
		}
		Position start = player.getPosition().floor();
		long now = System.currentTimeMillis();
		Long lastNotice = dbNoticeByPlayer.get(player.getUniqueId());
		if (lastNotice == null || now - lastNotice > 3000) {
			MessageUtil.title(player, t(player, LANG_DB_STAY_STILL), t(player, LANG_DB_FETCHING));
			dbNoticeByPlayer.put(player.getUniqueId(), now);
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
				if (player == null || !player.isOnline()) {
					return;
				}
				if (hasMoved(player, start)) {
					player.sendMessage(MessageUtil.error(t(player, LANG_DB_CANCEL_MOVED)));
					return;
				}
				Object result = getResult();
				if (result instanceof Exception e) {
					player.sendMessage(MessageUtil.error(t(player, LANG_DB_ERROR)));
					plugin.getLogger().warning("DB query failed: " + e.getMessage());
					return;
				}
				@SuppressWarnings("unchecked")
				T data = (T) result;
				onSuccess.accept(data);
			}
		});
	}

	private <T> void runDbAction(Player player, Supplier<T> query, Consumer<T> onSuccess) {
		AstraAuction plugin = AstraAuction.getInstance();
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
				if (player == null || !player.isOnline()) {
					return;
				}
				Object result = getResult();
				if (result instanceof Exception e) {
					player.sendMessage(MessageUtil.error(t(player, LANG_DB_ERROR)));
					plugin.getLogger().warning("DB query failed: " + e.getMessage());
					return;
				}
				@SuppressWarnings("unchecked")
				T data = (T) result;
				onSuccess.accept(data);
			}
		});
	}

	private boolean hasMoved(Player player, Position start) {
		if (start == null || player == null || !player.isOnline()) {
			return true;
		}
		Position now = player.getPosition().floor();
		if (now.getLevel() != start.getLevel()) {
			return true;
		}
		return now.getFloorX() != start.getFloorX()
				|| now.getFloorY() != start.getFloorY()
				|| now.getFloorZ() != start.getFloorZ();
	}

	private ItemHandler cancelHandler() {
		return (item, event) -> event.setCancelled(true);
	}

	private boolean canOpen(Player player) {
		AstraAuction plugin = AstraAuction.getInstance();
		if (plugin == null) {
			return true;
		}
		if (plugin.getServer().getPluginManager().getPlugin("FakeInventories") == null
				|| !plugin.getServer().getPluginManager().getPlugin("FakeInventories").isEnabled()) {
			player.sendMessage(MessageUtil.error(t(player, "messages.gui.unavailable")));
			return false;
		}
		return true;
	}

	private Item navItem(int id, String name) {
		Item item = Item.get(id);
		item.setCustomName(TextFormat.AQUA + name);
		return item;
	}

	private void openMarket(Player player, int page, String title, List<AuctionItem> items, int pageSize,
			Runnable reopen, AuctionSortMode sortMode, String filter, ViewType viewType) {
		FakeInventory inventory = new FakeInventory(InventoryType.DOUBLE_CHEST,
				title + TextFormat.RESET + " | " + t(player, "gui.page", "page", page));
		inventory.setDefaultItemHandler(cancelHandler());

		for (int i = 0; i < items.size() && i < pageSize; i++) {
			AuctionItem auction = items.get(i);
			Item view = ItemSerializer.fromBase64(auction.getItemNbt(), Item.get(0));
			String itemName = view.getName();
			view.setCustomName(TextFormat.YELLOW + itemName);
			view.setLore(new String[] {
					TextFormat.GRAY
							+ t(player, "gui.lore.seller", "seller", safeValue(player, auction.getSellerName())),
					TextFormat.GRAY + t(player, "gui.lore.price", "price", auction.getCurrentPrice()),
					TextFormat.GRAY + t(player, "gui.lore.remaining", "time",
							formatDuration(player, auction.getEndAt() - System.currentTimeMillis())),
					TextFormat.DARK_GRAY + t(player, "gui.lore.id", "id", auction.getId()),
					TextFormat.DARK_GRAY + t(player, "gui.lore.shift_hint")
			});

			bindItem(inventory, player, i, view, clicker -> {
				if (viewType == ViewType.MY_ITEMS) {
					runDbAction(clicker,
							() -> auctionService.cancelAuction(clicker, auction.getId()),
							ok -> {
								if (ok) {
									clicker.sendMessage(MessageUtil.success(t(clicker, "messages.lot.cancel.ok")));
								} else {
									clicker.sendMessage(MessageUtil.error(t(clicker, "messages.lot.cancel.fail")));
								}
								reopenAfterClose(clicker, inventory, reopen);
							});
					return;
				}
				if (!auctionService.isEconomyAvailable()) {
					clicker.sendMessage(MessageUtil.error(t(clicker, "messages.economy.missing")));
					return;
				}
				reopenAfterClose(clicker, inventory, () -> openConfirmBuy(clicker, auction, reopen));
			});
		}

		addBottomBar(inventory, player, page, items.size() >= pageSize,
				() -> {
					reopenAfterClose(player, inventory, () -> openMarketPage(player, page - 1, viewType, filter));
				},
				() -> player.removeWindow(inventory),
				() -> {
					reopenAfterClose(player, inventory, () -> openMarketPage(player, page + 1, viewType, filter));
				});

		bindItem(inventory, player, 46, navItem(399,
				sortMode == AuctionSortMode.PRICE_ASC ? t(player, "gui.sort.price_asc_selected")
						: t(player, "gui.sort.price_asc")),
				clicker -> {
					setSortMode(clicker, AuctionSortMode.PRICE_ASC);
					reopenAfterClose(clicker, inventory, reopen);
				});
		bindItem(inventory, player, 52, navItem(399,
				sortMode == AuctionSortMode.PRICE_DESC ? t(player, "gui.sort.price_desc_selected")
						: t(player, "gui.sort.price_desc")),
				clicker -> {
					setSortMode(clicker, AuctionSortMode.PRICE_DESC);
					reopenAfterClose(clicker, inventory, reopen);
				});
		bindItem(inventory, player, 47, navItem(54, t(player, "gui.nav.my_items")), clicker -> {
			reopenAfterClose(clicker, inventory, () -> openMyItems(clicker, 1));
		});
		bindItem(inventory, player, 51, navItem(394, t(player, "gui.nav.claims")), clicker -> {
			reopenAfterClose(clicker, inventory, () -> openClaims(clicker, 1));
		});

		openWindow(player, inventory);
	}

	private void openMarketPage(Player player, int page, ViewType viewType, String filter) {
		if (page < 1) {
			return;
		}
		switch (viewType) {
			case MAIN -> openMain(player, page);
			case VIEW_PLAYER -> openViewPlayer(player, filter, page);
			case SEARCH -> openSearch(player, filter, page);
			case MY_ITEMS -> openMyItems(player, page);
			default -> openMain(player, page);
		}
	}

	private void addBottomBar(FakeInventory inventory, Player player, int page, boolean hasNext, Runnable prev,
			Runnable close, Runnable next) {
		bindItem(inventory, player, 45, navItem(262, t(player, "gui.nav.back")), clicker -> {
			if (page > 1) {
				prev.run();
			}
		});
		bindItem(inventory, player, 49, navItem(331, t(player, "gui.nav.close")), clicker -> close.run());
		bindItem(inventory, player, 53, navItem(262, t(player, "gui.nav.next")), clicker -> {
			if (hasNext) {
				next.run();
			}
		});
	}

	private void openConfirmBuy(Player player, AuctionItem auction, Runnable reopen) {
		FakeInventory inventory = new FakeInventory(InventoryType.CHEST,
				TextFormat.GOLD + t(player, "gui.confirm.title"));
		inventory.setDefaultItemHandler(cancelHandler());

		Item display = ItemSerializer.fromBase64(auction.getItemNbt(), Item.get(0));
		display.setCustomName(TextFormat.YELLOW + display.getName());
		display.setLore(new String[] {
				TextFormat.GRAY + t(player, "gui.lore.price", "price", auction.getCurrentPrice()),
				TextFormat.GRAY + t(player, "gui.lore.seller", "seller", safeValue(player, auction.getSellerName()))
		});

		Item yes = Item.get(35, 5, 1);
		yes.setCustomName(TextFormat.GREEN + t(player, "gui.confirm.buy", "price", auction.getCurrentPrice()));
		Item no = Item.get(35, 14, 1);
		no.setCustomName(TextFormat.RED + t(player, "gui.confirm.cancel"));

		inventory.setItem(4, display, cancelHandler());
		bindItem(inventory, player, 2, yes, clicker -> {
			runDbAction(clicker,
					() -> auctionService.buyNowWithResult(clicker, auction.getId()),
					result -> {
						playBuyFeedback(clicker, result == AuctionService.BuyResult.OK);
						switch (result) {
							case OK -> clicker.sendMessage(MessageUtil.success(t(clicker, "messages.buy.ok")));
							case NOT_ENOUGH_MONEY ->
								clicker.sendMessage(MessageUtil.error(t(clicker, "messages.buy.not_enough")));
							case OWN_LOT -> clicker.sendMessage(MessageUtil.error(t(clicker, "messages.buy.own")));
							case NOT_ACTIVE, NOT_FOUND, CONFLICT ->
								clicker.sendMessage(MessageUtil.error(t(clicker, "messages.buy.not_available")));
							case ECONOMY_MISSING ->
								clicker.sendMessage(MessageUtil.error(t(clicker, "messages.economy.missing")));
							default -> clicker.sendMessage(MessageUtil.error(t(clicker, "messages.buy.fail")));
						}
						reopenAfterClose(clicker, inventory, reopen);
					});
		});
		bindItem(inventory, player, 6, no, clicker -> {
			reopenAfterClose(clicker, inventory, reopen);
		});

		openWindow(player, inventory);
	}

	private void playBuyFeedback(Player player, boolean success) {
		if (player == null || player.getLevel() == null) {
			return;
		}
		Sound sound = success ? Sound.RANDOM_LEVELUP : Sound.RANDOM_ANVIL_LAND;
		try {
			player.getLevel().addSound(player, sound, 1f, 1f, player);
		} catch (Exception ignored) {
		}
	}

	private void openWindow(Player player, FakeInventory inventory) {
		AstraAuction plugin = AstraAuction.getInstance();
		if (plugin == null) {
			trackClickContext(player, inventory);
			player.addWindow(inventory);
			return;
		}
		plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
			if (player == null || !player.isOnline()) {
				return;
			}
			trackClickContext(player, inventory);
			player.addWindow(inventory);
		}, configManager.getGuiOpenDelayTicks());
	}

	private void trackClickContext(Player player, FakeInventory inventory) {
		ClickContext context = clickContextByPlayer.get(player.getUniqueId());
		if (context == null || context.inventory != inventory) {
			clickContextByPlayer.put(player.getUniqueId(), new ClickContext(inventory));
		}
	}

	private void reopenAfterClose(Player player, FakeInventory inventory, Runnable action) {
		AstraAuction plugin = AstraAuction.getInstance();
		player.removeWindow(inventory);
		if (plugin == null) {
			action.run();
			return;
		}
		plugin.getServer().getScheduler().scheduleDelayedTask(plugin, action, 1);
	}

	private void bindItem(FakeInventory inventory, Player player, int slot, Item item,
			java.util.function.Consumer<Player> action) {
		inventory.setItem(slot, item, (clickedItem, event) -> {
			event.setCancelled(true);
			Player clicker = event.getTransaction().getSource();
			handleSlotAction(clicker, inventory, slot);
		});
		ClickContext context = clickContextByPlayer.get(player.getUniqueId());
		if (context == null || context.inventory != inventory) {
			context = new ClickContext(inventory);
			clickContextByPlayer.put(player.getUniqueId(), context);
		}
		context.handlers.put(slot, action);
	}

	public void handleClick(Player player, FakeInventory inventory, int slot) {
		handleSlotAction(player, inventory, slot);
	}

	private void handleSlotAction(Player player, FakeInventory inventory, int slot) {
		ClickContext context = clickContextByPlayer.get(player.getUniqueId());
		if (context == null || context.inventory != inventory) {
			return;
		}
		if (shouldSkipAction(player.getUniqueId(), slot)) {
			return;
		}
		java.util.function.Consumer<Player> action = context.handlers.get(slot);
		if (action != null) {
			action.accept(player);
		}
	}

	private boolean shouldSkipAction(UUID playerId, int slot) {
		long now = System.currentTimeMillis();
		LastAction lastAction = lastActionByPlayer.get(playerId);
		if (lastAction != null && lastAction.slot == slot && now - lastAction.at < 200) {
			return true;
		}
		lastActionByPlayer.put(playerId, new LastAction(slot, now));
		return false;
	}

	public void handleClose(Player player, FakeInventory inventory) {
		ClickContext context = clickContextByPlayer.get(player.getUniqueId());
		if (context != null && context.inventory == inventory) {
			clickContextByPlayer.remove(player.getUniqueId());
		}
		lastActionByPlayer.remove(player.getUniqueId());
	}

	public boolean isTracked(Player player, FakeInventory inventory) {
		ClickContext context = clickContextByPlayer.get(player.getUniqueId());
		return context != null && context.inventory == inventory;
	}

	private Item buildClaimItem(Player player, AuctionClaim claim) {
		if (claim.getItemNbt() != null && !claim.getItemNbt().isEmpty()) {
			Item item = ItemSerializer.fromBase64(claim.getItemNbt(), Item.get(0));
			item.setCustomName(TextFormat.YELLOW + item.getName());
			item.setLore(new String[] { TextFormat.GRAY + t(player, "gui.claim.take") });
			return item;
		}
		Item money = Item.get(371, 0, 1);
		money.setCustomName(TextFormat.GOLD + t(player, "gui.claim.money"));
		money.setLore(new String[] { TextFormat.GRAY + t(player, "gui.claim.amount", "amount", claim.getMoney()),
				TextFormat.GRAY + t(player, "gui.claim.take") });
		return money;
	}

	private AuctionSortMode getSortMode(Player player) {
		return sortModeByPlayer.computeIfAbsent(player.getUniqueId(),
				uuid -> AuctionSortMode.fromConfig(configManager.getGuiSortDefault()));
	}

	private void setSortMode(Player player, AuctionSortMode mode) {
		sortModeByPlayer.put(player.getUniqueId(), mode);
	}

	private int getPageSize() {
		int size = configManager.getGuiPageSize();
		return Math.min(45, Math.max(9, size));
	}

	private String formatDuration(Player player, long millis) {
		long seconds = Math.max(0, millis / 1000L);
		long days = seconds / 86400;
		seconds %= 86400;
		long hours = seconds / 3600;
		seconds %= 3600;
		long minutes = seconds / 60;
		if (days > 0) {
			return t(player, "time.days_hours", "days", days, "hours", hours);
		}
		if (hours > 0) {
			return t(player, "time.hours_minutes", "hours", hours, "minutes", minutes);
		}
		return t(player, "time.minutes", "minutes", minutes);
	}

	private String safeValue(Player player, String value) {
		return value == null || value.isEmpty() ? t(player, "time.empty") : value;
	}

	private String t(Player player, String key, Object... placeholders) {
		return Lang.t(player, key, placeholders);
	}

	private enum ViewType {
		MAIN,
		VIEW_PLAYER,
		SEARCH,
		MY_ITEMS
	}

	private static class ClaimsPage {
		private final int total;
		private final int page;
		private final List<AuctionClaim> claims;

		private ClaimsPage(int total, int page, List<AuctionClaim> claims) {
			this.total = total;
			this.page = page;
			this.claims = claims;
		}
	}

	private static class ClickContext {
		private final FakeInventory inventory;
		private final Map<Integer, java.util.function.Consumer<Player>> handlers = new HashMap<>();

		private ClickContext(FakeInventory inventory) {
			this.inventory = inventory;
		}
	}

	private static class LastAction {
		private final int slot;
		private final long at;

		private LastAction(int slot, long at) {
			this.slot = slot;
			this.at = at;
		}
	}
}
