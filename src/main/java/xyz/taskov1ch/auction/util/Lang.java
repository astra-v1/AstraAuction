package xyz.taskov1ch.auction.util;

import cn.nukkit.Player;
import cn.nukkit.lang.LangCode;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.densy.polyglot.api.Translation;
import org.densy.polyglot.api.context.TranslationContext;
import org.densy.polyglot.api.language.Language;
import org.densy.polyglot.api.language.LanguageStandard;
import org.densy.polyglot.api.util.FallbackStrategy;
import org.densy.polyglot.common.language.SimpleLanguage;
import org.densy.polyglot.core.context.BaseTranslationContext;
import org.densy.polyglot.core.language.SimpleLanguageStandard;
import org.densy.polyglot.core.provider.YamlFileProvider;
import org.densy.polyglot.core.parameter.TrParameters;
import xyz.taskov1ch.auction.config.ConfigManager;

public final class Lang {
	private static PluginBase plugin;
	private static Translation translation;
	private static LanguageStandard languageStandard;
	private static Language defaultLanguage;
	private static Language currentLanguage;
	private static Language serverLanguage;
	private static String languageMode;

	private Lang() {
	}

	public static void init(PluginBase plugin, ConfigManager configManager) {
		Lang.plugin = plugin;
		ensureResources();
		initializePolyglot();
		languageMode = safeLower(configManager.getLanguageMode());
		defaultLanguage = parseLanguage(configManager.getDefaultLanguage());
		translation.setDefaultLanguage(defaultLanguage);
		serverLanguage = parseLanguage(getServerLanguageCode());
		load(languageMode);
	}

	public static String t(String key, Object... placeholders) {
		if (translation == null || defaultLanguage == null) {
			return key;
		}
		Language lang = resolveLanguage(null);
		if (placeholders == null || placeholders.length < 2) {
			return translation.translate(lang, key);
		}
		var params = TrParameters.keyed();
		for (int i = 0; i < placeholders.length - 1; i += 2) {
			String name = String.valueOf(placeholders[i]);
			Object value = placeholders[i + 1];
			params.put(name, value);
		}
		return translation.translate(lang, key, params);
	}

	public static String t(Player player, String key, Object... placeholders) {
		if (translation == null || defaultLanguage == null) {
			return key;
		}
		Language lang = resolveLanguage(player);
		if (placeholders == null || placeholders.length < 2) {
			return translation.translate(lang, key);
		}
		var params = TrParameters.keyed();
		for (int i = 0; i < placeholders.length - 1; i += 2) {
			String name = String.valueOf(placeholders[i]);
			Object value = placeholders[i + 1];
			params.put(name, value);
		}
		return translation.translate(lang, key, params);
	}

	public static String colorize(String text) {
		return TextFormat.colorize('&', text);
	}

	private static void initializePolyglot() {
		languageStandard = new SimpleLanguageStandard();
		TranslationContext context = new BaseTranslationContext();
		context.setLanguageStandard(languageStandard);
		File langFolder = new File(plugin.getDataFolder(), "lang");
		translation = context.createTranslation(new YamlFileProvider(langFolder, languageStandard));
		translation.setFallbackStrategy(FallbackStrategy.keyToKey());
	}

	private static void load(String language) {
		if (languageStandard == null) {
			return;
		}
		if (isAutodetect(language)) {
			currentLanguage = defaultLanguage;
			return;
		}
		if (isServer(language)) {
			currentLanguage = serverLanguage == null ? defaultLanguage : serverLanguage;
			return;
		}
		Language parsed = parseLanguage(language);
		currentLanguage = parsed == null ? defaultLanguage : parsed;
	}

	private static Language parseLanguage(String language) {
		if (languageStandard == null || language == null || language.isBlank()) {
			return null;
		}
		String code = safeLower(language);
		try {
			String primary = normalizeLanguageCode(code);
			return switch (primary) {
				case "eng", "en" -> SimpleLanguage.ENG;
				case "rus", "ru" -> SimpleLanguage.RUS;
				case "ukr", "ua", "uk" -> SimpleLanguage.UKR;
				case "jpn", "ja" -> SimpleLanguage.JPN;
				default -> languageStandard.parseLanguage(code);
			};
		} catch (Exception e) {
			return null;
		}
	}

	private static Language resolveLanguage(Player player) {
		if (isAutodetect(languageMode)) {
			String playerCode = getPlayerLanguageCode(player);
			Language detected = parseLanguage(playerCode);
			return detected == null ? defaultLanguage : detected;
		}
		if (isServer(languageMode)) {
			return serverLanguage == null ? defaultLanguage : serverLanguage;
		}
		return currentLanguage == null ? defaultLanguage : currentLanguage;
	}

	private static String getPlayerLanguageCode(Player player) {
		if (player == null) {
			return null;
		}
		LangCode code = player.getLanguageCode();
		if (code == null) {
			return null;
		}
		return code.name().toLowerCase();
	}

	private static String getServerLanguageCode() {
		if (plugin == null) {
			return null;
		}
		try {
			String value = plugin.getServer().getLanguage().getLang();
			return value == null ? null : value.toLowerCase();
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean isAutodetect(String value) {
		return "autodetect".equalsIgnoreCase(value);
	}

	private static boolean isServer(String value) {
		return "server".equalsIgnoreCase(value);
	}

	private static String safeLower(String value) {
		return value == null ? null : value.trim().toLowerCase();
	}

	private static String normalizeLanguageCode(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String code = value.trim().toLowerCase();
		code = code.replace('-', '_');
		int idx = code.indexOf('_');
		if (idx > 0) {
			return code.substring(0, idx);
		}
		return code;
	}

	private static void ensureResources() {
		File langFolder = new File(plugin.getDataFolder(), "lang");
		if (!langFolder.exists() && !langFolder.mkdirs()) {
			return;
		}
		File jarFile = plugin.getFile();
		if (jarFile == null || !jarFile.exists()) {
			return;
		}
		try (JarFile jar = new JarFile(jarFile)) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (entry.isDirectory()) {
					continue;
				}
				if (!name.startsWith("lang/") || !name.endsWith(".yml")) {
					continue;
				}
				File outFile = new File(plugin.getDataFolder(), name);
				if (!outFile.exists()) {
					plugin.saveResource(name, false);
				}
			}
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to extract language files: " + e.getMessage());
		}
	}
}
