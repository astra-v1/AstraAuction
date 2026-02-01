package xyz.taskov1ch.auction.gui;

import cn.nukkit.Player;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.item.Item;
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
	private static final int GUI_OPEN_DELAY_TICKS = 10;
	private static final String LANG_DB_STAY_STILL = "messages.db.stay_still";
	private static final String LANG_DB_FETCHING = "messages.db.fetching";
	private static final String LANG_DB_CANCEL_MOVED = "messages.db.cancel_moved";
	private static final String LANG_DB_ERROR = "messages.db.error";

	private final AuctionService auctionService;
	private final ConfigManager configManager;
	private final Map<UUID, AuctionSortMode> sortModeByPlayer = new HashMap<>();
	private final Map<UUID, Long> dbNoticeByPlayer = new HashMap<>();

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
				items -> openMarket(player, safePage, TextFormat.GOLD + Lang.t("gui.market.title"), items, pageSize,
						() -> openMain(player, safePage), sortMode, null, ViewType.MAIN));
	}

	public void openViewPlayer(Player player, String sellerName, int page) {
		if (!canOpen(player)) {
			return;
		}
		AuctionSortMode sortMode = getSortMode(player);
		int pageSize = getPageSize();
		int safePage = Math.max(1, page);
		String title = TextFormat.GOLD + Lang.t("gui.view.title", "player", sellerName);
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
		String title = TextFormat.GOLD + Lang.t("gui.search.title", "query", keyword);
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
		String title = TextFormat.GOLD + Lang.t("gui.my.title");
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
							TextFormat.GOLD + Lang.t("gui.claims.title") + TextFormat.RESET + " | "
									+ Lang.t("gui.page", "page", pageNumber));
					inventory.setDefaultItemHandler(cancelHandler());
					int slot = 0;
					for (AuctionClaim claim : claims) {
						Item view = buildClaimItem(claim);
						int claimSlot = slot;
						inventory.setItem(claimSlot, view, (item, event) -> {
							event.setCancelled(true);
							Player clicker = event.getTransaction().getSource();
							boolean ok = auctionService.claimSingle(clicker, claim.getId());
							if (ok) {
								clicker.sendMessage(MessageUtil.success(Lang.t("messages.claim.single.ok")));
							} else {
								clicker.sendMessage(MessageUtil.error(Lang.t("messages.claim.single.fail")));
							}
							player.removeWindow(inventory);
							openClaims(clicker, pageNumber);
						});
						slot++;
					}

					boolean hasNext = total > (pageNumber * pageSize);
					addBottomBar(inventory, pageNumber, hasNext, () -> openClaims(player, pageNumber - 1),
							() -> player.removeWindow(inventory), () -> openClaims(player, pageNumber + 1));
					inventory.setItem(48, navItem(54, Lang.t("gui.nav.claim_all")), (item, event) -> {
						event.setCancelled(true);
						Player clicker = event.getTransaction().getSource();
						int count = auctionService.claim(clicker);
						clicker.sendMessage(MessageUtil.success(Lang.t("messages.claim.received", "count", count)));
						player.removeWindow(inventory);
						openClaims(clicker, pageNumber);
					});
					inventory.setItem(47, navItem(54, Lang.t("gui.nav.my_items")), (item, event) -> {
						event.setCancelled(true);
						player.removeWindow(inventory);
						openMyItems(player, 1);
					});
					inventory.setItem(51, navItem(54, Lang.t("gui.nav.market")), (item, event) -> {
						event.setCancelled(true);
						player.removeWindow(inventory);
						openMain(player, 1);
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
			MessageUtil.title(player, Lang.t(LANG_DB_STAY_STILL), Lang.t(LANG_DB_FETCHING));
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
					player.sendMessage(MessageUtil.error(Lang.t(LANG_DB_CANCEL_MOVED)));
					return;
				}
				Object result = getResult();
				if (result instanceof Exception e) {
					player.sendMessage(MessageUtil.error(Lang.t(LANG_DB_ERROR)));
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
			player.sendMessage(MessageUtil.error(Lang.t("messages.gui.unavailable")));
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
				title + TextFormat.RESET + " | " + Lang.t("gui.page", "page", page));
		inventory.setDefaultItemHandler(cancelHandler());

		for (int i = 0; i < items.size() && i < pageSize; i++) {
			AuctionItem auction = items.get(i);
			Item view = ItemSerializer.fromBase64(auction.getItemNbt(), Item.get(0));
			String itemName = view.getName();
			view.setCustomName(TextFormat.YELLOW + itemName);
			view.setLore(new String[] {
					TextFormat.GRAY + Lang.t("gui.lore.seller", "seller", safeValue(auction.getSellerName())),
					TextFormat.GRAY + Lang.t("gui.lore.price", "price", auction.getCurrentPrice()),
					TextFormat.GRAY + Lang.t("gui.lore.remaining", "time",
							formatDuration(auction.getEndAt() - System.currentTimeMillis())),
					TextFormat.DARK_GRAY + Lang.t("gui.lore.id", "id", auction.getId()),
					TextFormat.DARK_GRAY + Lang.t("gui.lore.shift_hint")
			});

			inventory.setItem(i, view, (item, event) -> {
				event.setCancelled(true);
				Player clicker = event.getTransaction().getSource();
				if (viewType == ViewType.MY_ITEMS) {
					boolean ok = auctionService.cancelAuction(clicker, auction.getId());
					if (ok) {
						clicker.sendMessage(MessageUtil.success(Lang.t("messages.lot.cancel.ok")));
					} else {
						clicker.sendMessage(MessageUtil.error(Lang.t("messages.lot.cancel.fail")));
					}
					reopen.run();
					return;
				}
				if (!auctionService.isEconomyAvailable()) {
					clicker.sendMessage(MessageUtil.error(Lang.t("messages.economy.missing")));
					return;
				}
				openConfirmBuy(clicker, auction, reopen);
			});
		}

		addBottomBar(inventory, page, items.size() >= pageSize,
				() -> {
					player.removeWindow(inventory);
					openMarketPage(player, page - 1, viewType, filter);
				},
				() -> player.removeWindow(inventory),
				() -> {
					player.removeWindow(inventory);
					openMarketPage(player, page + 1, viewType, filter);
				});

		inventory.setItem(46, navItem(399,
				sortMode == AuctionSortMode.PRICE_ASC ? Lang.t("gui.sort.price_asc_selected")
						: Lang.t("gui.sort.price_asc")),
				(item, event) -> {
					event.setCancelled(true);
					setSortMode(player, AuctionSortMode.PRICE_ASC);
					player.removeWindow(inventory);
					reopen.run();
				});
		inventory.setItem(52, navItem(399,
				sortMode == AuctionSortMode.PRICE_DESC ? Lang.t("gui.sort.price_desc_selected")
						: Lang.t("gui.sort.price_desc")),
				(item, event) -> {
					event.setCancelled(true);
					setSortMode(player, AuctionSortMode.PRICE_DESC);
					player.removeWindow(inventory);
					reopen.run();
				});
		inventory.setItem(47, navItem(54, Lang.t("gui.nav.my_items")), (item, event) -> {
			event.setCancelled(true);
			player.removeWindow(inventory);
			openMyItems(player, 1);
		});
		inventory.setItem(51, navItem(394, Lang.t("gui.nav.claims")), (item, event) -> {
			event.setCancelled(true);
			player.removeWindow(inventory);
			openClaims(player, 1);
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

	private void addBottomBar(FakeInventory inventory, int page, boolean hasNext, Runnable prev, Runnable close,
			Runnable next) {
		inventory.setItem(45, navItem(262, Lang.t("gui.nav.back")), (item, event) -> {
			event.setCancelled(true);
			if (page > 1) {
				prev.run();
			}
		});
		inventory.setItem(49, navItem(331, Lang.t("gui.nav.close")), (item, event) -> {
			event.setCancelled(true);
			close.run();
		});
		inventory.setItem(53, navItem(262, Lang.t("gui.nav.next")), (item, event) -> {
			event.setCancelled(true);
			if (hasNext) {
				next.run();
			}
		});
	}

	private void openConfirmBuy(Player player, AuctionItem auction, Runnable reopen) {
		FakeInventory inventory = new FakeInventory(InventoryType.CHEST,
				TextFormat.GOLD + Lang.t("gui.confirm.title"));
		inventory.setDefaultItemHandler(cancelHandler());

		Item display = ItemSerializer.fromBase64(auction.getItemNbt(), Item.get(0));
		display.setCustomName(TextFormat.YELLOW + display.getName());
		display.setLore(new String[] {
				TextFormat.GRAY + Lang.t("gui.lore.price", "price", auction.getCurrentPrice()),
				TextFormat.GRAY + Lang.t("gui.lore.seller", "seller", safeValue(auction.getSellerName()))
		});

		Item yes = Item.get(35, 5, 1);
		yes.setCustomName(TextFormat.GREEN + Lang.t("gui.confirm.buy", "price", auction.getCurrentPrice()));
		Item no = Item.get(35, 14, 1);
		no.setCustomName(TextFormat.RED + Lang.t("gui.confirm.cancel"));

		inventory.setItem(4, display, cancelHandler());
		inventory.setItem(2, yes, (item, event) -> {
			event.setCancelled(true);
			boolean ok = auctionService.buyNow(player, auction.getId());
			if (ok) {
				player.sendMessage(MessageUtil.success(Lang.t("messages.buy.ok")));
			} else {
				player.sendMessage(MessageUtil.error(Lang.t("messages.buy.fail")));
			}
			player.removeWindow(inventory);
			reopen.run();
		});
		inventory.setItem(6, no, (item, event) -> {
			event.setCancelled(true);
			player.removeWindow(inventory);
			reopen.run();
		});

		openWindow(player, inventory);
	}

	private void openWindow(Player player, FakeInventory inventory) {
		AstraAuction plugin = AstraAuction.getInstance();
		if (plugin == null) {
			player.addWindow(inventory);
			return;
		}
		plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
			if (player == null || !player.isOnline()) {
				return;
			}
			player.addWindow(inventory);
		}, GUI_OPEN_DELAY_TICKS);
	}

	private Item buildClaimItem(AuctionClaim claim) {
		if (claim.getItemNbt() != null && !claim.getItemNbt().isEmpty()) {
			Item item = ItemSerializer.fromBase64(claim.getItemNbt(), Item.get(0));
			item.setCustomName(TextFormat.YELLOW + item.getName());
			item.setLore(new String[] { TextFormat.GRAY + Lang.t("gui.claim.take") });
			return item;
		}
		Item money = Item.get(371, 0, 1);
		money.setCustomName(TextFormat.GOLD + Lang.t("gui.claim.money"));
		money.setLore(new String[] { TextFormat.GRAY + Lang.t("gui.claim.amount", "amount", claim.getMoney()),
				TextFormat.GRAY + Lang.t("gui.claim.take") });
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

	private String formatDuration(long millis) {
		long seconds = Math.max(0, millis / 1000L);
		long days = seconds / 86400;
		seconds %= 86400;
		long hours = seconds / 3600;
		seconds %= 3600;
		long minutes = seconds / 60;
		if (days > 0) {
			return Lang.t("time.days_hours", "days", days, "hours", hours);
		}
		if (hours > 0) {
			return Lang.t("time.hours_minutes", "hours", hours, "minutes", minutes);
		}
		return Lang.t("time.minutes", "minutes", minutes);
	}

	private String safeValue(String value) {
		return value == null || value.isEmpty() ? Lang.t("time.empty") : value;
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
}
