package xyz.taskov1ch.auction.service;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.Task;
import java.util.List;
import java.util.UUID;
import xyz.taskov1ch.auction.config.ConfigManager;
import xyz.taskov1ch.auction.repository.AuctionRepository;
import xyz.taskov1ch.auction.repository.DatabaseProvider;
import xyz.taskov1ch.auction.economy.EconomyHook;
import xyz.taskov1ch.auction.model.AuctionClaim;
import xyz.taskov1ch.auction.model.AuctionItem;
import xyz.taskov1ch.auction.model.AuctionSortMode;
import xyz.taskov1ch.auction.model.AuctionStatus;
import xyz.taskov1ch.auction.util.InventoryUtil;
import xyz.taskov1ch.auction.util.ItemSerializer;

public class AuctionService {
	private final PluginBase plugin;
	private final ConfigManager configManager;
	private final AuctionRepository repository;
	private final EconomyHook economyHook;

	public AuctionService(PluginBase plugin, DatabaseProvider databaseProvider, ConfigManager configManager) {
		this.plugin = plugin;
		this.configManager = configManager;
		this.repository = new AuctionRepository(databaseProvider);
		this.economyHook = new EconomyHook();
		this.repository.createTables();
		startExpireTask();
	}

	public void shutdown() {
		plugin.getLogger().info("AuctionService shutdown.");
	}

	public AuctionRepository getRepository() {
		return repository;
	}

	public boolean isEconomyAvailable() {
		return economyHook.isAvailable();
	}

	public long createAuction(Player seller, Item item, double startPrice, Double buyoutPrice) {
		long now = System.currentTimeMillis();
		long endAt = now + (configManager.getAuctionDurationSeconds() * 1000L);
		String sellerUuid = seller.getUniqueId().toString();
		int maxSlots = configManager.getMaxSlots();
		if (maxSlots > 0 && repository.countActiveBySeller(sellerUuid) >= maxSlots) {
			return -1;
		}
		double normalizedStart = normalizePrice(startPrice);
		Double normalizedBuyout = buyoutPrice == null ? null : normalizePrice(buyoutPrice);
		AuctionItem auction = new AuctionItem();
		auction.setSellerUuid(sellerUuid);
		auction.setSellerName(seller.getName());
		auction.setItemNbt(ItemSerializer.toBase64(item));
		auction.setItemName(getItemDisplayName(item));
		auction.setStartPrice(normalizedStart);
		auction.setCurrentPrice(normalizedStart);
		auction.setBuyoutPrice(normalizedBuyout);
		auction.setStatus(AuctionStatus.ACTIVE.name());
		auction.setCreatedAt(now);
		auction.setEndAt(endAt);
		return repository.createAuction(auction);
	}

	public double normalizePrice(double price) {
		if (!configManager.isRoundPrices()) {
			return price;
		}
		return Math.floor(price);
	}

	public AuctionItem getAuction(long id) {
		return repository.getAuction(id);
	}

	public List<AuctionItem> listActive(int page, int pageSize, AuctionSortMode sortMode) {
		int offset = Math.max(0, page - 1) * pageSize;
		return repository.listActive(pageSize, offset, sortMode == AuctionSortMode.PRICE_ASC);
	}

	public List<AuctionItem> listActiveBySeller(String sellerUuid, int page, int pageSize) {
		int offset = Math.max(0, page - 1) * pageSize;
		return repository.listActiveBySellerUuid(sellerUuid, pageSize, offset);
	}

	public List<AuctionItem> listActiveBySellerName(String sellerName, int page, int pageSize,
			AuctionSortMode sortMode) {
		int offset = Math.max(0, page - 1) * pageSize;
		return repository.listActiveBySellerName(sellerName, pageSize, offset, sortMode == AuctionSortMode.PRICE_ASC);
	}

	public List<AuctionItem> searchActive(String keyword, int page, int pageSize, AuctionSortMode sortMode) {
		int offset = Math.max(0, page - 1) * pageSize;
		return repository.searchActive(keyword, pageSize, offset, sortMode == AuctionSortMode.PRICE_ASC);
	}

