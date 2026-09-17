package net.lucy;

import fi.dy.masa.malilib.util.StringUtils;
import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyMaterialGathering implements ModInitializer {
	public static final String MOD_ID = "lazymaterialgathering";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String MOD_NAME = "Lazy Material Gathering";
	public static final String MOD_VERSION = StringUtils.getModVersionString(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}