#!/usr/bin/env bash
# 一键发版（WSL / Linux 版，对应 tools/release.ps1）：
# 构建 -> 归档产物与校验和 -> 打 tag -> 推送（tag 会触发 CI 自动发 Release）。
# 用法：tools/release.sh [--skip-build] [--skip-push]
# 前提：JDK 17+、Android SDK（ANDROID_HOME 或 android/local.properties），
#       工作区干净（仅允许 design/ 下未跟踪图片），当前分支为 main。
set -euo pipefail

SKIP_BUILD=0
SKIP_PUSH=0
for arg in "$@"; do
    case "$arg" in
        --skip-build) SKIP_BUILD=1 ;;
        --skip-push)  SKIP_PUSH=1 ;;
        -h|--help)    sed -n '2,7p' "$0"; exit 0 ;;
        *)            echo "未知参数：$arg" >&2; exit 2 ;;
    esac
done

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

version="$(grep -oP 'versionName\s*=\s*"\K[^"]+' android/app/build.gradle.kts)"
[ -n "$version" ] || { echo "无法从 build.gradle.kts 读取 versionName" >&2; exit 1; }
tag="v$version"

if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
    echo "tag $tag 已存在；先提升 versionName 再发版" >&2
    exit 1
fi

branch="$(git rev-parse --abbrev-ref HEAD)"
[ "$branch" = "main" ] || { echo "当前分支是 $branch，切回 main 再发版" >&2; exit 1; }

dirty="$(git status --porcelain | grep -v '^?? design/' || true)"
[ -z "$dirty" ] || { printf '工作区不干净，先提交这些改动：\n%s\n' "$dirty" >&2; exit 1; }

if [ "$SKIP_BUILD" -eq 0 ]; then
    # 在 WSL 里没有全局 JAVA_HOME/ANDROID_HOME 时，用 docs/development-wsl.md 装的默认位置。
    if [ -z "${JAVA_HOME:-}" ] && ! command -v java >/dev/null 2>&1; then
        for candidate in "$HOME"/.local/java/jdk-17*; do
            [ -x "$candidate/bin/java" ] && export JAVA_HOME="$candidate" && break
        done
    fi
    if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    fi
    if [ -n "${JAVA_HOME:-}" ]; then
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
    if ! command -v java >/dev/null 2>&1; then
        echo "找不到 java：请安装 JDK 17+，或设置 JAVA_HOME" >&2
        exit 1
    fi
    # local.properties 在 Windows 与 WSL 之间共用：sdk.dir 指向不存在的 SDK 时临时改成本机路径，退出时还原。
    props="android/local.properties"
    restore_props=0
    backup_props=""
    if [ -n "${ANDROID_HOME:-}" ] && [ -f "$props" ]; then
        current_sdk="$(grep -oP '^sdk\.dir=\K.*' "$props" 2>/dev/null || true)"
        plain_sdk="${current_sdk//\\/}"
        if [ -n "$plain_sdk" ] && [ ! -d "$plain_sdk" ]; then
            backup_props="$(mktemp)"
            cp "$props" "$backup_props"
            printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$props"
            restore_props=1
        fi
    fi
    cleanup_props() {
        if [ "$restore_props" -eq 1 ]; then
            mv -f "$backup_props" "$props"
        fi
    }
    trap cleanup_props EXIT
    (
        cd android
        ./gradlew :app:assembleRelease --console=plain
        # AGP 不允许 ABI 拆分与 AAB 同时构建，AAB 单独跑并关掉拆分
        ./gradlew -PzippestBundle :app:bundleRelease --console=plain
    )
fi

apk_dir="android/app/build/outputs/apk/release"
aab="android/app/build/outputs/bundle/release/app-release.aab"
out="release"

for f in "$apk_dir/app-arm64-v8a-release.apk" \
         "$apk_dir/app-armeabi-v7a-release.apk" \
         "$apk_dir/app-universal-release.apk" \
         "$aab"; do
    [ -f "$f" ] || { echo "缺少构建产物：$f" >&2; exit 1; }
done

mkdir -p "$out"
cp -f "$apk_dir/app-arm64-v8a-release.apk"   "$out/Zippest-$version-arm64-v8a.apk"
cp -f "$apk_dir/app-armeabi-v7a-release.apk" "$out/Zippest-$version-armeabi-v7a.apk"
cp -f "$apk_dir/app-universal-release.apk"   "$out/Zippest-$version-universal.apk"
cp -f "$aab"                                 "$out/Zippest-$version.aab"

(
    cd "$out"
    sha256sum Zippest-"$version"-* > "SHA256SUMS-$version.txt"
)

git tag -a "$tag" -m "Zippest $version"

if [ "$SKIP_PUSH" -eq 0 ]; then
    GIT_TERMINAL_PROMPT=0 git push origin main
    git push origin "$tag"
fi

echo
echo "已发布 $tag"
ls -l "$out"/Zippest-"$version"* "$out/SHA256SUMS-$version.txt" |
    awk '{ printf "  %-44s %10d B\n", $9, $5 }'
if [ "$SKIP_PUSH" -eq 1 ]; then
    echo "（--skip-push：未推送，CI 不会触发）"
else
    echo "已推送，等待 GitHub Actions 自动构建并发版"
fi
