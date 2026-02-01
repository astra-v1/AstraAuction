package xyz.taskov1ch.auction.database;

import me.hteppl.data.database.PostgreSQLDatabase;

public class AuctionPostgreSQLDatabase extends PostgreSQLDatabase implements AuctionDatabase {
	public AuctionPostgreSQLDatabase(String host, String database, String username, String password) {
		super(host, database, username, password);
	}
}
