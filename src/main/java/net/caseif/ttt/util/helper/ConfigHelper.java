/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2015, Maxim Roncacé <mproncace@lapis.blue>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.caseif.ttt.util.helper;

import static net.caseif.ttt.util.MiscUtil.isDouble;

import net.caseif.ttt.TTTCore;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * Static utility class for config-related functionality.
 */
public final class ConfigHelper {

    public static final double DETECTIVE_RATIO;
    public static final int MAXIMUM_PLAYERS;
    public static final double TRAITOR_RATIO;
    public static final int MINIMUM_PLAYERS_FOR_DETECTIVE;
    public static final boolean ALLOW_JOIN_AS_SPECTATOR;
    public static final int SCANNER_CHARGE_TIME;
    public static final int CROWBAR_DAMAGE;
    public static final boolean GUNS_OUTSIDE_ARENAS;
    public static final boolean REQUIRE_AMMO_FOR_GUNS;
    public static final int INITIAL_AMMO;
    public static final boolean KARMA_PERSISTENCE;
    public static final int DEFAULT_KARMA;
    public static final int MAX_KARMA;
    public static final int KARMA_HEAL;
    public static final int KARMA_CLEAN_BONUS;
    public static final double KARMA_CLEAN_HALF;
    public static final double DAMAGE_PENALTY;
    public static final int KILL_PENALTY;
    public static final int T_DAMAGE_REWARD;
    public static final int TBONUS;
    public static final boolean KARMA_ROUND_TO_ONE;
    public static final boolean KARMA_STRICT;
    public static final int KARMA_KICK;
    public static final boolean KARMA_BAN;
    public static final int KARMA_BAN_TIME;
    public static final boolean KARMA_DEBUG;
    public static final boolean VERBOSE_LOGGING;
    public static final boolean DAMAGE_REDUCTION;
    public static final boolean ENABLE_AUTO_UPDATE;
    public static final boolean ENABLE_METRICS;
    public static final String LOCALE;
    public static final int MINIMUM_PLAYERS;
    public static final int TIME_LIMIT;
    public static final int SETUP_TIME;
    public static final String SB_ALIVE_PREFIX;
    public static final String SB_MIA_PREFIX;
    public static final String SB_DEAD_PREFIX;
    public static final String SB_I_INNOCENT_PREFIX;
    public static final String SB_I_TRAITOR_PREFIX;
    public static final String SB_I_DETECTIVE_PREFIX;
    public static final String SB_T_INNOCENT_PREFIX;
    public static final String SB_T_TRAITOR_PREFIX;
    public static final String SB_T_DETECTIVE_PREFIX;
    public static final boolean SB_USE_SIDEBAR;
    public static final boolean SEND_TITLES;
    public static final boolean SMALL_STATUS_TITLES;
    public static final boolean SMALL_VICTORY_TITLES;
    public static final Material CROWBAR_ITEM;
    public static final Material GUN_ITEM;

    private static final ImmutableMap<String, String> LEGACY_NODES;

