# GitHub Release 与应用内更新说明

## 1) GitHub Secrets 配置

在仓库 `Settings -> Secrets and variables -> Actions` 中新增：

- `SIGNING_KEYSTORE_BASE64`: keystore 的 base64 内容
- `SIGNING_STORE_PASSWORD`: keystore 密码
- `SIGNING_KEY_ALIAS`: 签名别名
- `SIGNING_KEY_PASSWORD`: 签名私钥密码

生成 base64 示例（Windows Git Bash）：

```bash
base64 -w 0 your-release.keystore > keystore.base64.txt
```

## 2) 发布流程

1. 更新 `app/build.gradle.kts` 中 `versionCode`/`versionName`（如 `1.0.3`）。
2. 提交代码并打 tag：

```bash
git tag -a v1.0.3 -m "release: v1.0.3"
git push origin v1.0.3
```

3. GitHub Actions 工作流 `.github/workflows/android-release.yml` 自动执行：
   - 解码 keystore
   - 生成 `keystore.properties`
   - 构建 `:app:assembleRelease`
   - 上传 APK 到 GitHub Release

## 3) 应用内更新逻辑

- 客户端调用 `GET /repos/{owner}/{repo}/releases/latest` 检查最新版本。
- 使用本地 `versionName` 对比 release 的 `tag_name`（去 `v` 前缀）。
- 若有新版本，显示“下载并安装”按钮。
- 下载完成后通过 `FileProvider` 拉起系统安装器。

## 4) 验证清单

1. 在旧版本设备中打开“设置 -> 应用更新”。
2. 点击“检查更新”，确认能发现新版本。
3. 点击“下载并安装”，观察下载进度。
4. 完成安装后重新打开应用，确认版本号已更新。

## 5) 常见问题

- 若提示无法安装，请在系统设置中允许本应用“安装未知来源应用”。
- 若检查更新失败，确认网络可访问 `api.github.com` 与 Release 页面。
- 若 Actions 失败，优先检查四个签名 Secrets 是否完整且有效。
