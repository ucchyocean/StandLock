/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.sl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;

/**
 * ロックデータマネージャ
 * @author ucchy
 */
public class LockDataManager {

    /** フラットデータファイルを格納するフォルダ */
    private File dataFolder;

    /** ロックデータの、プレイヤーUUIDをキーとしたマップ */
    private HashMap<UUID, ArrayList<LockData>> idMap;

    /** ロックデータの、ArmorStandのUUIDをキーとしたマップ */
    private HashMap<UUID, LockData> standMap;

    /**
     * コンストラクタ
     * @param dataFolder フラットデータファイルを格納するフォルダ
     */
    public LockDataManager(File dataFolder) {

        this.dataFolder = dataFolder;

        // データフォルダがまだ存在しないなら、ここで作成する
        if ( !dataFolder.exists() ) {
            dataFolder.mkdirs();
        }

        // データのロード
        reloadData();

        // ロードしたデータをセーブ（nullだったHangingをデータに反映するため）
        saveAllData();
    }

    /**
     * データを再読込する
     */
    public void reloadData() {

        // データフォルダに格納されているymlファイルのリストを取得
        File[] files = dataFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        // 全ワールドに存在する全てのArmorStandを取得
        HashMap<String, Collection<ArmorStand>> stands =
                new HashMap<String, Collection<ArmorStand>>();
        for ( World world : Bukkit.getWorlds() ) {
            stands.put(world.getName(), world.getEntitiesByClass(ArmorStand.class));
        }

        // 全てのデータをロード
        idMap = new HashMap<UUID, ArrayList<LockData>>();
        standMap = new HashMap<UUID, LockData>();

        for ( File file : files ) {

            // 後ろの4文字を削って拡張子を抜く
            String key = file.getName().substring(0, file.getName().length() - 4);

            // UUIDへ変換する
            if ( !isUUID(key) ) {
                continue;
            }
            UUID uuid = UUID.fromString(key);

            // データをロードする
            idMap.put(uuid, loadLockData(file, uuid, stands));

            // Hangingマップにも展開する
            for ( LockData ld : idMap.get(uuid) ) {
                standMap.put(ld.getStand().getUniqueId(), ld);
            }
        }
    }

    /**
     * プレイヤーファイルから、ロックデータをロードする
     * @param file プレイヤーファイル
     * @param uuid プレイヤーのUUID（あらかじめ取得したもの）
     * @param stands 全ワールドのArmorStand（あらかじめ取得したもの）
     * @return ロードされたロックデータ
     */
    private ArrayList<LockData> loadLockData(File file, UUID uuid,
            HashMap<String, Collection<ArmorStand>> stands) {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ArrayList<LockData> data = new ArrayList<LockData>();

        for ( String key : config.getKeys(false) ) {

            if ( key.equals("name") ) continue;

            Location location = getLocationFromDescription(key);
            if ( location == null ) {
                continue;
            }

            ArmorStand stand = getArmorStandFromLocation(location, stands);
            long time = config.getLong(key, -1);

            if ( stand == null ) {
                continue;
            }

            data.add(new LockData(uuid, stand, time));
        }

        return data;
    }

    /**
     * 全てのデータを保存する
     */
    public void saveAllData() {
        for ( UUID uuid : idMap.keySet() ) {
            saveData(uuid);
        }
    }

