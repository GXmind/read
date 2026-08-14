# 悦读安全发布说明

## 更新信任模型

客户端只读取 `GXmind/read` 的 GitHub Releases，并执行以下校验：

1. 所有请求必须使用 HTTPS，且每一次重定向的主机都在硬编码白名单内。
2. 下载限制为 300 MB；若 GitHub API 提供 SHA-256 digest，必须完全匹配。
3. APK 的包名必须为 `com.codex.yuedu`，版本号必须高于已安装版本。
4. APK 当前签名或有效签名轮换历史必须包含已安装应用的证书。
5. 最终安装仍由 Android 系统安装器完成，应用不能静默安装。

## GitHub Secrets

仓库不得提交 `.jks`、密码、`local.properties` 或任何私钥。发布工作流需要：

- `SIGNING_KEY_BASE64`：生产 JKS 的 Base64 编码。
- `LEGACY_SIGNING_KEY_BASE64`：仅用于兼容既有安装的旧证书 JKS Base64；Android 9 以上通过轮换链迁移到生产证书。
- `KEYSTORE_PASSWORD`：JKS 密码。
- `KEY_PASSWORD`：`yuedu-release` 别名的密钥密码。

`signing/yuedu-lineage.bin` 只包含证书与旧证书对新证书的授权证明，不包含任何私钥，可以提交到仓库。Android 8/8.1 仍使用旧证书兼容安装；Android 9 以上使用轮换后的生产证书。

## 防逆向边界

Release 构建启用 R8 代码压缩、优化、重打包、标识符混淆、资源收缩和源文件名替换。生产版本还检查自身签名证书。上述措施只能提高逆向和篡改成本，不能从理论上阻止反编译；因此任何服务器私钥、签名私钥或长期访问令牌都不能放入客户端。
