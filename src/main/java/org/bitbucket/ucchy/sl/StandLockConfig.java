/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.sl;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * コンフィグ管理クラス
 * @author ucchy
 */
public class StandLockConfig {

    private StandLock parent;

    private String lang;
    private int armorStandLimit;
    private boolean autoLock;
    private int cleanupTaskDelay;
    private boolean cleanupTaskLog;
    private boolean cooperateWithArmorStandEditor;

    /**
     * コンストラクタ
     * @param parent
     */
    public StandLockConfig(StandLock parent) {

        this.parent = parent;
        reloadConfig();
    }

    /**
     * コンフィグを読み込む
     */
    protected void reloadConfig() {

        if ( !parent.getDataFolder().exists() ) {
            parent.getDataFolder().mkdirs();
        }

        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            Utility.copyFileFromJar(
                    parent.getJarFile(), file, "config_ja.yml", false);
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        lang = conf.getString("lang", "ja");
        armorStandLimit = conf.getInt("armorStandLimit", 100);
        autoLock = conf.getBoolean("autoLock", true);

        cleanupTaskDelay = conf.getInt("cleanupTaskDelay", -1);
        cleanupTaskLog = conf.getBoolean("cleanupTaskLog", true);

        cooperateWithArmorStandEditor =
                conf.getBoolean("cooperateWithArmorStandEditor", true);
    }

    public String getLang() {
        return lang;
    }

    public int getArmorStandLimit() {
        return armorStandLimit;
    }

    public boolean isAutoLock() {
        return autoLock;
    }

    /**
     * @return cleanupTaskDelay
     */
    public int getCleanupTaskDelay() {
        return cleanupTaskDelay;
    }

    /**
     * @return cleanupTaskLog
     */
    public boolean isCleanupTaskLog() {
        return cleanupTaskLog;
    }

    public boolean isCooperateWithArmorStandEditor() {
        return cooperateWithArmorStandEditor;
    }

}
