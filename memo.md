- スタンドの配置
PlayerInteractEventをつかんで、
<pre>
(event.getItem().getType() == Material.ARMOR_STAND) + " : " + (event.getAction() == Action.RIGHT_CLICK_BLOCK)
</pre>
ただし、ArmorStandは直接取得できないので、設置したプレイヤーの近隣でエンティティを検索する必要がある
（もしかしたら、遅延が必要かもしれない）。
キャンセルは工夫しないとちょっと難しいかもしれない。

- スタンドの破壊
EntityDamageByEntityEventをつかんで、
<pre>
event.getEntityType() == ARMOR_STAND
</pre>
event.getDamager() で破壊者が取得できる。Projectileの人は、投げた人を取得する必要あり。
event.getEntity() で破壊されたスタンドが取得できる。
キャンセルはそのまま可能。

- スタンドの操作
PlayerArmorStandManipulateEventでそのままできる。

