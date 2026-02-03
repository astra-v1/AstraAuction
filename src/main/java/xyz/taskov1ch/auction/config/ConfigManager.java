package xyz.taskov1ch.auction.config;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

public class ConfigManager {
	private Config config;

	public ConfigManager(PluginBase plugin) {
		this.config = plugin.getConfig();
	}

	public String getDatabaseType() {
		return config.getString("database.type", "sqlite").toLowerCase();
	}

	public String getSqliteFolder() {
		return config.getString("database.sqlite.folder", "data");
	}

	public String getSqliteDatabase() {
		return config.getString("database.sqlite.database", "auction");
	}

	public String getMySqlHost() {
		return config.getString("database.mysql.host", "localhost");
	}

	public int getMySqlPort() {
		return config.getInt("database.mysql.port", 3306);
	}

	public String getMySqlDatabase() {
		return config.getString("database.mysql.database", "auction");
	}

	public String getMySqlUsername() {
		return config.getString("database.mysql.username", "root");
	}

	public String getMySqlPassword() {
		return config.getString("database.mysql.password", "");
	}

	public String getPostgresHost() {
		return config.getString("database.postgres.host", "localhost");
	}

	public int getPostgresPort() {
		return config.getInt("database.postgres.port", 5432);
	}

	public String getPostgresDatabase() {
		return config.getString("database.postgres.database", "auction");
	}

	public String getPostgresUsername() {
		return config.getString("database.postgres.username", "postgres");
	}

	public String getPostgresPassword() {
		return config.getString("database.postgres.password", "");
	}

	public String getLanguageMode() {
		return config.getString("language.value", "eng");
	}

	public String getDefaultLanguage() {
		return config.getString("language.default", "eng");
	}

	public int getAuctionDurationSeconds() {
		return config.getInt("auction.duration-seconds", 172800);
	}

	public double getAuctionTaxPercent() {
		return config.getDouble("auction.tax-percent", 10.0);
	}

	public boolean isRoundPrices() {
		return config.getBoolean("auction.round-prices", false);
	}

	public int getMaxSlots() {
		return config.getInt("auction.max-slots", 6);
	}

	public int getClaimExpireSeconds() {
		return config.getInt("auction.claim-expire-seconds", 604800);
	}

	public int getGuiPageSize() {
		return config.getInt("auction.gui.page-size", 45);
	}

	public String getGuiSortDefault() {
		return config.getString("auction.gui.sort-default", "price_asc");
	}

	public int getGuiOpenDelayTicks() {
		return config.getInt("auction.gui.open-delay-ticks", 10);
	}
}