	public boolean buyNow(Player buyer, long id) {
		try {
			AuctionItem auction = repository.getAuction(id);
			if (auction == null || !AuctionStatus.ACTIVE.name().equals(auction.getStatus())) {
				return false;
			}
			if (auction.getSellerUuid().equals(buyer.getUniqueId().toString())) {
				return false;
			}
			double price = auction.getCurrentPrice();
			if (!economyHook.isAvailable() || !economyHook.has(buyer, price)) {
				return false;
			}
			if (!economyHook.withdraw(buyer, price)) {
				return false;
			}
			int updated = repository.tryBuyNow(id, price, price);
			if (updated == 0) {
				economyHook.deposit(buyer, price);
				return false;
			}
			finishAuction(auction, buyer, price, AuctionStatus.SOLD, false);
			return true;
		} catch (Exception e) {
			plugin.getLogger().warning("Buy now failed: " + e.getMessage());
			return false;
		}
	}

	public BuyResult buyNowWithResult(Player buyer, long id) {
		try {
			AuctionItem auction = repository.getAuction(id);
			if (auction == null) {
				return BuyResult.NOT_FOUND;
			}
			if (!AuctionStatus.ACTIVE.name().equals(auction.getStatus())) {
				return BuyResult.NOT_ACTIVE;
			}
			if (auction.getSellerUuid().equals(buyer.getUniqueId().toString())) {
				return BuyResult.OWN_LOT;
			}
			double price = auction.getCurrentPrice();
			if (!economyHook.isAvailable()) {
				return BuyResult.ECONOMY_MISSING;
			}
			if (!economyHook.has(buyer, price)) {
				return BuyResult.NOT_ENOUGH_MONEY;
			}
			if (!economyHook.withdraw(buyer, price)) {
				return BuyResult.WITHDRAW_FAILED;
			}
			int updated = repository.tryBuyNow(id, price, price);
			if (updated == 0) {
				economyHook.deposit(buyer, price);
				return BuyResult.CONFLICT;
			}
			finishAuction(auction, buyer, price, AuctionStatus.SOLD, false);
			return BuyResult.OK;
		} catch (Exception e) {
			plugin.getLogger().warning("Buy now failed: " + e.getMessage());
			return BuyResult.ERROR;
		}
	}

	public enum BuyResult {
		OK,
		NOT_FOUND,
		NOT_ACTIVE,
		OWN_LOT,
		ECONOMY_MISSING,
		NOT_ENOUGH_MONEY,
		WITHDRAW_FAILED,
		CONFLICT,
		ERROR
	}

	public boolean forceBuy(Player buyer, long id) {
		try {
			AuctionItem auction = repository.getAuction(id);
			if (auction == null || !AuctionStatus.ACTIVE.name().equals(auction.getStatus())) {
				return false;
			}
			double price = auction.getCurrentPrice();
			if (!economyHook.isAvailable() || !economyHook.has(buyer, price)) {
				return false;
			}
			if (!economyHook.withdraw(buyer, price)) {
				return false;
			}
			int updated = repository.tryBuyNow(id, price, price);
			if (updated == 0) {
				economyHook.deposit(buyer, price);
				return false;
			}
			finishAuction(auction, buyer, price, AuctionStatus.SOLD, false);
			return true;
		} catch (Exception e) {
			plugin.getLogger().warning("Force buy failed: " + e.getMessage());
			return false;
		}
	}

	public boolean forceExpire(long id) {
		try {
			AuctionItem auction = repository.getAuction(id);
			if (auction == null || !AuctionStatus.ACTIVE.name().equals(auction.getStatus())) {
				return false;
			}
			int updated = repository.updateStatusIfActive(auction.getId(), AuctionStatus.EXPIRED);
			if (updated > 0) {
				addClaim(auction.getSellerUuid(), auction.getItemNbt(), 0, "return");
				return true;
			}
			return false;
		} catch (Exception e) {
			plugin.getLogger().warning("Force expire failed: " + e.getMessage());
			return false;
		}
	}

	public boolean cancelAuction(Player player, long id) {
		try {
			AuctionItem auction = repository.getAuction(id);
			if (auction == null) {
				return false;
			}
			if (!auction.getSellerUuid().equals(player.getUniqueId().toString())) {
				return false;
			}
			int updated = repository.updateStatusIfActive(id, AuctionStatus.CANCELLED);
			if (updated == 0) {
				return false;
			}
			addClaim(auction.getSellerUuid(), auction.getItemNbt(), 0, "return");
			return true;
		} catch (Exception e) {
			plugin.getLogger().warning("Cancel auction failed: " + e.getMessage());
			return false;
		}
	}

