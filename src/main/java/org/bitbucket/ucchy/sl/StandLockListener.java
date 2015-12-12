/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.sl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * StandLockのリスナークラス
 * @author ucchy
 */
public class StandLockListener implements Listener {

    private static final String PERMISSION = "standlock.entity";
    private static final String PERMISSION_INFINITE_PLACE =
            "standlock.entity.infinite-place";

    private StandLock parent;
    private LockDataManager lockManager;
    private StandLockConfig config;

    /**
     * コンストラクタ
     * @param parent
     */
    public StandLockListener(StandLock parent) {
        this.parent = parent;
        this.lockManager = parent.getLockDataManager();
        this.config = parent.getStandLockConfig();
    }

    /**
     * プレイヤーがクリックした時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {

        // アーマースタンドの設置でなければ無視する
        if ( event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getItem() == null
                || event.getItem().getType() != Material.ARMOR_STAND ) {
            return;
        }

        // 本プラグイン導入後は、同じ位置へのArmorStandの設置を認めないため、
        // 壁の横をクリックして配置しようとした場合は、イベントをキャンセルする。
        // TODO: 要検討。浮遊させるか、横からの設置を禁止するか。
//        if ( event.getBlockFace() != BlockFace.UP ) {
//            event.setCancelled(true);
//            return;
//        }

        // 本プラグイン導入後は、同じ位置へのArmorStandの設置を認めないため、
        // 同じLocationを持つスタンドが既にあるなら、イベントをキャンセルする。
        final Location location =
                event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();
        if ( StandUtility.getArmorstandFromLocation(location) != null ) {
            event.setCancelled(true);
            return;
        }

        final Player player = event.getPlayer();

        // ここから、1tick後のスタンド設置後の処理を行う。
        new BukkitRunnable() {
            public void run() {

                // 設置されたスタンドを取得。
                ArmorStand stand = StandUtility.getArmorstandFromLocation(location);
                if ( stand == null ) {
                    return;
                }

                // 権限がなければ、操作を禁止する
                if ( !player.hasPermission(PERMISSION + ".place") ) {
                    player.sendMessage(Messages.get("PermissionDeniedPlace"));
                    stand.remove();
                    return;
                }

                if ( config.isAutoLock() ) {
                    // 自動ロック処理

                    // 設置数制限を超える場合は、設置を許可しない。
                    if ( config.getArmorStandLimit() >= 0 &&
                            !player.hasPermission(PERMISSION_INFINITE_PLACE) &&
                            lockManager.getPlayerLockNum(player.getUniqueId()) >=
                                config.getArmorStandLimit() ) {
                        player.sendMessage(Messages.get("ExceedLockLimit"));
                        stand.remove();
                        return;
                    }

                    // スタンドを浮遊させる。
                    // TODO: 要検討。浮遊させるか、横からの設置を禁止するか。
                    stand.setGravity(false);

                    // 新しいロックデータを登録する
                    lockManager.addLockData(player.getUniqueId(), stand);
                    player.sendMessage(Messages.get("Locked"));
                }

            }
        }.runTaskLater(StandLock.getInstance(), 1);
    }

    /**
     * ピストンが伸びたときのイベント
     * @param event
     */
    @EventHandler
    public void onBlockPistonExtended(BlockPistonExtendEvent event) {

        // ピストンが伸びた先にスタンドがあるなら、イベントをキャンセルする。
        Location extLoc = event.getBlock().getRelative(event.getDirection()).getLocation();
        if (  StandUtility.getArmorstandFromLocation(extLoc) != null ) {
            event.setCancelled(true);
            return;
        }

        // 動いたブロックのところにスタンドがあるなら、イベントをキャンセルする。
        for ( Block block : event.getBlocks() ) {
            Location location = block.getRelative(event.getDirection()).getLocation();
            ArmorStand stand = StandUtility.getArmorstandFromLocation(location);
            if ( stand != null && lockManager.getLockDataByArmorStand(stand) != null ) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * ピストンが縮んだときのイベント
     * @param event
     */
    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {

        // 動いたブロックのところにスタンドがあるなら、イベントをキャンセルする。
        for ( Block block : event.getBlocks() ) {
            Location location = block.getRelative(event.getDirection()).getLocation();
            ArmorStand stand = StandUtility.getArmorstandFromLocation(location);
            if ( stand != null && lockManager.getLockDataByArmorStand(stand) != null ) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * プレイヤーがスタンドを操作した時のイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {

        // MEMO: このイベントハンドラは、
        //   スタンドにアイテムを入れたり出したりした時に呼び出されるので、
        //   所有者を確認して、所有者でなければメッセージを表示して操作をキャンセルする。

        // ロックデータ取得
        ArmorStand stand = event.getRightClicked();
        LockData ld = lockManager.getLockDataByArmorStand(stand);

        if ( ld != null && !ld.getOwnerUuid().equals(event.getPlayer().getUniqueId()) &&
                !event.getPlayer().hasPermission(PERMISSION + ".admin") ) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Messages.get("ArmorStandLocked"));
            return;
        }
    }

    /**
     * エンティティがエンティティからダメージを受けた時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        // ダメージ対象がArmorStandでなければ、イベントを無視する。
        if ( event.getEntity().getType() != EntityType.ARMOR_STAND ) {
            return;
        }

        final ArmorStand stand = (ArmorStand)event.getEntity();

        // 事前コマンドが実行されている場合の処理
        if ( event.getDamager() instanceof Player ) {
            Player damager = (Player)event.getDamager();
            if ( processPrecommand(damager, stand) ) {
                event.setCancelled(true);
                return;
            }
        }

        // ==== 以下、スタンドに対する攻撃の保護処理 ====

        // ロックデータ取得
        LockData ld = lockManager.getLockDataByArmorStand(stand);

        // 攻撃者取得
        Player damager = null;
        if ( event.getDamager() instanceof Player ) {
            damager = (Player)event.getDamager();
        } else if ( event.getDamager() instanceof Projectile ) {
            if ( ((Projectile)event.getDamager()).getShooter() instanceof Player ) {
                damager = (Player)((Projectile)event.getDamager()).getShooter();
            }
        }

        // ロックデータが無い場合
        if ( ld == null ) {

            // 権限がなければ、操作を禁止する
            if ( damager == null || !damager.hasPermission(PERMISSION + ".break") ) {
                if ( damager != null ) {
                    damager.sendMessage(Messages.get("PermissionDeniedBreak"));
                }
                event.setCancelled(true);
                return;
            }

            return; // ロックデータが無い場合はここで終わり。
        }

        // 所有者でなくて、管理者でもなければ、操作を禁止する
        if ( damager == null || (!ld.getOwnerUuid().equals(damager.getUniqueId()) &&
                !damager.hasPermission(PERMISSION + ".admin") ) ) {
            event.setCancelled(true);
            if ( damager != null ) {
                damager.sendMessage(Messages.get("ArmorStandLocked"));
            }
            return;
        }

        final Player player = damager;

        // 1tick後に処理する
        new BukkitRunnable() {
            public void run() {

                // スタンドが消去されたなら、ロックを解除しておく。
                if ( stand.isDead() ) {
                    lockManager.removeLockData(stand);
                    if ( player != null ) {
                        player.sendMessage(Messages.get("LockRemoved"));
                    }
                }
            }
        }.runTaskLater(StandLock.getInstance(), 1);
    }

    /**
     * 事前実行されたコマンドを処理する
     * @param player 実行したプレイヤー
     * @param stand 実行対象のスタンド
     * @return コマンド実行したかどうか
     */
    private boolean processPrecommand(Player player, ArmorStand stand) {

        if ( player.hasMetadata(StandLockCommand.META_INFO_COMMAND) ) {

            if ( !player.hasMetadata(StandLockCommand.META_PERSIST_MODE) ) {
                player.removeMetadata(StandLockCommand.META_INFO_COMMAND, parent);
            }

            // ロックデータ取得
            LockData ld = lockManager.getLockDataByArmorStand(stand);

            if ( ld == null ) {
                player.sendMessage(Messages.get("ArmorStandUnlocked"));
                return true;
            } else {
                String owner;
                if ( ld.getOwner() == null ) {
                    owner = Messages.get("UnknownUUID");
                } else {
                    owner = ld.getOwner().getName();
                }
                player.sendMessage(Messages.getMessageWithKeywords(
                        "InformationOwner",
                        new String[]{"%player", "%uuid"},
                        new String[]{owner, ld.getOwnerUuid().toString()}));
                return true;
            }

        } else if ( player.hasMetadata(StandLockCommand.META_PRIVATE_COMMAND) ) {

            if ( !player.hasMetadata(StandLockCommand.META_PERSIST_MODE) ) {
                player.removeMetadata(StandLockCommand.META_PRIVATE_COMMAND, parent);
            }

            // ロックデータ取得
            LockData ld = lockManager.getLockDataByArmorStand(stand);

            if ( ld == null ) {

                // 設置数制限を超える場合は、設置を許可しない。
                if ( config.getArmorStandLimit() >= 0 &&
                        !player.hasPermission(PERMISSION_INFINITE_PLACE) &&
                        lockManager.getPlayerLockNum(player.getUniqueId()) >=
                            config.getArmorStandLimit() ) {
                    player.sendMessage(Messages.get("ExceedLockLimit"));
                    return true;
                }

                // スタンドを浮遊させる
                // TODO: 要検討。浮遊させるか、横からの設置を禁止するか。
                stand.setGravity(false);

                // 新しいロックデータを登録する
                lockManager.addLockData(player.getUniqueId(), stand);
                player.sendMessage(Messages.get("Locked"));
                return true;

            } else {
                player.sendMessage(Messages.get("ArmorStandAlreadyLocked"));
                return true;
            }

        } else if ( player.hasMetadata(StandLockCommand.META_REMOVE_COMMAND) ) {

            if ( !player.hasMetadata(StandLockCommand.META_PERSIST_MODE) ) {
                player.removeMetadata(StandLockCommand.META_REMOVE_COMMAND, parent);
            }

            // ロックデータ取得
            LockData ld = lockManager.getLockDataByArmorStand(stand);

            if ( ld == null ) {
                player.sendMessage(Messages.get("ArmorStandUnlocked"));
                return true;

            } else {
                // Adminではなくて、かつ、クリックした人のスタンドでないなら、操作を禁止する
                if ( !player.hasPermission(PERMISSION + ".admin") &&
                        !ld.getOwnerUuid().equals(player.getUniqueId()) ) {
                    player.sendMessage(Messages.get("ArmorStandNotOwner"));
                    return true;
                }

                // ロックデータを削除する
                lockManager.removeLockData(stand);
                player.sendMessage(Messages.get("LockRemoved"));
                return true;

            }
        }

        return false;
    }
}
