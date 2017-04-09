/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.sl;

import java.io.File;
import java.util.List;

import org.bitbucket.ucchy.sl.bridge.PermissionsExBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ArmorStand Lock Plugin
 * @author ucchy
 */
public class StandLock extends JavaPlugin {

    public static final String PERMISSION_ENTITY = "standlock.entity";
    public static final String PERMISSION_COMMAND = "standlock.command";
    public static final String PERMISSION_INFINITE_PLACE =
            "standlock.entity.infinite-place";

    private static final String DATA_FOLDER = "data";

    private LockDataManager lockManager;
    private StandLockConfig config;
    private StandLockCommand command;

    private PermissionsExBridge pex;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // サーバーのバージョンが v1.7.10 以前なら、プラグインを停止して動作しない。
        if ( !Utility.isCB18orLater() ) {
            getLogger().warning("Bukkit 1.7.x 以前のバージョンでは、このプラグインは動作しません。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // マネージャを生成し、データをロードする
        lockManager = new LockDataManager(
                new File(getDataFolder(), DATA_FOLDER));

        // コンフィグをロードする
        config = new StandLockConfig(this);

        // メッセージをロードする
        Messages.initialize(getFile(), getDataFolder());
        Messages.reload(config.getLang());

        // PermissionsExをロード
        if ( getServer().getPluginManager().isPluginEnabled("PermissionsEx") ) {
            pex = PermissionsExBridge.load(
                    getServer().getPluginManager().getPlugin("PermissionsEx"));
        }

        // ArmorStand Editor と連携する
        /* この部分は、ArmorStandEditor1.9.4-0.1.11 との組み合わせでしか動作しない。see issue #13
        if ( getServer().getPluginManager().isPluginEnabled("ArmorStandEditor")
                && config.isCooperateWithArmorStandEditor() ) {
            ArmorStandEditorBridge.registerToArmorStandEditor(
                    getServer().getPluginManager().getPlugin("ArmorStandEditor"));
        }
        */

        // リスナークラスを登録する
        getServer().getPluginManager().registerEvents(
                new StandLockListener(this), this);

        // コマンドクラスを作成する
        command = new StandLockCommand(this);

        // クリーンアップタスクを登録する
        if ( config.getCleanupTaskDelay() >= 0 ) {
            LockDataCleanupTask task = new LockDataCleanupTask();
            task.runTaskLater(this, config.getCleanupTaskDelay() * 60 * 20);
        }
    }

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return this.command.onCommand(sender, command, label, args);
    }

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.command.onTabComplete(sender, command, alias, args);
    }

    /**
     * ロックデータマネージャを返す
     * @return ロックデータマネージャ
     */
    public LockDataManager getLockDataManager() {
        return lockManager;
    }

    /**
     * コンフィグデータを返す
     * @return
     */
    public StandLockConfig getStandLockConfig() {
        return config;
    }

    /**
     * PermissionsExへのアクセスブリッジを取得する
     * @return PermissionsExBridge、ロードされていなければnullになる
     */
    public PermissionsExBridge getPex() {
        return pex;
    }

    /**
     * このプラグインのJarファイルを返す
     * @return Jarファイル
     */
    protected File getJarFile() {
        return getFile();
    }

    /**
     * このプラグインのインスタンスを返す。
     * @return インスタンス
     */
    public static StandLock getInstance() {
        return (StandLock)Bukkit.getPluginManager().getPlugin("StandLock");
    }
}