    static {
        TIME_LIMIT = getInt("roundtime-seconds");
        SETUP_TIME = getInt("preptime-seconds");
        MINIMUM_PLAYERS = getInt("minimum-players");
        MAXIMUM_PLAYERS = getInt("maximum-players");
        ALLOW_JOIN_AS_SPECTATOR = getBoolean("allow-join-as-spectator");
        TRAITOR_RATIO = getDouble("traitor-pct");
        DETECTIVE_RATIO = getDouble("detective-pct");
        MINIMUM_PLAYERS_FOR_DETECTIVE = getInt("minimum-players-for-detective");
        SCANNER_CHARGE_TIME = getInt("scanner-charge-time");
        CROWBAR_DAMAGE = getInt("crowbar-damage");
        GUNS_OUTSIDE_ARENAS = getBoolean("guns-outside-arenas");
        REQUIRE_AMMO_FOR_GUNS = getBoolean("require-ammo-for-guns");
        INITIAL_AMMO = getInt("initial-ammo");

        KARMA_STRICT = getBoolean("karma-strict");
        DEFAULT_KARMA = getInt("karma-starting");
        MAX_KARMA = getInt("karma-max");
        DAMAGE_PENALTY = getDouble("karma-ratio");
        KILL_PENALTY = getInt("karma-kill-penalty");
        KARMA_HEAL = getInt("karma-round-increment");
        KARMA_CLEAN_BONUS = getInt("karma-clean-bonus");
        KARMA_CLEAN_HALF = getDouble("karma-clean-half");
        T_DAMAGE_REWARD = getInt("karma-traitordmg-ratio");
        TBONUS = getInt("karma-traitorkill-bonus");
        KARMA_KICK = getInt("karma-low-autokick");
        KARMA_BAN = getBoolean("karma-low-ban");
        KARMA_BAN_TIME = getInt("karma-low-ban-minutes");
        KARMA_PERSISTENCE = getBoolean("karma-persist");
        DAMAGE_REDUCTION = getBoolean("karma-damage-reduction");
        KARMA_ROUND_TO_ONE = getBoolean("karma-round-to-one");
        KARMA_DEBUG = getBoolean("karma-debug");

        VERBOSE_LOGGING = getBoolean("verbose-logging");
        ENABLE_AUTO_UPDATE = getBoolean("enable-auto-update");
        ENABLE_METRICS = getBoolean("enable-metrics");
        LOCALE = getString("locale");
        SB_ALIVE_PREFIX = getString("sb-alive-prefix");
        SB_MIA_PREFIX = getString("sb-mia-prefix");
        SB_DEAD_PREFIX = getString("sb-dead-prefix");
        SB_I_INNOCENT_PREFIX = getString("sb-i-innocent-prefix");
        SB_I_TRAITOR_PREFIX = getString("sb-i-traitor-prefix");
        SB_I_DETECTIVE_PREFIX = getString("sb-i-detective-prefix");
        SB_T_INNOCENT_PREFIX = getString("sb-t-innocent-prefix");
        SB_T_TRAITOR_PREFIX = getString("sb-t-traitor-prefix");
        SB_T_DETECTIVE_PREFIX = getString("sb-t-detective-prefix");
        SB_USE_SIDEBAR = getBoolean("sb-use-sidebar");
        SEND_TITLES = getBoolean("send-titles");
        SMALL_STATUS_TITLES = getBoolean("small-status-titles");
        SMALL_VICTORY_TITLES = getBoolean("small-victory-titles");
        CROWBAR_ITEM = getMaterial("crowbar-item", Material.IRON_SWORD);
        GUN_ITEM = getMaterial("gun-item", Material.IRON_BARDING);

        LEGACY_NODES = ImmutableMap.<String, String>builder()
                .put("setup-time", "preptime-seconds")
                .put("time-limit", "roundtime-seconds")

                .put("traitor-ratio", "traitor-pct")
                .put("detective-ratio", "detective-pct")
                .put("minimum-players-for-detective", "detective-min-players")

                .put("default-karma", "karma-starting")
                .put("max-karma", "karma-max")
                .put("damage-penalty", "karma-ratio")
                .put("kill-penalty", "karma-kill-penalty")
                .put("karma-heal", "karma-round-increment")
                .put("t-damage-reward", "karma-traitordmg-ratio")
                .put("tbonus", "karma-traitorkill-bonus")
                .put("karma-kick", "karma-low-autokick")
                .put("karma-ban", "karma-low-ban")
                .put("karma-ban-time", "karma-low-ban-minutes")
                .put("karma-persistance", "karma-persist")
                .put("damage-reduction", "karma-damage-reduction")
                .build();
    }

    public static String getString(String key) {
        String value = TTTCore.getInstance().getConfig().getString(key);
        if (value != null) {
            if (value.contains("Â§")) { // fix encoding mistakes on Windoofs
                value = value.replace("Â§", "§");
            }
            return value;
        }
        return "";
    }

    public static boolean getBoolean(String key) {
        return TTTCore.getInstance().getConfig().getBoolean(key);
    }