    /**
     * 指定したオーナープレイヤーのデータを保存する
     * @param uuid オーナープレイヤー
     */
    public void saveData(UUID uuid) {

        File file = new File(dataFolder, uuid.toString() + ".yml");

        YamlConfiguration config = new YamlConfiguration();

        config.set("name", Bukkit.getOfflinePlayer(uuid).getName());

        ArrayList<LockData> datas = idMap.get(uuid);
        for ( LockData data : datas ) {
            String desc = getDescriptionFromLocation(data.getLocation());
            config.set(desc, data.getDate());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定されたArmorStandから、ロックデータを取得する
     * @param stand ArmorStand
     * @return ロックデータ
     */
    public LockData getLockDataByArmorStand(ArmorStand stand) {
        return standMap.get(stand.getUniqueId());
    }

    /**
     * ロックデータを追加する
     * @param uuid オーナープレイヤー
     * @param stand ArmorStand
     */
    public void addLockData(UUID uuid, ArmorStand stand) {

        // 既にロックデータが存在する場合は、古いデータを削除する
        if ( standMap.containsKey(stand.getUniqueId()) ) {
            removeLockData(stand);
        }

        // オーナープレイヤーのデータが無いなら新規作成する
        if ( !idMap.containsKey(uuid) ) {
            idMap.put(uuid, new ArrayList<LockData>());
        }

        // ロックデータ追加
        LockData data = new LockData(uuid, stand, System.currentTimeMillis());
        idMap.get(uuid).add(data);
        standMap.put(stand.getUniqueId(), data);

        // データを保存
        saveData(uuid);
    }

    /**
     * ロックデータを削除する
     * @param stand 削除するArmorStand
     */
    public void removeLockData(ArmorStand stand) {

        // 既にロックデータが無い場合は、何もしない
        if ( !standMap.containsKey(stand.getUniqueId()) ) {
            return;
        }

        LockData ld = standMap.get(stand.getUniqueId());

        // 削除を実行
        standMap.remove(stand.getUniqueId());
        if ( idMap.containsKey(ld.getOwnerUuid()) ) {
            idMap.get(ld.getOwnerUuid()).remove(ld);
        }

        // データを保存
        saveData(ld.getOwnerUuid());
    }

    /**
     * 指定したプレイヤーのロック数を返す
     * @param uuid プレイヤー
     * @return ロック数
     */
    public int getPlayerLockNum(UUID uuid) {

        if ( idMap.containsKey(uuid) ) {
            return idMap.get(uuid).size();
        }
        return 0;
    }


    /**
     * 指定された場所に存在するHangingを取得する
     * @param location 場所
     * @param stands 全ワールドのArmorStand（あらかじめ取得したもの）
     * @return ArmorStand、指定した場所に存在しなければnull
     */
    private ArmorStand getArmorStandFromLocation(Location location,
            HashMap<String, Collection<ArmorStand>> stands) {

        if ( !stands.containsKey(location.getWorld().getName()) ) {
            return null;
        }

        for ( ArmorStand stand : stands.get(location.getWorld().getName()) ) {
            if ( location.getBlockX() == stand.getLocation().getBlockX() &&
                    location.getBlockY() == stand.getLocation().getBlockY() &&
                    location.getBlockZ() == stand.getLocation().getBlockZ() ) {
                return stand;
            }
        }
        return null;
    }

    /**
     * Locationを文字列に変換する
     * @param location Location
     * @return 変換後の文字列
     */
    private static String getDescriptionFromLocation(Location location) {

        return String.format("%s_%d_%d_%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ() );
    }

    /**
     * 文字列をLocationに変換する
     * @param description 文字列
     * @return Location、変換に失敗した場合はnull
     */
    private static Location getLocationFromDescription(String description) {

        String[] temp = description.split("_");
        if ( temp.length < 4 ) {
            return null;
        }

        int offset = temp.length - 4;
        String temp_x = temp[offset + 1];
        String temp_y = temp[offset + 2];
        String temp_z = temp[offset + 3];
        if ( !isDigit(temp_x) || !isDigit(temp_y) || !isDigit(temp_z) ) {
            return null;
        }
        int x = Integer.parseInt(temp_x);
        int y = Integer.parseInt(temp_y);
        int z = Integer.parseInt(temp_z);

        String suffix = temp_x + "_" + temp_y + "_" + temp_z;
        String wname = description.substring(0, description.lastIndexOf(suffix) - 1);
        World world = Bukkit.getWorld(wname);
        if ( world == null ) {
            return null;
        }

        return new Location(world, x, y, z);
    }

    /**
     * 文字列がUUIDかどうかを判定する
     * @param source 文字列
     * @return UUIDかどうか
     */
    private static boolean isUUID(String source) {
        return source.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    /**
     * 文字列が整数値に変換可能かどうかを判定する
     * @param source 変換対象の文字列
     * @return 整数に変換可能かどうか
     */
    private static boolean isDigit(String source) {
        return source.matches("^-?[0-9]{1,9}$");
    }
}
