package xyz.taskov1ch.auction.database;

import org.sql2o.Connection;

public interface AuctionDatabase {
	Connection openConnection();
}
