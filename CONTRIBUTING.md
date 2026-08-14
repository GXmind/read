# 悦读分支与发布流程

## 长期分支

- `main`：只保存已经正式释放的生产版本。
- `develop`：下一版本的集成分支，日常开发的合并目标。

## 开发分支

每次修改必须从最新 `develop` 创建独立分支：

- 新功能：`feature/<简短名称>`
- 修复：`fix/<简短名称>`

修改完成后先在分支执行：

```powershell
./gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

通过后合并到 `develop`。不得直接在 `main` 开发。

## Release 分支

1. 从测试通过的 `develop` 创建 `release/vX.Y.Z`。
2. 只允许版本号、发布说明和阻断性问题修复进入 Release 分支。
3. 执行 Debug、混淆 Release、Lint 和测试质量门禁。
4. 测试通过后将 Release 分支合并到 `main`。
5. 在 `main` 的合并提交上创建带说明的 `vX.Y.Z` 标签并推送。
6. GitHub Actions 使用生产密钥和轮换链构建最终 APK，并创建 GitHub Release。
7. 发布工作流自动把同一发布提交合并回 `develop`，确保开发线同步。

最终对外 APK 只能来自 GitHub Release，不允许使用 Debug APK 或开发分支产物。

## 版本规则

- 每个公开版本必须同时增加 `versionCode` 并更新 `versionName`。
- Git 标签必须严格等于 `v` + `versionName`。
- 不得覆盖、删除或重新使用已经发布的标签。
- 生产 JKS、旧版迁移 JKS、密码、R8 mapping 和 GitHub Token 禁止提交。
