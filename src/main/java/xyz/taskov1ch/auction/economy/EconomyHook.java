package xyz.taskov1ch.auction.economy;

import cn.nukkit.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class EconomyHook {
	private final boolean available;
	private final Object economyApi;
	private final Method myMoney;
	private final Method addMoney;
	private final Method reduceMoney;
	private final Method addMoneyUuid;
	private final Method addMoneyString;

	public EconomyHook() {
		Object api = null;
		Method money = null;
		Method add = null;
		Method reduce = null;
		Method addUuid = null;
		Method addString = null;
		boolean ok = false;
		try {
			Class<?> clazz = Class.forName("me.onebone.economyapi.EconomyAPI");
			api = clazz.getMethod("getInstance").invoke(null);
			money = clazz.getMethod("myMoney", Player.class);
			add = clazz.getMethod("addMoney", Player.class, double.class);
			reduce = clazz.getMethod("reduceMoney", Player.class, double.class);
			addUuid = clazz.getMethod("addMoney", UUID.class, double.class);
			addString = clazz.getMethod("addMoney", String.class, double.class);
			ok = true;
		} catch (Exception ignored) {
			ok = false;
		}
		this.available = ok;
		this.economyApi = api;
		this.myMoney = money;
		this.addMoney = add;
		this.reduceMoney = reduce;
		this.addMoneyUuid = addUuid;
		this.addMoneyString = addString;
	}

	public boolean isAvailable() {
		return available;
	}

	public boolean has(Player player, double amount) {
		if (!available) {
			return false;
		}
		try {
			double bal = (double) myMoney.invoke(economyApi, player);
			return bal >= amount;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean withdraw(Player player, double amount) {
		if (!available) {
			return false;
		}
		try {
			reduceMoney.invoke(economyApi, player, amount);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean deposit(Player player, double amount) {
		if (!available) {
			return false;
		}
		try {
			addMoney.invoke(economyApi, player, amount);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean deposit(String uuid, double amount) {
		if (!available) {
			return false;
		}
		try {
			if (addMoneyUuid != null) {
				addMoneyUuid.invoke(economyApi, UUID.fromString(uuid), amount);
				return true;
			}
			if (addMoneyString != null) {
				addMoneyString.invoke(economyApi, uuid, amount);
				return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}
}
