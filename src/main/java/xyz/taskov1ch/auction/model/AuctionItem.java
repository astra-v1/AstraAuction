package xyz.taskov1ch.auction.model;

public class AuctionItem {
	private long id;
	private String sellerUuid;
	private String sellerName;
	private String itemNbt;
	private String itemName;
	private double startPrice;
	private double currentPrice;
	private Double buyoutPrice;
	private long endAt;
	private long createdAt;
	private String status;

	public AuctionItem() {
	}

	public AuctionItem(long id, String sellerUuid, String sellerName, String itemNbt, String itemName,
			double startPrice, double currentPrice, Double buyoutPrice, long endAt, long createdAt,
			String status) {
		this.id = id;
		this.sellerUuid = sellerUuid;
		this.sellerName = sellerName;
		this.itemNbt = itemNbt;
		this.itemName = itemName;
		this.startPrice = startPrice;
		this.currentPrice = currentPrice;
		this.buyoutPrice = buyoutPrice;
		this.endAt = endAt;
		this.createdAt = createdAt;
		this.status = status;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getSellerUuid() {
		return sellerUuid;
	}

	public void setSellerUuid(String sellerUuid) {
		this.sellerUuid = sellerUuid;
	}

	public String getSellerName() {
		return sellerName;
	}

	public void setSellerName(String sellerName) {
		this.sellerName = sellerName;
	}

	public String getItemNbt() {
		return itemNbt;
	}

	public void setItemNbt(String itemNbt) {
		this.itemNbt = itemNbt;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public double getStartPrice() {
		return startPrice;
	}

	public void setStartPrice(double startPrice) {
		this.startPrice = startPrice;
	}

	public double getCurrentPrice() {
		return currentPrice;
	}

	public void setCurrentPrice(double currentPrice) {
		this.currentPrice = currentPrice;
	}

	public Double getBuyoutPrice() {
		return buyoutPrice;
	}

	public void setBuyoutPrice(Double buyoutPrice) {
		this.buyoutPrice = buyoutPrice;
	}

	public long getEndAt() {
		return endAt;
	}

	public void setEndAt(long endAt) {
		this.endAt = endAt;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
