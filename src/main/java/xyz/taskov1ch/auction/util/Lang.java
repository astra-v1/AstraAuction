package xyz.taskov1ch.auction.util;

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
import org.densy.polyglot.api.util.LanguageStrategy;
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

	private Lang() {
	}

	public static void init(PluginBase plugin, ConfigManager configManager) {
		Lang.plugin = plugin;
		ensureResources();
		initializePolyglot();
		load(configManager.getLanguage());
	}

	public static String t(String key, Object... placeholders) {
		if (translation == null || currentLanguage == null) {
			return key;
		}
		if (placeholders == null || placeholders.length < 2) {
			return translation.translate(currentLanguage, key);
		}
		var params = TrParameters.keyed();
		for (int i = 0; i < placeholders.length - 1; i += 2) {
			String name = String.valueOf(placeholders[i]);
			Object value = placeholders[i + 1];
			params.put(name, value);
		}
		return translation.translate(currentLanguage, key, params);
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
		defaultLanguage = SimpleLanguage.ENG;
		translation.setDefaultLanguage(defaultLanguage);
		translation.setLanguageStrategy(LanguageStrategy.mappingsBuilder()
				.put(SimpleLanguage.ENG, SimpleLanguage.RUS)
				.build());
		translation.setFallbackStrategy(FallbackStrategy.keyToKey());
	}

	private static void load(String language) {
		if (languageStandard == null) {
			return;
		}
		Language parsed = safeParse(language);
		currentLanguage = parsed == null ? defaultLanguage : parsed;
	}

	private static Language safeParse(String language) {
		if (languageStandard == null || language == null || language.isBlank()) {
			return null;
		}
		try {
			return languageStandard.parseLanguage(language.trim().toLowerCase());
		} catch (Exception e) {
			return null;
		}
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
