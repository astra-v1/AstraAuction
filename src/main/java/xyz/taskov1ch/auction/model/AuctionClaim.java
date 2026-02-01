package xyz.taskov1ch.auction.model;

public class AuctionClaim {
	private long id;
	private String playerUuid;
	private String itemNbt;
	private double money;
	private String reason;
	private long createdAt;
	private long expireAt;

	public AuctionClaim() {
	}

	public AuctionClaim(long id, String playerUuid, String itemNbt, double money, String reason, long createdAt,
			long expireAt) {
		this.id = id;
		this.playerUuid = playerUuid;
		this.itemNbt = itemNbt;
		this.money = money;
		this.reason = reason;
		this.createdAt = createdAt;
		this.expireAt = expireAt;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getPlayerUuid() {
		return playerUuid;
	}

	public void setPlayerUuid(String playerUuid) {
		this.playerUuid = playerUuid;
	}

	public String getItemNbt() {
		return itemNbt;
	}

	public void setItemNbt(String itemNbt) {
		this.itemNbt = itemNbt;
	}

	public double getMoney() {
		return money;
	}

	public void setMoney(double money) {
		this.money = money;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public long getExpireAt() {
		return expireAt;
	}

	public void setExpireAt(long expireAt) {
		this.expireAt = expireAt;
	}
}
