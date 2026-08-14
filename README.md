# 悦读 Android 阅读器

一款以本地阅读为核心的 Android 文档阅读器；仅在用户主动检查更新时访问官方 GitHub Releases。

## 功能

- TXT（UTF-8 / GB18030）小说导入与章节识别
- EPUB 目录与正文解析
- DOCX Word 文档按“第X章 / Chapter X”等标题自动分章
- PDF 单页渲染阅读
- 左右滑动或点击两侧翻页
- 目录跳转、书签、逐页笔记
- 独立“我的笔记”中心：跨书汇总、章节/页码/原文摘要、编辑、确认删除、跳回原文
- 兼容 1.3.0 及更早版本的逐页笔记数据，首次读取时无损升级
- 自动保存每本书最后阅读位置
- 根据屏幕实际尺寸精确分页，每个章节强制从新页开始
- 阅读页左上角显示当前章节，支持字号与行距调整
- 全局搜索书名及 TXT、EPUB、DOCX 正文并跳转到命中位置
- 从 `GXmind/read` GitHub Releases 安全检查更新，验证 HTTPS、SHA-256、包名、版本号与 APK 签名轮换链
- 对比当前安装版本与远端版本，已经是最新版时明确提示并隐藏下载入口

## 安全发布

Release 构建启用 R8 混淆、优化、代码重打包、资源收缩和应用自身签名校验。发布流程及 GitHub Secrets 配置请参阅 `SECURITY.md`。生产或旧版迁移私钥禁止提交到仓库。

项目采用 `develop` → `feature/*` / `fix/*` → `release/vX.Y.Z` → `main` 的发布流程。详细质量门禁、打标签和 develop 回同步规则见 `CONTRIBUTING.md`。
- Android 系统文档扫描/选择，持久保留访问权限

## 构建

使用 Android Studio 打开本目录，等待 Gradle 同步后运行 `app`。最低 Android 8.0，目标 Android 15。

> 当前“扫描”指扫描手机存储并选择文档，不包含纸质书相机 OCR。
