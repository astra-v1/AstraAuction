package xyz.taskov1ch.auction.database;

import cn.nukkit.plugin.PluginBase;
import xyz.taskov1ch.auction.config.ConfigManager;

public class DatabaseProvider {
	private final PluginBase plugin;
	private final ConfigManager configManager;
	private AuctionDatabase database;
	private String type;

	public DatabaseProvider(PluginBase plugin, ConfigManager configManager) {
		this.plugin = plugin;
		this.configManager = configManager;
	}

	public void init() {
		String type = configManager.getDatabaseType();
		this.type = type;
		switch (type) {
			case "mysql" -> {
				String host = configManager.getMySqlHost();
				int port = configManager.getMySqlPort();
				String hostWithPort = host != null && host.contains(":") ? host : host + ":" + port;
				database = new AuctionMySQLDatabase(
						hostWithPort,
						configManager.getMySqlDatabase(),
						configManager.getMySqlUsername(),
						configManager.getMySqlPassword());
			}
			case "postgres", "postgresql" -> {
				String host = configManager.getPostgresHost();
				int port = configManager.getPostgresPort();
				String hostWithPort = host != null && host.contains(":") ? host : host + ":" + port;
				database = new AuctionPostgreSQLDatabase(
						hostWithPort,
						configManager.getPostgresDatabase(),
						configManager.getPostgresUsername(),
						configManager.getPostgresPassword());
			}
			default -> database = new AuctionSQLiteDatabase(
					configManager.getSqliteFolder(),
					configManager.getSqliteDatabase());
		}

		plugin.getLogger().info("Database initialized: " + type);
	}

	public String getType() {
		return type == null ? "sqlite" : type;
	}

	public AuctionDatabase getDatabase() {
		return database;
	}

	public void shutdown() {
		if (database instanceof AutoCloseable) {
			try {
				((AutoCloseable) database).close();
			} catch (Exception e) {
				plugin.getLogger().warning("Failed to close database: " + e.getMessage());
			}
		}
	}
}
