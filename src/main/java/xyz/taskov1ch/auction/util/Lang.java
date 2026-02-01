package xyz.taskov1ch.auction.util;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import java.io.File;
import java.util.Locale;
import xyz.taskov1ch.auction.config.ConfigManager;

public final class Lang {
	private static final String[] SUPPORTED = new String[] { "ru", "en", "zh", "ja" };
	private static PluginBase plugin;
	private static Config langConfig;

	private Lang() {
	}

	public static void init(PluginBase plugin, ConfigManager configManager) {
		Lang.plugin = plugin;
		ensureResources();
		load(configManager.getLanguage());
	}

	public static String t(String key, Object... placeholders) {
		if (langConfig == null) {
			return key;
		}
		String value = langConfig.getString(key, key);
		if (placeholders != null && placeholders.length >= 2) {
			for (int i = 0; i < placeholders.length - 1; i += 2) {
				String name = String.valueOf(placeholders[i]);
				String replacement = String.valueOf(placeholders[i + 1]);
				value = value.replace("{" + name + "}", replacement);
			}
		}
		return value;
	}

	public static String colorize(String text) {
		return TextFormat.colorize('&', text);
	}

	private static void load(String language) {
		String code = normalize(language);
		File file = new File(plugin.getDataFolder(), "lang/lang_" + code + ".yml");
		if (!file.exists()) {
			file = new File(plugin.getDataFolder(), "lang/lang_ru.yml");
		}
		langConfig = new Config(file, Config.YAML);
	}

	private static void ensureResources() {
		for (String code : SUPPORTED) {
			String path = "lang/lang_" + code + ".yml";
			plugin.saveResource(path, false);
		}
	}

	private static String normalize(String language) {
		if (language == null) {
			return "ru";
		}
		String value = language.toLowerCase(Locale.ROOT);
		for (String code : SUPPORTED) {
			if (code.equals(value)) {
				return code;
			}
		}
		return "ru";
	}
}
