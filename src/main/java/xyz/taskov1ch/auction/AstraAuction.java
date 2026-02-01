package xyz.taskov1ch.auction;

import cn.nukkit.plugin.PluginBase;
import xyz.taskov1ch.auction.command.AuctionCommand;
import xyz.taskov1ch.auction.config.ConfigManager;
import xyz.taskov1ch.auction.database.DatabaseProvider;
import xyz.taskov1ch.auction.gui.AuctionGui;
import xyz.taskov1ch.auction.gui.AuctionGuiListener;
import xyz.taskov1ch.auction.service.AuctionService;
import xyz.taskov1ch.auction.util.Lang;

public class AstraAuction extends PluginBase {
	private static AstraAuction instance;
	private ConfigManager configManager;
	private DatabaseProvider databaseProvider;
	private AuctionService auctionService;
	private AuctionGui auctionGui;

	@Override
	public void onEnable() {
		instance = this;
		saveDefaultConfig();

		configManager = new ConfigManager(this);

		databaseProvider = new DatabaseProvider(this, configManager);
		databaseProvider.init();

		auctionService = new AuctionService(this, databaseProvider, configManager);
		auctionGui = new AuctionGui(auctionService, configManager);

		Lang.init(this, configManager);
		getServer().getPluginManager().registerEvents(new AuctionGuiListener(auctionGui), this);

		getServer().getCommandMap().register("astraauction", new AuctionCommand(this, auctionService));

		getLogger().info("AstraAuction enabled.");
	}

	@Override
	public void onDisable() {
		if (databaseProvider != null) {
			databaseProvider.shutdown();
		}
		getLogger().info("AstraAuction disabled.");
	}

	public static AstraAuction getInstance() {
		return instance;
	}

	public ConfigManager getConfigManager() {
		return configManager;
	}

	public DatabaseProvider getDatabaseProvider() {
		return databaseProvider;
	}

	public AuctionService getAuctionService() {
		return auctionService;
	}

	public AuctionGui getAuctionGui() {
		return auctionGui;
	}
}
