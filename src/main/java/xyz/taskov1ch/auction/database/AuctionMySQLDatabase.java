package xyz.taskov1ch.auction.database;

import me.hteppl.data.database.MySQLDatabase;

public class AuctionMySQLDatabase extends MySQLDatabase implements AuctionDatabase {
	public AuctionMySQLDatabase(String host, String database, String username, String password) {
		super(host, database, username, password);
	}
}
