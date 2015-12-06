/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.sl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;

/**
 * スタンドを扱うユーティリティクラス
 * @author ucchy
 */
public class StandUtility {

    /**
     * 指定された地点にあるArmorStandを取得する
     * @param location 地点
     * @return Hanging、無かった場合はnull
     */
    public static ArmorStand getArmorstandFromLocation(Location location) {

        World world = location.getWorld();
        for ( ArmorStand stand : world.getEntitiesByClass(ArmorStand.class) ) {
            if ( isSameLocation(location, stand.getLocation()) ) {
                return stand;
            }
        }
        return null;
    }

    /**
     * 2つのLocationが同じブロックかどうかを確認する
     * @param loc1
     * @param loc2
     * @return
     */
    private static boolean isSameLocation(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }
}