	public int claim(Player player) {
		try {
			long now = System.currentTimeMillis();
			List<AuctionClaim> claims = repository.listClaims(player.getUniqueId().toString(), now);
			int count = 0;
			for (AuctionClaim claim : claims) {
				if (claim.getMoney() > 0) {
					economyHook.deposit(player, claim.getMoney());
				}
				if (claim.getItemNbt() != null && !claim.getItemNbt().isEmpty()) {
					Item item = ItemSerializer.fromBase64(claim.getItemNbt(), Item.get(0));
					InventoryUtil.giveItem(player, item);
				}
				repository.removeClaim(claim.getId());
				count++;
			}
			return count;
		} catch (Exception e) {
			plugin.getLogger().warning("Claim all failed: " + e.getMessage());
			return 0;
		}
	}

	public int countClaimsByUuid(String playerUuid) {
		return repository.countClaims(playerUuid, System.currentTimeMillis());
	}

	public List<AuctionClaim> listClaimsByUuid(String playerUuid, int page, int pageSize) {
		int offset = Math.max(0, page - 1) * pageSize;
		return repository.listClaimsPaged(playerUuid, System.currentTimeMillis(), pageSize, offset);
	}

	public boolean claimSingle(Player player, long claimId) {
		try {
			List<AuctionClaim> claims = repository.listClaims(player.getUniqueId().toString(),
					System.currentTimeMillis());
			for (AuctionClaim claim : claims) {
				if (claim.getId() == claimId) {
					if (claim.getMoney() > 0) {
						economyHook.deposit(player, claim.getMoney());
					}
					if (claim.getItemNbt() != null && !claim.getItemNbt().isEmpty()) {
						Item item = ItemSerializer.fromBase64(claim.getItemNbt(), Item.get(0));
						InventoryUtil.giveItem(player, item);
					}
					repository.removeClaim(claim.getId());
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			plugin.getLogger().warning("Claim single failed: " + e.getMessage());
			return false;
		}
	}

	private void startExpireTask() {
		plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, new Task() {
			@Override
			public void onRun(int currentTick) {
				plugin.getServer().getScheduler().scheduleAsyncTask(plugin, new cn.nukkit.scheduler.AsyncTask() {
					@Override
					public void onRun() {
						processExpired();
						processExpiredClaims();
					}
				});
			}
		}, 20 * 60);
	}

	private void processExpired() {
		long now = System.currentTimeMillis();
		List<AuctionItem> expired = repository.listExpired(now);
		for (AuctionItem auction : expired) {
			int updated = repository.updateStatusIfActive(auction.getId(), AuctionStatus.EXPIRED);
			if (updated > 0) {
				addClaim(auction.getSellerUuid(), auction.getItemNbt(), 0, "return");
			}
		}
	}

	private void processExpiredClaims() {
		repository.removeExpiredClaims(System.currentTimeMillis());
	}

	private void finishAuction(AuctionItem auction, Player bidder, double price, AuctionStatus status,
			boolean updateStatus) {
		if (updateStatus) {
			repository.updateStatus(auction.getId(), status);
		}
		Item item = ItemSerializer.fromBase64(auction.getItemNbt(), Item.get(0));
		InventoryUtil.giveItem(bidder, item);
		Player seller = getOnlinePlayer(auction.getSellerUuid());
		double taxPercent = configManager.getAuctionTaxPercent();
		double taxAmount = price * (taxPercent / 100.0);
		double payout = Math.max(0, price - taxAmount);
		if (seller != null) {
			economyHook.deposit(seller, payout);
		} else {
			boolean paid = economyHook.deposit(auction.getSellerUuid(), payout);
			if (!paid) {
				plugin.getLogger().warning("Failed to pay seller (offline) for auction id " + auction.getId());
			}
		}
	}

	private void addClaim(String playerUuid, String itemNbt, double money, String reason) {
		long now = System.currentTimeMillis();
		long expireAt = now + (configManager.getClaimExpireSeconds() * 1000L);
		repository.addClaim(new AuctionClaim(0, playerUuid, itemNbt, money, reason, now, expireAt));
	}

	private String getItemDisplayName(Item item) {
		try {
			if (item.hasCustomName()) {
				return item.getCustomName();
			}
		} catch (Exception ignored) {
		}
		return item.getName();
	}

	private Player getOnlinePlayer(String uuid) {
		try {
			return plugin.getServer().getPlayer(UUID.fromString(uuid)).orElse(null);
		} catch (Exception e) {
			return null;
		}
	}
}
