package xyz.taskov1ch.auction.database;

import java.util.List;
import org.sql2o.Connection;
import xyz.taskov1ch.auction.model.AuctionClaim;
import xyz.taskov1ch.auction.model.AuctionItem;
import xyz.taskov1ch.auction.model.AuctionStatus;

public class AuctionRepository {
	private final DatabaseProvider databaseProvider;
	private final String dbType;

	public AuctionRepository(DatabaseProvider databaseProvider) {
		this.databaseProvider = databaseProvider;
		this.dbType = databaseProvider.getType();
	}

	public void createTables() {
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			con.createQuery(getAuctionItemsDdl()).executeUpdate();
			con.createQuery(getAuctionItemsIndexDdl()).executeUpdate();
			con.createQuery(getAuctionSellerIndexDdl()).executeUpdate();
			con.createQuery(getAuctionItemNameIndexDdl()).executeUpdate();
			con.createQuery(getClaimsDdl()).executeUpdate();
		}
	}

	private String getAuctionItemsDdl() {
		return switch (dbType) {
			case "mysql" -> "CREATE TABLE IF NOT EXISTS auction_items (" +
					"id BIGINT AUTO_INCREMENT PRIMARY KEY," +
					"seller_uuid VARCHAR(36) NOT NULL," +
					"seller_name VARCHAR(64) NOT NULL," +
					"item_nbt LONGTEXT NOT NULL," +
					"item_name VARCHAR(128) NOT NULL," +
					"start_price DOUBLE NOT NULL," +
					"current_price DOUBLE NOT NULL," +
					"buyout_price DOUBLE NULL," +
					"status VARCHAR(16) NOT NULL," +
					"created_at BIGINT NOT NULL," +
					"end_at BIGINT NOT NULL" +
					")";
			case "postgres", "postgresql" -> "CREATE TABLE IF NOT EXISTS auction_items (" +
					"id BIGSERIAL PRIMARY KEY," +
					"seller_uuid VARCHAR(36) NOT NULL," +
					"seller_name VARCHAR(64) NOT NULL," +
					"item_nbt TEXT NOT NULL," +
					"item_name VARCHAR(128) NOT NULL," +
					"start_price DOUBLE PRECISION NOT NULL," +
					"current_price DOUBLE PRECISION NOT NULL," +
					"buyout_price DOUBLE PRECISION NULL," +
					"status VARCHAR(16) NOT NULL," +
					"created_at BIGINT NOT NULL," +
					"end_at BIGINT NOT NULL" +
					")";
			default -> "CREATE TABLE IF NOT EXISTS auction_items (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"seller_uuid TEXT NOT NULL," +
					"seller_name TEXT NOT NULL," +
					"item_nbt TEXT NOT NULL," +
					"item_name TEXT NOT NULL," +
					"start_price REAL NOT NULL," +
					"current_price REAL NOT NULL," +
					"buyout_price REAL NULL," +
					"status TEXT NOT NULL," +
					"created_at INTEGER NOT NULL," +
					"end_at INTEGER NOT NULL" +
					")";
		};
	}

	private String getAuctionItemsIndexDdl() {
		return switch (dbType) {
			case "mysql" -> "CREATE INDEX IF NOT EXISTS idx_auction_status_end ON auction_items (status, end_at)";
			case "postgres", "postgresql" ->
				"CREATE INDEX IF NOT EXISTS idx_auction_status_end ON auction_items (status, end_at)";
			default -> "CREATE INDEX IF NOT EXISTS idx_auction_status_end ON auction_items (status, end_at)";
		};
	}

	private String getAuctionSellerIndexDdl() {
		return switch (dbType) {
			case "mysql" ->
				"CREATE INDEX IF NOT EXISTS idx_auction_seller_status ON auction_items (seller_uuid, status)";
			case "postgres", "postgresql" ->
				"CREATE INDEX IF NOT EXISTS idx_auction_seller_status ON auction_items (seller_uuid, status)";
			default -> "CREATE INDEX IF NOT EXISTS idx_auction_seller_status ON auction_items (seller_uuid, status)";
		};
	}

	private String getAuctionItemNameIndexDdl() {
		return switch (dbType) {
			case "mysql" -> "CREATE INDEX IF NOT EXISTS idx_auction_item_name ON auction_items (item_name)";
			case "postgres", "postgresql" ->
				"CREATE INDEX IF NOT EXISTS idx_auction_item_name ON auction_items (item_name)";
			default -> "CREATE INDEX IF NOT EXISTS idx_auction_item_name ON auction_items (item_name)";
		};
	}

	private String getClaimsDdl() {
		return switch (dbType) {
			case "mysql" -> "CREATE TABLE IF NOT EXISTS auction_claims (" +
					"id BIGINT AUTO_INCREMENT PRIMARY KEY," +
					"player_uuid VARCHAR(36) NOT NULL," +
					"item_nbt LONGTEXT NULL," +
					"money DOUBLE NOT NULL DEFAULT 0," +
					"reason VARCHAR(64) NULL," +
					"created_at BIGINT NOT NULL," +
					"expire_at BIGINT NOT NULL" +
					")";
			case "postgres", "postgresql" -> "CREATE TABLE IF NOT EXISTS auction_claims (" +
					"id BIGSERIAL PRIMARY KEY," +
					"player_uuid VARCHAR(36) NOT NULL," +
					"item_nbt TEXT NULL," +
					"money DOUBLE PRECISION NOT NULL DEFAULT 0," +
					"reason VARCHAR(64) NULL," +
					"created_at BIGINT NOT NULL," +
					"expire_at BIGINT NOT NULL" +
					")";
			default -> "CREATE TABLE IF NOT EXISTS auction_claims (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"player_uuid TEXT NOT NULL," +
					"item_nbt TEXT NULL," +
					"money REAL NOT NULL DEFAULT 0," +
					"reason TEXT NULL," +
					"created_at INTEGER NOT NULL," +
					"expire_at INTEGER NOT NULL" +
					")";
		};
	}

	public long createAuction(AuctionItem item) {
		String sql = "INSERT INTO auction_items (seller_uuid, seller_name, item_nbt, item_name, start_price, current_price, buyout_price, status, created_at, end_at) "
				+
				"VALUES (:seller_uuid, :seller_name, :item_nbt, :item_name, :start_price, :current_price, :buyout_price, :status, :created_at, :end_at)";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql, true)
					.addParameter("seller_uuid", item.getSellerUuid())
					.addParameter("seller_name", item.getSellerName())
					.addParameter("item_nbt", item.getItemNbt())
					.addParameter("item_name", item.getItemName())
					.addParameter("start_price", item.getStartPrice())
					.addParameter("current_price", item.getCurrentPrice())
					.addParameter("buyout_price", item.getBuyoutPrice())
					.addParameter("status", item.getStatus())
					.addParameter("created_at", item.getCreatedAt())
					.addParameter("end_at", item.getEndAt())
					.executeUpdate()
					.getKey(Long.class);
		}
	}

	public AuctionItem getAuction(long id) {
		String sql = "SELECT " + auctionItemColumns() + " FROM auction_items WHERE id = :id";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("id", id)
					.executeAndFetchFirst(AuctionItem.class);
		}
	}

	public List<AuctionItem> listActive(int limit, int offset, boolean sortAsc) {
		String order = sortAsc ? "ASC" : "DESC";
		String sql = "SELECT " + auctionItemColumns()
				+ " FROM auction_items WHERE status = :status ORDER BY current_price " + order
				+ ", end_at ASC LIMIT :limit OFFSET :offset";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("status", AuctionStatus.ACTIVE.name())
					.addParameter("limit", limit)
					.addParameter("offset", offset)
					.executeAndFetch(AuctionItem.class);
		}
	}

	public List<AuctionItem> listActiveBySellerUuid(String sellerUuid, int limit, int offset) {
		String sql = "SELECT " + auctionItemColumns()
				+ " FROM auction_items WHERE status = :status AND seller_uuid = :seller_uuid ORDER BY end_at ASC LIMIT :limit OFFSET :offset";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("status", AuctionStatus.ACTIVE.name())
					.addParameter("seller_uuid", sellerUuid)
					.addParameter("limit", limit)
					.addParameter("offset", offset)
					.executeAndFetch(AuctionItem.class);
		}
	}

	public List<AuctionItem> listActiveBySellerName(String sellerName, int limit, int offset, boolean sortAsc) {
		String order = sortAsc ? "ASC" : "DESC";
		String sql = "SELECT " + auctionItemColumns()
				+ " FROM auction_items WHERE status = :status AND LOWER(seller_name) = :seller_name "
				+ "ORDER BY current_price " + order + ", end_at ASC LIMIT :limit OFFSET :offset";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("status", AuctionStatus.ACTIVE.name())
					.addParameter("seller_name", sellerName.toLowerCase())
					.addParameter("limit", limit)
					.addParameter("offset", offset)
					.executeAndFetch(AuctionItem.class);
		}
	}

	public List<AuctionItem> searchActive(String keyword, int limit, int offset, boolean sortAsc) {
		String order = sortAsc ? "ASC" : "DESC";
		String sql = "SELECT " + auctionItemColumns()
				+ " FROM auction_items WHERE status = :status AND LOWER(item_name) LIKE :keyword "
				+ "ORDER BY current_price " + order + ", end_at ASC LIMIT :limit OFFSET :offset";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("status", AuctionStatus.ACTIVE.name())
					.addParameter("keyword", "%" + keyword.toLowerCase() + "%")
					.addParameter("limit", limit)
					.addParameter("offset", offset)
					.executeAndFetch(AuctionItem.class);
		}
	}

	public List<AuctionItem> listExpired(long now) {
		String sql = "SELECT " + auctionItemColumns()
				+ " FROM auction_items WHERE status = :status AND end_at <= :now";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("status", AuctionStatus.ACTIVE.name())
					.addParameter("now", now)
					.executeAndFetch(AuctionItem.class);
		}
	}

	public int tryBuyNow(long id, double currentPrice, double expectedPrice) {
		String sql = "UPDATE auction_items SET current_price = :current_price, "
				+ "status = :status WHERE id = :id AND status = :active AND current_price = :expected_price";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("current_price", currentPrice)
					.addParameter("status", AuctionStatus.SOLD.name())
					.addParameter("active", AuctionStatus.ACTIVE.name())
					.addParameter("expected_price", expectedPrice)
					.addParameter("id", id)
					.executeUpdate()
					.getResult();
		}
	}

	public void updateStatus(long id, AuctionStatus status) {
		String sql = "UPDATE auction_items SET status = :status WHERE id = :id";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			con.createQuery(sql)
					.addParameter("status", status.name())
					.addParameter("id", id)
					.executeUpdate();
		}
	}

	public int updateStatusIfActive(long id, AuctionStatus status) {
		String sql = "UPDATE auction_items SET status = :status WHERE id = :id AND status = :active";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("status", status.name())
					.addParameter("id", id)
					.addParameter("active", AuctionStatus.ACTIVE.name())
					.executeUpdate()
					.getResult();
		}
	}

	public void addClaim(AuctionClaim claim) {
		String sql = "INSERT INTO auction_claims (player_uuid, item_nbt, money, reason, created_at, expire_at) " +
				"VALUES (:player_uuid, :item_nbt, :money, :reason, :created_at, :expire_at)";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			con.createQuery(sql)
					.addParameter("player_uuid", claim.getPlayerUuid())
					.addParameter("item_nbt", claim.getItemNbt())
					.addParameter("money", claim.getMoney())
					.addParameter("reason", claim.getReason())
					.addParameter("created_at", claim.getCreatedAt())
					.addParameter("expire_at", claim.getExpireAt())
					.executeUpdate();
		}
	}

	public List<AuctionClaim> listClaims(String playerUuid, long now) {
		String sql = "SELECT " + claimColumns()
				+ " FROM auction_claims WHERE player_uuid = :player_uuid AND expire_at > :now ORDER BY created_at ASC";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("player_uuid", playerUuid)
					.addParameter("now", now)
					.executeAndFetch(AuctionClaim.class);
		}
	}

	public List<AuctionClaim> listClaimsPaged(String playerUuid, long now, int limit, int offset) {
		String sql = "SELECT " + claimColumns()
				+ " FROM auction_claims WHERE player_uuid = :player_uuid AND expire_at > :now ORDER BY created_at ASC LIMIT :limit OFFSET :offset";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("player_uuid", playerUuid)
					.addParameter("now", now)
					.addParameter("limit", limit)
					.addParameter("offset", offset)
					.executeAndFetch(AuctionClaim.class);
		}
	}

	public int countClaims(String playerUuid, long now) {
		String sql = "SELECT COUNT(1) FROM auction_claims WHERE player_uuid = :player_uuid AND expire_at > :now";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("player_uuid", playerUuid)
					.addParameter("now", now)
					.executeScalar(Integer.class);
		}
	}

	public void removeClaim(long id) {
		String sql = "DELETE FROM auction_claims WHERE id = :id";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			con.createQuery(sql)
					.addParameter("id", id)
					.executeUpdate();
		}
	}

	public int removeExpiredClaims(long now) {
		String sql = "DELETE FROM auction_claims WHERE expire_at <= :now";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("now", now)
					.executeUpdate()
					.getResult();
		}
	}

	public int countActiveBySeller(String sellerUuid) {
		String sql = "SELECT COUNT(1) FROM auction_items WHERE status = :status AND seller_uuid = :seller_uuid";
		try (Connection con = databaseProvider.getDatabase().openConnection()) {
			return con.createQuery(sql)
					.addParameter("status", AuctionStatus.ACTIVE.name())
					.addParameter("seller_uuid", sellerUuid)
					.executeScalar(Integer.class);
		}
	}

	private String auctionItemColumns() {
		return "id, seller_uuid AS sellerUuid, seller_name AS sellerName, item_nbt AS itemNbt, item_name AS itemName, "
				+ "start_price AS startPrice, current_price AS currentPrice, buyout_price AS buyoutPrice, "
				+ "status, created_at AS createdAt, end_at AS endAt";
	}

	private String claimColumns() {
		return "id, player_uuid AS playerUuid, item_nbt AS itemNbt, money, reason, created_at AS createdAt, "
				+ "expire_at AS expireAt";
	}
}
