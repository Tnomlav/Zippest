# 在 WSL 里开发与构建 Zippest

项目本身与操作系统无关：CI（GitHub Actions）一直在 `ubuntu-latest` + JDK 17 上构建、签名、校验并发布，所以 WSL 里的目标只是把同一条链路在本地跑通。

## 一次性准备（不需要 root）

JDK 17（Temurin 压缩包）：

```bash
mkdir -p ~/.local/java
curl -sSL -o /tmp/jdk17.tgz \
  "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar -xzf /tmp/jdk17.tgz -C ~/.local/java
export JAVA_HOME="$(ls -d ~/.local/java/jdk-17*)"
```

Android SDK（命令行工具 + 编译所需组件）：

```bash
mkdir -p ~/Android/Sdk/cmdline-tools
curl -sSL -o /tmp/ct.zip \
  https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
python3 -m zipfile -e /tmp/ct.zip /tmp/ct
mv /tmp/ct/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
chmod +x ~/Android/Sdk/cmdline-tools/latest/bin/*

export ANDROID_HOME="$HOME/Android/Sdk"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
```

- AGP 9 的平台包按小版本命名，`compileSdk = 37` 对应 **`platforms;android-37.0`**，不是 `android-37`
- 没有 `unzip` 时用 `python3 -m zipfile -e` 解压，解压后要 `chmod +x` 命令行工具

## 构建

```bash
cd android
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
./gradlew assembleRelease
./gradlew -PzippestBundle bundleRelease
./gradlew testDebugUnitTest
```

`android/local.properties` 是机器相关文件、不纳入 Git。同一个仓库被 Windows 和 WSL 共用时，它会互相覆盖——默认内容指向 Windows SDK，WSL 构建前要改成本机路径（上面这一行），Windows 侧再构建时改回去（Android Studio 打开项目会自动重写）。

走代理时，Java 不读 `https_proxy` 环境变量，需要：

```bash
export JAVA_TOOL_OPTIONS="-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890 \
  -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890"
```

## 发版

```bash
bash tools/release.sh              # 构建 -> 归档 -> SHA256SUMS -> 打 tag -> push
bash tools/release.sh --skip-build # 用已有产物归档并打 tag
```

等价于 Windows 上的 `tools/release.ps1`；推送 tag 后由 CI 构建并发布 Release。

## Git 认证

WSL 里的 git 用不到 Windows 的凭据管理器，第一次 `git push` 会报
`could not read Username for 'https://github.com'`。用 SSH 最省事：

```bash
ssh-keygen -t ed25519 -C "wsl@$(hostname)"      # 已生成过就跳过
cat ~/.ssh/id_ed25519.pub                        # 贴到 GitHub → Settings → SSH and GPG keys
ssh-keyscan -t ed25519 github.com >> ~/.ssh/known_hosts
git config --global url."git@github.com:".insteadOf "https://github.com/"
```

最后一行只影响 WSL（`~/.gitconfig`），Windows 侧继续用 https + 凭据管理器；仓库里的
`origin` 保持 https 不变。撤销：`git config --global --unset url."git@github.com:".insteadOf`。

## 真机调试

WSL 看不到手机的 USB 设备（没有 `/dev/bus/usb`），USB 调试在 Windows 侧，WSL 里的 adb 用不到。两条路：

**无线调试（推荐）**——手机开「无线调试」就够了，不用每次给端口：mDNS 会发现设备，`adb devices` 自动出现 `adb-XXXX._adb-tls-connect._tcp device`。端口每次重开都会变，所以别硬编码；上次配过对就不用再配对。

```bash
adb mdns services                                  # 看当前服务与端口
adb devices                                         # 自动连上的设备直接可用
adb -s adb-XXXX._adb-tls-connect._tcp shell ...     # 多个设备时用 serial
```

注意：手机「无线调试」页显示的 IP 可能是 VPN/热点网段（例如 `172.19.0.1`），WSL 要连的是 mDNS 报出来的局域网地址（例如 `192.168.3.99`）。

**USB 经 Windows adb 转发**——维持 USB 调试不动，Windows 把 adb 服务开给局域网，WSL 连过去：

```powershell
adb -a -P 5037 nodaemon server
```

```bash
export ADB_SERVER_SOCKET=tcp:<Windows 局域网 IP>:5037
adb devices
```

Windows 那边的 adb 一直连着手机时，`ADB_SERVER_SOCKET` 指向它、然后 `adb connect <手机>:<端口>` 即可。前提是 Windows 侧的 adb 与手机仍是 USB 连接。

## 已知限制

- **性能**：仓库在 `/mnt/c`（drvfs）上，全量构建明显慢于 Windows 本机或 ext4。长期在 WSL 构建建议把仓库放到 `~/projects/`。
- **真机调试**：WSL 默认看不到 USB 设备，`adb` 需要 `adb connect <手机IP>` 走网络，或用 usbipd-win 转发。
- **`tools/*.mjs`**：截图、设备测试和 Open Design 相关脚本写死了 Windows 路径（`C:\Program Files\Google\Chrome\...`、`C:\Open Design\...`），只能在 Windows 侧用 Windows 的 node 运行。
- **`tools/release.ps1`**：需要 `pwsh` 7+；在 WSL 里优先用 `tools/release.sh`。

## 换行符

仓库统一 LF（见 `.gitattributes`），只有 `*.bat` 保持 CRLF。WSL 里不要设置 `core.autocrlf=true`。
