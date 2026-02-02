package xyz.taskov1ch.auction.repository;

import cn.nukkit.plugin.PluginBase;
import com.mefrreex.jooq.JOOQConnector;
import com.mefrreex.jooq.database.IDatabase;
import com.mefrreex.jooq.database.MySQLDatabase;
import com.mefrreex.jooq.database.SQLiteDatabase;
import java.io.File;
import xyz.taskov1ch.auction.config.ConfigManager;

public class DatabaseProvider {
	private final PluginBase plugin;
	private final ConfigManager configManager;
	private IDatabase database;
	private String type;

	public DatabaseProvider(PluginBase plugin, ConfigManager configManager) {
		this.plugin = plugin;
		this.configManager = configManager;
	}

	public void init() {
		JOOQConnector.setJOOQMessagesEnabled(false);
		String type = configManager.getDatabaseType();
		this.type = type;
		switch (type) {
			case "mysql" -> {
				String host = configManager.getMySqlHost();
				int port = configManager.getMySqlPort();
				String hostWithPort = host != null && host.contains(":") ? host : host + ":" + port;
				database = new MySQLDatabase(
						hostWithPort,
						configManager.getMySqlDatabase(),
						configManager.getMySqlUsername(),
						configManager.getMySqlPassword());
			}
			case "postgres", "postgresql" -> {
				String host = configManager.getPostgresHost();
				int port = configManager.getPostgresPort();
				String hostWithPort = host != null && host.contains(":") ? host : host + ":" + port;
				database = new PostgresDatabase(
						hostWithPort,
						configManager.getPostgresDatabase(),
						configManager.getPostgresUsername(),
						configManager.getPostgresPassword());
			}
			default -> {
				File sqliteFolder = new File(plugin.getDataFolder(), configManager.getSqliteFolder());
				if (!sqliteFolder.exists()) {
					sqliteFolder.mkdirs();
				}
				File sqliteFile = new File(sqliteFolder, configManager.getSqliteDatabase() + ".db");
				database = new SQLiteDatabase(sqliteFile);
			}
		}

		plugin.getLogger().info("Database initialized: " + type);
	}

	public String getType() {
		return type == null ? "sqlite" : type;
	}

	public IDatabase getDatabase() {
		return database;
	}

	public void shutdown() {
		// TODO: Remove or implement proper shutdown if needed
	}
}