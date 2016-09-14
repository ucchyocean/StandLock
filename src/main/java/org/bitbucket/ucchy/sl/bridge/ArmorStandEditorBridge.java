/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.sl.bridge;

import org.bitbucket.ucchy.sl.LockData;
import org.bitbucket.ucchy.sl.LockDataManager;
import org.bitbucket.ucchy.sl.StandLock;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import io.github.rypofalem.armorstandeditor.ArmorStandEditorPlugin;
import io.github.rypofalem.armorstandeditor.protection.ASEProtection;

/**
 * ArmorStand Editor との連携クラス
 * @author ucchy
 */
public class ArmorStandEditorBridge implements ASEProtection {

    /**
     * @param plugin
     */
    public static void registerToArmorStandEditor(Plugin plugin) {

        if ( plugin == null || !(plugin instanceof ArmorStandEditorPlugin) ) {
            return;
        }

        ArmorStandEditorPlugin ase = (ArmorStandEditorPlugin)plugin;
        ase.addProtection(new ArmorStandEditorBridge());
    }

    /**
     * @see io.github.rypofalem.armorstandeditor.protection.ASEProtection#canEdit(org.bukkit.entity.Player, org.bukkit.entity.ArmorStand)
     */
    @Override
    public boolean canEdit(Player player, ArmorStand stand) {

        LockDataManager manager = StandLock.getInstance().getLockDataManager();
        LockData data = manager.getLockDataByArmorStand(stand);

        // ロック情報が無い場合は、権限に従って編集可能かどうかを返す。
        if ( data == null ) {
            return player.hasPermission(StandLock.PERMISSION_ENTITY + ".interact");
        }

        // ロック情報がある場合は、ロック保持者なら編集可能とする。
        return player.getUniqueId().equals(data.getOwnerUuid());
    }

}
