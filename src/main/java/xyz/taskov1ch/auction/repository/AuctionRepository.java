package xyz.taskov1ch.auction.repository;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.SQLDataType;
import org.jooq.Table;
import org.jooq.impl.DSL;
import xyz.taskov1ch.auction.model.AuctionClaim;
import xyz.taskov1ch.auction.model.AuctionItem;
import xyz.taskov1ch.auction.model.AuctionStatus;

public class AuctionRepository {
	private static final Table<?> AUCTION_ITEMS = DSL.table(DSL.name("auction_items"));
	private static final Table<?> AUCTION_CLAIMS = DSL.table(DSL.name("auction_claims"));

	private static final Field<Long> ID = DSL.field(DSL.name("id"), Long.class);
	private static final Field<String> SELLER_UUID = DSL.field(DSL.name("seller_uuid"), String.class);
	private static final Field<String> SELLER_NAME = DSL.field(DSL.name("seller_name"), String.class);
	private static final Field<String> ITEM_NBT = DSL.field(DSL.name("item_nbt"), String.class);
	private static final Field<String> ITEM_NAME = DSL.field(DSL.name("item_name"), String.class);
	private static final Field<Double> START_PRICE = DSL.field(DSL.name("start_price"), Double.class);
	private static final Field<Double> CURRENT_PRICE = DSL.field(DSL.name("current_price"), Double.class);
	private static final Field<Double> BUYOUT_PRICE = DSL.field(DSL.name("buyout_price"), Double.class);
	private static final Field<String> STATUS = DSL.field(DSL.name("status"), String.class);
	private static final Field<Long> CREATED_AT = DSL.field(DSL.name("created_at"), Long.class);
	private static final Field<Long> END_AT = DSL.field(DSL.name("end_at"), Long.class);

	private static final Field<Long> CLAIM_ID = DSL.field(DSL.name("id"), Long.class);
	private static final Field<String> CLAIM_PLAYER_UUID = DSL.field(DSL.name("player_uuid"), String.class);
	private static final Field<String> CLAIM_ITEM_NBT = DSL.field(DSL.name("item_nbt"), String.class);
	private static final Field<Double> CLAIM_MONEY = DSL.field(DSL.name("money"), Double.class);
	private static final Field<String> CLAIM_REASON = DSL.field(DSL.name("reason"), String.class);
	private static final Field<Long> CLAIM_CREATED_AT = DSL.field(DSL.name("created_at"), Long.class);
	private static final Field<Long> CLAIM_EXPIRE_AT = DSL.field(DSL.name("expire_at"), Long.class);

	private final DatabaseProvider databaseProvider;

	public AuctionRepository(DatabaseProvider databaseProvider) {
		this.databaseProvider = databaseProvider;
	}

	public void createTables() {
		withContext(dsl -> {
			dsl.createTableIfNotExists(AUCTION_ITEMS)
					.column(ID, SQLDataType.BIGINT.identity(true))
					.column(SELLER_UUID, SQLDataType.VARCHAR(36).nullable(false))
					.column(SELLER_NAME, SQLDataType.VARCHAR(64).nullable(false))
					.column(ITEM_NBT, SQLDataType.CLOB.nullable(false))
					.column(ITEM_NAME, SQLDataType.VARCHAR(128).nullable(false))
					.column(START_PRICE, SQLDataType.DOUBLE.nullable(false))
					.column(CURRENT_PRICE, SQLDataType.DOUBLE.nullable(false))
					.column(BUYOUT_PRICE, SQLDataType.DOUBLE.nullable(true))
					.column(STATUS, SQLDataType.VARCHAR(16).nullable(false))
					.column(CREATED_AT, SQLDataType.BIGINT.nullable(false))
					.column(END_AT, SQLDataType.BIGINT.nullable(false))
					.constraints(DSL.constraint("pk_auction_items").primaryKey(ID))
					.execute();

			dsl.createIndexIfNotExists("idx_auction_status_end")
					.on(AUCTION_ITEMS, STATUS, END_AT)
					.execute();
			dsl.createIndexIfNotExists("idx_auction_seller_status")
					.on(AUCTION_ITEMS, SELLER_UUID, STATUS)
					.execute();
			dsl.createIndexIfNotExists("idx_auction_item_name")
					.on(AUCTION_ITEMS, ITEM_NAME)
					.execute();

			dsl.createTableIfNotExists(AUCTION_CLAIMS)
					.column(CLAIM_ID, SQLDataType.BIGINT.identity(true))
					.column(CLAIM_PLAYER_UUID, SQLDataType.VARCHAR(36).nullable(false))
					.column(CLAIM_ITEM_NBT, SQLDataType.CLOB.nullable(true))
					.column(CLAIM_MONEY, SQLDataType.DOUBLE.nullable(false).defaultValue(0d))
					.column(CLAIM_REASON, SQLDataType.VARCHAR(64).nullable(true))
					.column(CLAIM_CREATED_AT, SQLDataType.BIGINT.nullable(false))
					.column(CLAIM_EXPIRE_AT, SQLDataType.BIGINT.nullable(false))
					.constraints(DSL.constraint("pk_auction_claims").primaryKey(CLAIM_ID))
					.execute();
			return null;
		});
	}

