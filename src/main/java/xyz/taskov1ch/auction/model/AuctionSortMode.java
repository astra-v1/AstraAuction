package xyz.taskov1ch.auction.model;

public enum AuctionSortMode {
	PRICE_ASC,
	PRICE_DESC;

	public static AuctionSortMode fromConfig(String value) {
		if (value == null) {
			return PRICE_ASC;
		}
		return value.equalsIgnoreCase("price_desc") ? PRICE_DESC : PRICE_ASC;
	}
}
