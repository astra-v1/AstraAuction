package xyz.taskov1ch.auction.gui;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryClickEvent;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.inventory.Inventory;
import me.iwareq.fakeinventories.FakeInventory;

public class AuctionGuiListener implements Listener {
	private final AuctionGui auctionGui;

	public AuctionGuiListener(AuctionGui auctionGui) {
		this.auctionGui = auctionGui;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Inventory inventory = event.getInventory();
		if (!(inventory instanceof FakeInventory fakeInventory)) {
			return;
		}
		Player player = event.getPlayer();
		if (!auctionGui.isTracked(player, fakeInventory)) {
			return;
		}
		event.setCancelled(true);
		auctionGui.handleClick(player, fakeInventory, event.getSlot());
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		Inventory inventory = event.getInventory();
		if (!(inventory instanceof FakeInventory fakeInventory)) {
			return;
		}
		auctionGui.handleClose(event.getPlayer(), fakeInventory);
	}
}