    public static int getInt(String key) {
        return TTTCore.getInstance().getConfig().getInt(key);
    }

    public static double getDouble(String key) {
        return TTTCore.getInstance().getConfig().getDouble(key);
    }

    public static Material getMaterial(String key) {
        return getMaterial(key, null);
    }

    public static Material getMaterial(String key, Material fallback) {
        Material m = Material.getMaterial(TTTCore.getInstance().getConfig().getString(key));
        return m != null ? m : fallback;
    }

    private static String getModernKey(String legacyKey) {
        return LEGACY_NODES.get(legacyKey);
    }

    private static Set<String> getLegacyKeys(final String modernKey) {
        return new HashSet<>(Collections2.filter(LEGACY_NODES.keySet(), new Predicate<String>() {
            @Override
            public boolean apply(String key) {
                return LEGACY_NODES.get(key).equals(modernKey);
            }
        }));
    }

    public static void addMissingKeys() throws InvalidConfigurationException, IOException {
        BufferedReader stockConfig
                = new BufferedReader(new InputStreamReader(TTTCore.class.getResourceAsStream("/config.yml")));
        File userConfigFile = new File(TTTCore.getInstance().getDataFolder(), "config.yml");
        YamlConfiguration userConfig = new YamlConfiguration();
        userConfig.load(userConfigFile);
        StringBuilder sb = new StringBuilder();
        final char newlineChar = '\n';
        String line;
        // Before reading this code, understand that this method reconstructs the user config from scratch using the
        // internal config as a foundation and substituting in user-changed values where possible.
        while ((line = stockConfig.readLine()) != null) { // iterate the lines of the internal config file
            if (!line.startsWith("#")) { // check that the line's not a comment
                if (line.contains(":")) { // check that it's not a list item or something
                    //TODO: this method doesn't support nested keys, but it doesn't need to atm anyway
                    String key = line.split(":")[0]; // derive the key
                    String internalValue = line.substring(key.length() + 1, line.length()).trim(); // derive the value
                    // get the value of the key as defined in the user config
                    String userValue = null;
                    if (userConfig.contains(key.trim())) {
                        userValue = userConfig.getString(key.trim());
                    } else {
                        Set<String> legacyKeys = getLegacyKeys(key.trim());
                        for (String leg : legacyKeys) {
                            if (userConfig.contains(leg)) {
                                userValue = userConfig.getString(leg);
                                break;
                            }
                        }
                        if (userValue == null) {
                            userValue = internalValue;
                        }
                    }

                    if (key.equals("gun-item") && userValue.equals("IRON_HORSE_ARMOR")) {
                        userValue = "IRON_BARDING";
                    }

                    // This seems counterintuitive, but bear with me: essentially, the internal value will be used if
                    // the key is missing from the user config (so that `userValue == internalValue`), or if the
                    // user-defined value is effectively the same as the internal value
                    // (i.e. `1 == 1` or `1.0 == 1.00`). This ensures that the user config is as clean as possible
                    boolean equal;
                    try {
                        // try to parse it as a number and compare it
                        equal = NumberFormat.getInstance().parse(internalValue)
                                .equals(NumberFormat.getInstance().parse(userValue));
                    } catch (ParseException ex) {
                        // it's not a number, so just compare it as a string
                        equal = internalValue.equals(userValue);
                    }
                    if (!equal) { // if they're not effectively equal, reconstruct the line using the user-defined value
                        if (isDouble(userValue)) {
                            // clean the value-to-write up if it's a double
                            userValue = BigDecimal.valueOf(Double.parseDouble(userValue))
                                    .stripTrailingZeros().toPlainString();
                        }
                        sb.append(key).append(": ").append(userValue).append(newlineChar);
                        continue;
                    }
                }
            }
            // just copy the line directly if it hasn't already been reconstructed
            sb.append(line).append(newlineChar);
        }
        FileHelper.copyFile(userConfigFile, new File(userConfigFile.getParentFile(), "config.yml.old"));
        FileWriter w = new FileWriter(userConfigFile);
        w.append(sb.toString());
        w.flush();
    }
}
