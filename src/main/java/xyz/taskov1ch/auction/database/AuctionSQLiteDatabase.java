package xyz.taskov1ch.auction.database;

import me.hteppl.data.database.SQLiteDatabase;

public class AuctionSQLiteDatabase extends SQLiteDatabase implements AuctionDatabase {
	public AuctionSQLiteDatabase(String folder, String database) {
		super(folder, database);
	}
}
