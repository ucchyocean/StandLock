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
import org.bukkit.inventory.ItemStack;
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

        final int pre = event.getItem().getAmount();
        final ItemStack item = event.getItem();
        final Player player = event.getPlayer();

        // ここから、1tick後のスタンド設置後の処理を行う。
        new BukkitRunnable() {
            public void run() {

                // アイテムの量が減っていないなら、スタンドは設置されていないので何もしない。
                if ( pre == item.getAmount() ) {
                    return;
                }

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

                // スタンドを浮遊させる。
                // TODO: 要検討。浮遊させるか、横からの設置を禁止するか。
                stand.setGravity(false);

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

                    // 新しいロックデータを登録する
                    lockManager.addLockData(player.getUniqueId(), stand);
                    player.sendMessage(Messages.get("Locked"));
                }

            }
        }.runTaskLater(StandLock.getInstance(), 1);
    }
//
//    /**
//     * かべかけ物が破壊された時に呼び出されるイベント
//     * @param event
//     */
//    @EventHandler(priority=EventPriority.HIGHEST)
//    public void onHangingBreak(HangingBreakEvent event) {
//
//        Hanging hanging = event.getEntity();
//
//        // 額縁でなければ無視する
//        if ( !(hanging instanceof ItemFrame) ) {
//            return;
//        }
//
//        // エンティティによる破壊なら、HangingBreakByEntityEventで処理するので、
//        // ここでは何もしない。
//        if ( event instanceof HangingBreakByEntityEvent ) {
//            return;
//        }
//
//        // 対象物のロックデータを取得する
//        LockData ld = lockManager.getLockDataByHanging(hanging);
//
//        // ロックデータが無い場合はイベントを無視する
//        if ( ld == null ) {
//            return;
//        }
//
//        switch (event.getCause()) {
//
//        case ENTITY:
//            // Entityにより破壊された場合。
//            // HangingBreakByEntityEventの方で処理するので、ここでは何もしない
//            break;
//
//        case EXPLOSION:
//            // 爆破により破壊された場合。
//            // イベントをキャンセルして復元させる。
//            // ただし、かけられていた壁も爆破で消滅していた場合は、
//            // しばらくした後に（100ticks後くらい）同じイベントがPHYSICSで呼び出される。
//            event.setCancelled(true);
//            break;
//
//        case OBSTRUCTION:
//            // 額縁のある場所が、ブロックに塞がれた場合。
//            // CB1.7.10-R0.1では、Hangingの設置方向によっては、
//            // なぜかOBSTRUCTIONではなくPHYSICSになる（不具合？）。
//
//            // hangingにかぶさる位置のブロックをAIRにし、イベントをキャンセルする
//            Block obst = hanging.getLocation().getBlock();
//            obst.setType(Material.AIR);
//
//            event.setCancelled(true);
//            break;
//
//        case PHYSICS:
//            // 額縁のかかっている壁のブロックが無くなったり、
//            // 壁掛け物として不自然な状態になったりしたとき、
//            // ワールドのPhysicsUpdate処理で剥がされた場合。
//            // fall through
//        case DEFAULT:
//            // 破壊された原因が不明な場合。
//            // 他のプラグインなどで、Hangingが強制除去された場合などに発生する。
//            // default: のところでまとめて対応する。
//            // fall through
//        default:
//            // 破壊原因が不明な場合。
//
//            // PHYSICS、DEFAULTは、ここでまとめて処理する。
//
//            // Hangingにかぶっているブロックがある場合は、AIRにして消滅させる。
//            obst = hanging.getLocation().getBlock();
//            obst.setType(Material.AIR);
//
//            if ( config.getWallMode() == WallMode.REGEN_STONE ) {
//                // 設置されていたであろう壁の方向に石を作って、壁を復活させる。
//                // イベントをキャンセルする。
//                // ロック情報はそのままにする。
//                Block wall = obst.getRelative(hanging.getAttachedFace());
//                if ( wall.getType() == Material.AIR || wall.isLiquid() ) {
//                    wall.setType(Material.STONE);
//                }
//                event.setCancelled(true);
//
//            } else if ( config.getWallMode() == WallMode.EXTINCTION ) {
//                // hangingエンティティを消去して、ドロップしないようにする。
//                // イベントをキャンセルする。
//                // ロック情報を削除する。
//                lockManager.removeLockData(hanging);
//                hanging.remove();
//                event.setCancelled(true);
//
//            } else if ( config.getWallMode() == WallMode.ITEM_DROP ) {
//                // バニラ挙動と同様。つまり何もしない。
//                // ロック情報の削除はする。
//                lockManager.removeLockData(hanging);
//
//            }
//
//            break;
//        }
//    }
//
//    /**
//     * かべかけ物がエンティティに破壊された時に呼び出されるイベント
//     * @param event
//     */
//    @EventHandler(priority=EventPriority.HIGHEST)
//    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
//
//        Hanging hanging = event.getEntity();
//
//        // 額縁でなければ無視する
//        if ( !(hanging instanceof ItemFrame) ) {
//            return;
//        }
//
//        // 事前コマンドが実行されている場合の処理
//        if ( event.getRemover() instanceof Player ) {
//            Player damager = (Player)event.getRemover();
//            if ( processPrecommand(damager, hanging) ) {
//                event.setCancelled(true);
//                return;
//            }
//        }
//
//        // ==== 以下、額縁に対する攻撃の保護処理 ====
//
//        // 対象物のロックデータを取得する
//        LockData ld = lockManager.getLockDataByHanging(hanging);
//
//        // ロックデータが無い場合
//        if ( ld == null ) {
//
//            // 権限がなければ、操作を禁止する
//            if ( event.getRemover() instanceof Player ) {
//                Player remover = (Player)event.getRemover();
//                if ( !remover.hasPermission(PERMISSION + ".break") ) {
//                    remover.sendMessage(Messages.get("PermissionDeniedBreak"));
//                    event.setCancelled(true);
//                }
//            }
//
//            return; // ロックデータが無い場合は、ここで処理終了。
//        }
//
//        // 操作者取得
//        Player remover = null;
//        if ( event.getRemover() instanceof Player ) {
//            remover = (Player)event.getRemover();
//        } else if ( event.getRemover() instanceof Projectile ) {
//            if ( ((Projectile)event.getRemover()).getShooter() instanceof Player ) {
//                remover = (Player)((Projectile)event.getRemover()).getShooter();
//            }
//        }
//
//        // 所有者でなくて、管理者でもなければ、操作を禁止する
//        if ( remover == null ||
//                (!ld.getOwnerUuid().equals(remover.getUniqueId()) &&
//                 !remover.hasPermission(PERMISSION + ".admin") ) ) {
//            event.setCancelled(true);
//            if ( remover != null ) {
//                remover.sendMessage(Messages.get("ItemFrameLocked"));
//            }
//            return;
//        }
//
//        // ロック情報を削除する
//        lockManager.removeLockData(hanging);
//
//        // メッセージを出す
//        remover.sendMessage(Messages.get("LockRemoved"));
//    }


    /**
     * ピストンが伸びたときのイベント
     * @param event
     */
    @EventHandler
    public void onBlockPistonExtended(BlockPistonExtendEvent event) {

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

        ArmorStand stand = (ArmorStand)event.getEntity();

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

        // ロックデータが無いなら何もしない
        if ( ld == null ) {
            return;
        }

        // 攻撃者取得
        Player damager = null;
        if ( event.getDamager() instanceof Player ) {
            damager = (Player)event.getDamager();
        } else if ( event.getDamager() instanceof Projectile ) {
            if ( ((Projectile)event.getDamager()).getShooter() instanceof Player ) {
                damager = (Player)((Projectile)event.getDamager()).getShooter();
            }
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

        // ロックを解除する
        lockManager.removeLockData(stand);
        damager.sendMessage(Messages.get("LockRemoved"));
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
