package org.bitbucket.ucchy.sl;

/**
 * 壁が壊された時の対応モード
 * @author ucchy
 */
public enum WallMode {

    /** 再生モード。
     * 石で壁が再生します。ロック情報は維持されます。 */
    REGEN_STONE,

    /** 消滅モード。
     * 壁は再生せず、額縁および額縁の中身が消滅し、ロック情報も削除されます。 */
    EXTINCTION,

    /** ドロップモード。
     * バニラ挙動と同様。アイテムとしてドロップします。ロック情報は削除されます。 */
    ITEM_DROP;

    /**
     * 文字列からWallModeを取得する
     * @param id 文字列
     * @param def デフォルト
     * @return WallMode
     */
    public static WallMode fromString(String id, WallMode def) {

        if ( id == null ) return def;
        for ( WallMode mode : values() ) {
            if ( mode.toString().equals(id.toUpperCase()) ) {
                return mode;
            }
        }
        return def;
    }
}