	public long createAuction(AuctionItem item) {
		return withContext(dsl -> {
			Record record = dsl.insertInto(AUCTION_ITEMS)
					.set(SELLER_UUID, item.getSellerUuid())
					.set(SELLER_NAME, item.getSellerName())
					.set(ITEM_NBT, item.getItemNbt())
					.set(ITEM_NAME, item.getItemName())
					.set(START_PRICE, item.getStartPrice())
					.set(CURRENT_PRICE, item.getCurrentPrice())
					.set(BUYOUT_PRICE, item.getBuyoutPrice())
					.set(STATUS, item.getStatus())
					.set(CREATED_AT, item.getCreatedAt())
					.set(END_AT, item.getEndAt())
					.returning(ID)
					.fetchOne();
			return record == null ? 0L : record.get(ID);
		});
	}

	public AuctionItem getAuction(long id) {
		return withContext(dsl -> {
			Record record = dsl.select(auctionItemFields())
					.from(AUCTION_ITEMS)
					.where(ID.eq(id))
					.fetchOne();
			return record == null ? null : record.into(AuctionItem.class);
		});
	}

	public List<AuctionItem> listActive(int limit, int offset, boolean sortAsc) {
		return withContext(dsl -> dsl.select(auctionItemFields())
				.from(AUCTION_ITEMS)
				.where(STATUS.eq(AuctionStatus.ACTIVE.name()))
				.orderBy(sortAsc ? CURRENT_PRICE.asc() : CURRENT_PRICE.desc(), END_AT.asc())
				.limit(limit)
				.offset(offset)
				.fetchInto(AuctionItem.class));
	}

	public List<AuctionItem> listActiveBySellerUuid(String sellerUuid, int limit, int offset) {
		return withContext(dsl -> dsl.select(auctionItemFields())
				.from(AUCTION_ITEMS)
				.where(STATUS.eq(AuctionStatus.ACTIVE.name())
						.and(SELLER_UUID.eq(sellerUuid)))
				.orderBy(END_AT.asc())
				.limit(limit)
				.offset(offset)
				.fetchInto(AuctionItem.class));
	}

	public List<AuctionItem> listActiveBySellerName(String sellerName, int limit, int offset, boolean sortAsc) {
		return withContext(dsl -> dsl.select(auctionItemFields())
				.from(AUCTION_ITEMS)
				.where(STATUS.eq(AuctionStatus.ACTIVE.name())
						.and(DSL.lower(SELLER_NAME).eq(sellerName.toLowerCase())))
				.orderBy(sortAsc ? CURRENT_PRICE.asc() : CURRENT_PRICE.desc(), END_AT.asc())
				.limit(limit)
				.offset(offset)
				.fetchInto(AuctionItem.class));
	}

	public List<AuctionItem> searchActive(String keyword, int limit, int offset, boolean sortAsc) {
		String search = "%" + keyword.toLowerCase() + "%";
		return withContext(dsl -> dsl.select(auctionItemFields())
				.from(AUCTION_ITEMS)
				.where(STATUS.eq(AuctionStatus.ACTIVE.name())
						.and(DSL.lower(ITEM_NAME).like(search)))
				.orderBy(sortAsc ? CURRENT_PRICE.asc() : CURRENT_PRICE.desc(), END_AT.asc())
				.limit(limit)
				.offset(offset)
				.fetchInto(AuctionItem.class));
	}

	public List<AuctionItem> listExpired(long now) {
		return withContext(dsl -> dsl.select(auctionItemFields())
				.from(AUCTION_ITEMS)
				.where(STATUS.eq(AuctionStatus.ACTIVE.name())
						.and(END_AT.le(now)))
				.fetchInto(AuctionItem.class));
	}

	public int tryBuyNow(long id, double currentPrice, double expectedPrice) {
		return withContext(dsl -> dsl.update(AUCTION_ITEMS)
				.set(CURRENT_PRICE, currentPrice)
				.set(STATUS, AuctionStatus.SOLD.name())
				.where(ID.eq(id)
						.and(STATUS.eq(AuctionStatus.ACTIVE.name()))
						.and(CURRENT_PRICE.eq(expectedPrice)))
				.execute());
	}

