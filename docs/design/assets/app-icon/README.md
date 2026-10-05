# 小猫记帐应用图标

当前采用 `cardtally-app-icon-v6-round-cat.png`：白底、黑白圆脸小猫抱账本，无文字。对应生成提示词见 `cardtally-app-icon-v6-round-cat-prompt.txt`，由内置 imagegen 生成。

原图按原始字节复制到 `app/src/main/res/drawable-nodpi/ic_launcher_artwork.png`，不作密度放大；Android 7 使用 `mipmap-anydpi/ic_launcher.xml` 按图标边界缩放，Android 8 及以上使用 `mipmap-anydpi-v26/ic_launcher.xml` 的自适应图标，白色背景和四边 13% 内缩的图像前景保留裁切空间。Manifest 普通与圆形入口共用 `@mipmap/ic_launcher`。

其他版本是生成过程中的备选稿，未用于应用。应用显示名为“小猫记帐”，中英文环境一致；Debug 保留 `(Dev)` 后缀区分两套安装；内部包名及资源前缀继续使用 CardTally。
