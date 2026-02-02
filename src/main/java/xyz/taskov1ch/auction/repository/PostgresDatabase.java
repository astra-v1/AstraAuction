package xyz.taskov1ch.auction.repository;

import com.mefrreex.jooq.database.IDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import org.jooq.SQLDialect;

public class PostgresDatabase implements IDatabase {
	private final String url;
	private final String user;
	private final String password;

	public PostgresDatabase(String hostWithPort, String database, String user, String password) {
		this.url = "jdbc:postgresql://" + hostWithPort + "/" + database;
		this.user = user;
		this.password = password;
	}

	@Override
	public CompletableFuture<Connection> getConnection() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return DriverManager.getConnection(url, user, password);
			} catch (SQLException e) {
				throw new IllegalStateException("Failed to open PostgreSQL connection", e);
			}
		});
	}

	@Override
	public SQLDialect dialect() {
		return SQLDialect.POSTGRES;
	}
}