	public void updateStatus(long id, AuctionStatus status) {
		withContext(dsl -> dsl.update(AUCTION_ITEMS)
				.set(STATUS, status.name())
				.where(ID.eq(id))
				.execute());
	}

	public int updateStatusIfActive(long id, AuctionStatus status) {
		return withContext(dsl -> dsl.update(AUCTION_ITEMS)
				.set(STATUS, status.name())
				.where(ID.eq(id).and(STATUS.eq(AuctionStatus.ACTIVE.name())))
				.execute());
	}

	public void addClaim(AuctionClaim claim) {
		withContext(dsl -> dsl.insertInto(AUCTION_CLAIMS)
				.set(CLAIM_PLAYER_UUID, claim.getPlayerUuid())
				.set(CLAIM_ITEM_NBT, claim.getItemNbt())
				.set(CLAIM_MONEY, claim.getMoney())
				.set(CLAIM_REASON, claim.getReason())
				.set(CLAIM_CREATED_AT, claim.getCreatedAt())
				.set(CLAIM_EXPIRE_AT, claim.getExpireAt())
				.execute());
	}

	public List<AuctionClaim> listClaims(String playerUuid, long now) {
		return withContext(dsl -> dsl.select(claimFields())
				.from(AUCTION_CLAIMS)
				.where(CLAIM_PLAYER_UUID.eq(playerUuid)
						.and(CLAIM_EXPIRE_AT.gt(now)))
				.orderBy(CLAIM_CREATED_AT.asc())
				.fetchInto(AuctionClaim.class));
	}

	public List<AuctionClaim> listClaimsPaged(String playerUuid, long now, int limit, int offset) {
		return withContext(dsl -> dsl.select(claimFields())
				.from(AUCTION_CLAIMS)
				.where(CLAIM_PLAYER_UUID.eq(playerUuid)
						.and(CLAIM_EXPIRE_AT.gt(now)))
				.orderBy(CLAIM_CREATED_AT.asc())
				.limit(limit)
				.offset(offset)
				.fetchInto(AuctionClaim.class));
	}

	public int countClaims(String playerUuid, long now) {
		return withContext(dsl -> {
			Integer count = dsl.selectCount()
					.from(AUCTION_CLAIMS)
					.where(CLAIM_PLAYER_UUID.eq(playerUuid)
							.and(CLAIM_EXPIRE_AT.gt(now)))
					.fetchOne(0, Integer.class);
			return count == null ? 0 : count;
		});
	}

	public void removeClaim(long id) {
		withContext(dsl -> dsl.deleteFrom(AUCTION_CLAIMS)
				.where(CLAIM_ID.eq(id))
				.execute());
	}

	public int removeExpiredClaims(long now) {
		return withContext(dsl -> dsl.deleteFrom(AUCTION_CLAIMS)
				.where(CLAIM_EXPIRE_AT.le(now))
				.execute());
	}

	public int countActiveBySeller(String sellerUuid) {
		return withContext(dsl -> {
			Integer count = dsl.selectCount()
					.from(AUCTION_ITEMS)
					.where(STATUS.eq(AuctionStatus.ACTIVE.name())
							.and(SELLER_UUID.eq(sellerUuid)))
					.fetchOne(0, Integer.class);
			return count == null ? 0 : count;
		});
	}

	private <T> T withContext(Function<DSLContext, T> action) {
		try (Connection connection = databaseProvider.getDatabase().getConnection()
				.orTimeout(5, TimeUnit.SECONDS)
				.join()) {
			DSLContext dsl = DSL.using(connection, databaseProvider.getDatabase().dialect());
			return action.apply(dsl);
		} catch (Exception e) {
			throw new IllegalStateException("Database operation failed", e);
		}
	}

	private Field<?>[] auctionItemFields() {
		return new Field<?>[] {
				ID.as("id"),
				SELLER_UUID.as("sellerUuid"),
				SELLER_NAME.as("sellerName"),
				ITEM_NBT.as("itemNbt"),
				ITEM_NAME.as("itemName"),
				START_PRICE.as("startPrice"),
				CURRENT_PRICE.as("currentPrice"),
				BUYOUT_PRICE.as("buyoutPrice"),
				STATUS.as("status"),
				CREATED_AT.as("createdAt"),
				END_AT.as("endAt")
		};
	}

	private Field<?>[] claimFields() {
		return new Field<?>[] {
				CLAIM_ID.as("id"),
				CLAIM_PLAYER_UUID.as("playerUuid"),
				CLAIM_ITEM_NBT.as("itemNbt"),
				CLAIM_MONEY.as("money"),
				CLAIM_REASON.as("reason"),
				CLAIM_CREATED_AT.as("createdAt"),
				CLAIM_EXPIRE_AT.as("expireAt")
		};
	}
}