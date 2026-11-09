#!/usr/bin/env bash
# 发布前门禁：把《产品优化与调整指南》第 5 章清单里能机器判定的项一次跑完。
#
# 用法：
#   tools/preflight.sh                     # 构建 + 全部检查
#   tools/preflight.sh --skip-build        # 复用已有产物，只做检查
#   tools/preflight.sh --no-links          # 跳过联网链接检查（离线/无网络时）
#   tools/preflight.sh --device <serial>   # 额外做真机安装与启动冒烟（清单第 5 项的前半段）
#   tools/preflight.sh --report <路径>      # 指定报告文件（默认 release/preflight-<版本>.txt）
#
# 退出码：0 全部通过；1 有失败项；2 环境不满足（缺 JDK/SDK/产物）。
#
# 覆盖清单：1（versionName 与 Git 标签）、2（versionCode 单调）、3（lint）、
# 4（单元测试）、5（真机安装/启动冒烟，可选）、13（16 KB 对齐）、14（ABI）、
# 15（权限与网络调用面）、16（签名）、17（SHA256SUMS）、18（隐私与许可链接）。
# 第 6-12 项需要逐场景真机操作，本脚本只负责第 5 项的冒烟部分。
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SKIP_BUILD=0
NO_LINKS=0
CLEAN_DEVICE=0
DEVICE_SERIAL=""
REPORT=""

while [ $# -gt 0 ]; do
    case "$1" in
        --skip-build) SKIP_BUILD=1 ;;
        --no-links)   NO_LINKS=1 ;;
        --clean)      CLEAN_DEVICE=1 ;;
        --device)     DEVICE_SERIAL="${2:-}"; shift ;;
        --report)     REPORT="${2:-}"; shift ;;
        -h|--help)    sed -n '2,18p' "$0"; exit 0 ;;
        *)            echo "未知参数：$1" >&2; exit 2 ;;
    esac
    shift
done

# JDK / Android SDK：与 tools/release.sh 相同，WSL 里找不到时用默认安装位置。
if [ -z "${JAVA_HOME:-}" ] && ! command -v java >/dev/null 2>&1; then
    for candidate in "$HOME"/.local/java/jdk-17*; do
        [ -x "$candidate/bin/java" ] && export JAVA_HOME="$candidate" && break
    done
fi
if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/Android/Sdk" ]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
fi
[ -n "${JAVA_HOME:-}" ] && export PATH="$JAVA_HOME/bin:$PATH"
[ -n "${ANDROID_HOME:-}" ] && export PATH="$ANDROID_HOME/platform-tools:$PATH"

VERSION="$(grep -oP 'versionName\s*=\s*"\K[^"]+' android/app/build.gradle.kts 2>/dev/null || true)"
CODE="$(grep -oP 'versionCode\s*=\s*\K[0-9]+' android/app/build.gradle.kts 2>/dev/null || true)"
[ -n "$VERSION" ] && [ -n "$CODE" ] || { echo "无法从 build.gradle.kts 读取版本号" >&2; exit 2; }

APK_DIR="android/app/build/outputs/apk/release"
AAB="android/app/build/outputs/bundle/release/app-release.aab"
ARM64="$APK_DIR/app-arm64-v8a-release.apk"
V7A="$APK_DIR/app-armeabi-v7a-release.apk"
UNIVERSAL="$APK_DIR/app-universal-release.apk"

REPORT="${REPORT:-release/preflight-$VERSION.txt}"
mkdir -p "$(dirname "$REPORT")"
: > "$REPORT"
{
    printf 'Zippest 发布前检查报告\n'
    printf '版本：%s（versionCode %s）\n' "$VERSION" "$CODE"
    printf '提交：%s\n' "$(git rev-parse --short HEAD 2>/dev/null || echo '?')"
    printf '时间：%s\n' "$(date '+%Y-%m-%d %H:%M:%S %Z')"
} | tee -a "$REPORT"

FAILED=0
PASSED=0

record() { # record <pass|fail|skip> <编号> <名称> <证据>
    printf '%-4s %-4s %-34s %s\n' "$1" "$2" "$3" "$4" | tee -a "$REPORT"
    case "$1" in
        PASS) PASSED=$((PASSED + 1)) ;;
        FAIL) FAILED=$((FAILED + 1)) ;;
    esac
}
pass() { record PASS "$1" "$2" "$3"; }
fail() { record FAIL "$1" "$2" "$3"; }
skip() { record SKIP "$1" "$2" "$3"; }
section() { printf '\n== %s ==\n' "$1" | tee -a "$REPORT"; }

latest_build_tool() {
    find "$ANDROID_HOME/build-tools" -maxdepth 2 -name "$1" -type f 2>/dev/null | sort -V | tail -n 1
}

command -v java >/dev/null 2>&1 || { echo "找不到 java：安装 JDK 17+ 或设置 JAVA_HOME" >&2; exit 2; }
[ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ] || { echo "找不到 Android SDK：设置 ANDROID_HOME" >&2; exit 2; }

section "版本与标签"
current_tag="$(git describe --tags --exact-match 2>/dev/null || true)"
if [ -n "$current_tag" ] && [ "$current_tag" = "v$VERSION" ]; then
    pass 1 "versionName 与 tag 一致" "versionName=$VERSION tag=$current_tag"
elif [ -n "$current_tag" ] && [ -n "$(git status --porcelain 2>/dev/null)" ]; then
    skip 1 "versionName 与 tag 一致" "版本号已改但尚未提交（HEAD 在 $current_tag）；提交并打 tag 后由 CI 校验"
elif [ -n "$current_tag" ]; then
    fail 1 "versionName 与 tag 一致" "versionName=$VERSION 但 HEAD 在 $current_tag"
else
    skip 1 "versionName 与 tag 一致" "HEAD 尚未打 tag（打 tag 时由 CI 校验）"
fi

previous_tag="$(git tag --list 'v*' --sort=-v:refname | grep -v -x "v$VERSION" | head -n 1 || true)"
if [ -n "$previous_tag" ]; then
    previous_code="$(git show "$previous_tag:android/app/build.gradle.kts" 2>/dev/null | grep -oP 'versionCode\s*=\s*\K[0-9]+' | head -n 1 || true)"
    if [ -n "$previous_code" ] && [ "$CODE" -gt "$previous_code" ]; then
        pass 2 "versionCode 单调递增" "$previous_tag=$previous_code 当前=$CODE"
    elif [ -n "$previous_code" ]; then
        fail 2 "versionCode 单调递增" "$previous_tag=$previous_code 当前=$CODE（未递增）"
    else
        skip 2 "versionCode 单调递增" "读不到 $previous_tag 的 versionCode"
    fi
else
    skip 2 "versionCode 单调递增" "仓库里没有更早的 tag"
fi

if [ "$SKIP_BUILD" -eq 0 ]; then
    section "构建、lint 与单元测试"
    build_log="$(mktemp)"
    if ( cd android && ./gradlew --console=plain lintDebug testDebugUnitTest ) >"$build_log" 2>&1; then
        pass 3 "lintDebug 0 error" "gradle lintDebug 成功（报告：android/app/build/reports/lint-results-debug.html）"
        pass 4 "单元测试全绿" "gradle testDebugUnitTest 成功"
    else
        tail -n 40 "$build_log" | tee -a "$REPORT"
        fail 3 "lintDebug 0 error" "gradle lintDebug 失败（见上方输出）"
        fail 4 "单元测试全绿" "gradle testDebugUnitTest 失败（见上方输出）"
        rm -f "$build_log"
        echo "构建阶段已失败，跳过产物检查。" | tee -a "$REPORT"
        printf '\n通过 %d ／ 失败 %d\n' "$PASSED" "$FAILED" | tee -a "$REPORT"
        exit 1
    fi
    if ( cd android && ./gradlew --console=plain :app:assembleRelease ) >>"$build_log" 2>&1; then
        pass 5 "release APK 构建" "assembleRelease 成功"
    else
        tail -n 40 "$build_log" | tee -a "$REPORT"
        fail 5 "release APK 构建" "assembleRelease 失败"
    fi
    if ( cd android && ./gradlew --console=plain -PzippestBundle :app:bundleRelease ) >>"$build_log" 2>&1; then
        pass 5 "release AAB 构建" "bundleRelease 成功"
    else
        tail -n 40 "$build_log" | tee -a "$REPORT"
        fail 5 "release AAB 构建" "bundleRelease 失败"
    fi
    rm -f "$build_log"
else
    skip 3 "lintDebug 0 error" "--skip-build：未重新运行"
    skip 4 "单元测试全绿" "--skip-build：未重新运行"
fi

section "产物检查"
missing=0
for f in "$ARM64" "$V7A" "$UNIVERSAL" "$AAB"; do
    [ -f "$f" ] || { missing=1; fail 5 "产物存在" "缺少 $f"; }
done
if [ "$missing" -eq 0 ]; then
    pass 5 "产物存在" "arm64 / v7a / universal / AAB 均在 $APK_DIR"
fi

if [ "$missing" -eq 0 ]; then
    abi_out="$(python3 - "$ARM64" "$V7A" "$UNIVERSAL" <<'PY'
import sys, zipfile
allowed = {"arm64-v8a", "armeabi-v7a"}
problems = []
for path in sys.argv[1:]:
    with zipfile.ZipFile(path) as z:
        abis = sorted({n.split("/")[1] for n in z.namelist()
                       if n.startswith("lib/") and n.count("/") >= 2})
    extra = [a for a in abis if a not in allowed]
    if extra:
        problems.append(f"{path}: 多余 ABI {extra}")
    if "arm64-v8a" in path and "arm64-v8a" not in abis:
        problems.append(f"{path}: 缺 arm64-v8a")
    if "armeabi-v7a" in path and "armeabi-v7a" not in abis:
        problems.append(f"{path}: 缺 armeabi-v7a")
print("; ".join(problems) if problems else "ABI 仅含 arm64-v8a / armeabi-v7a")
PY
)"
    if [ "$abi_out" = "ABI 仅含 arm64-v8a / armeabi-v7a" ]; then
        pass 14 "APK 仅含 ARM 两个 ABI" "$abi_out"
    else
        fail 14 "APK 仅含 ARM 两个 ABI" "$abi_out"
    fi
fi

ZIPALIGN="$(latest_build_tool zipalign)"
if [ -z "$ZIPALIGN" ]; then
    fail 13 "16 KB 页对齐" "找不到 zipalign"
elif [ "$missing" -eq 0 ]; then
    aligned=0
    for apk in "$ARM64" "$V7A" "$UNIVERSAL"; do
        "$ZIPALIGN" -c -P 16 -v 4 "$apk" >/dev/null 2>&1 || aligned=1
    done
    if [ "$aligned" -eq 0 ]; then
        pass 13 "16 KB 页对齐" "zipalign -c -P 16 三个 APK 均通过"
    else
        fail 13 "16 KB 页对齐" "至少一个 APK 未通过 zipalign -c -P 16"
    fi
fi

APKSIGNER="$(latest_build_tool apksigner)"
if [ -z "$APKSIGNER" ]; then
    fail 16 "签名与历史版本一致" "找不到 apksigner"
elif [ "$missing" -eq 0 ]; then
    certs="$("$APKSIGNER" verify --print-certs "$ARM64" 2>/dev/null || true)"
    dn="$(printf '%s\n' "$certs" | sed -n 's/^Signer #1 certificate DN: //p')"
    digest="$(printf '%s\n' "$certs" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
    if [ -z "$dn" ] || [ -z "$digest" ]; then
        fail 16 "签名与历史版本一致" "APK 未签名或 apksigner 失败"
    else
        same=1
        for apk in "$V7A" "$UNIVERSAL"; do
            other="$("$APKSIGNER" verify --print-certs "$apk" 2>/dev/null | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
            [ "$other" = "$digest" ] || same=0
        done
        case "$dn" in
            *CN=Zippest*) cn_ok=1 ;;
            *) cn_ok=0 ;;
        esac
        if [ "$same" -eq 1 ] && [ "$cn_ok" -eq 1 ]; then
            pass 16 "签名与历史版本一致" "CN=Zippest，三个 APK 证书 SHA-256 相同（${digest:0:16}…）"
        else
            fail 16 "签名与历史版本一致" "DN=$dn 三个 APK 证书一致=$same"
        fi
    fi
fi

AAPT2="$(latest_build_tool aapt2)"
if [ -z "$AAPT2" ]; then
    fail 15 "权限清单符合预期" "找不到 aapt2"
elif [ "$missing" -eq 0 ]; then
    allow_list="tools/allowed-permissions.txt"
    dump="$("$AAPT2" dump permissions "$ARM64" 2>/dev/null || true)"
    actual="$(printf '%s\n' "$dump" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p" | sort -u)"
    expected="$(sed 's/#.*//' "$allow_list" | awk 'NF { print $1 }' | sort -u)"
    extra="$(comm -23 <(printf '%s\n' "$actual") <(printf '%s\n' "$expected"))"
    missing_perm="$(comm -13 <(printf '%s\n' "$actual") <(printf '%s\n' "$expected"))"
    if [ -z "$extra" ] && [ -z "$missing_perm" ]; then
        count="$(printf '%s\n' "$actual" | grep -c .)"
        pass 15 "权限清单符合预期" "$count 项，与 tools/allowed-permissions.txt 完全一致"
    else
        detail=""
        [ -n "$extra" ] && detail="多出：$(printf '%s' "$extra" | tr '\n' ' ')"
        [ -n "$missing_perm" ] && detail="$detail 缺少：$(printf '%s' "$missing_perm" | tr '\n' ' ')"
        fail 15 "权限清单符合预期" "$detail"
    fi
    network_hits="$(grep -rln 'HttpURLConnection\|openConnection\|java\.net\.URL\|OkHttpClient\|java\.net\.Socket' android/app/src/main/java --include='*.kt' | sort)"
    if [ "$network_hits" = "android/app/src/main/java/com/decompress/app/data/UpdateChecker.kt" ]; then
        pass 15 "网络调用只出现在更新检查" "仅 UpdateChecker.kt 使用 HTTP"
    else
        fail 15 "网络调用只出现在更新检查" "命中：$(printf '%s' "$network_hits" | tr '\n' ' ')"
    fi
fi

if [ -f "release/SHA256SUMS-$VERSION.txt" ]; then
    checksum_ok=0
    for pair in \
        "Zippest-$VERSION-arm64-v8a.apk:$ARM64" \
        "Zippest-$VERSION-armeabi-v7a.apk:$V7A" \
        "Zippest-$VERSION-universal.apk:$UNIVERSAL" \
        "Zippest-$VERSION.aab:$AAB"; do
        name="${pair%%:*}"
        path="${pair#*:}"
        want="$(grep -E "  $name\$" "release/SHA256SUMS-$VERSION.txt" | awk '{print $1}' | head -n 1)"
        got="$(sha256sum "$path" | awk '{print $1}')"
        [ -n "$want" ] && [ "$want" = "$got" ] || checksum_ok=1
    done
    if [ "$checksum_ok" -eq 0 ]; then
        pass 17 "SHA256SUMS 与产物一致" "release/SHA256SUMS-$VERSION.txt 四个产物全部匹配"
    else
        fail 17 "SHA256SUMS 与产物一致" "产物与 release/SHA256SUMS-$VERSION.txt 不一致"
    fi
else
    skip 17 "SHA256SUMS 与产物一致" "尚未归档：release/SHA256SUMS-$VERSION.txt 不存在"
fi

section "链接与离线副本"
if [ "$NO_LINKS" -eq 1 ]; then
    skip 18 "隐私政策与许可链接可打开" "--no-links：跳过"
else
    bad=0
    for url in \
        "https://github.com/Tnomlav/Zippest" \
        "https://raw.githubusercontent.com/Tnomlav/Zippest/main/LICENSE" \
        "https://raw.githubusercontent.com/Tnomlav/Zippest/main/PRIVACY.md"; do
        code="$(curl -L -s -o /dev/null -w '%{http_code}' --max-time 15 "$url" || echo 000)"
        case "$code" in
            2*) ;;
            *) bad=1; echo "  $url -> HTTP $code" | tee -a "$REPORT" ;;
        esac
    done
    if [ "$bad" -eq 0 ]; then
        pass 18 "隐私政策与许可链接可打开" "GitHub 仓库 / LICENSE / PRIVACY.md 均返回 2xx"
    else
        fail 18 "隐私政策与许可链接可打开" "有链接不可达（见上）"
    fi
fi

if [ "$missing" -eq 0 ]; then
    legal_out="$(python3 - "$ARM64" LICENSE PRIVACY.md <<'PY'
import sys, zipfile
apk, *files = sys.argv[1:]
with zipfile.ZipFile(apk) as z:
    names = set(z.namelist())
    problems = []
    for f in files:
        entry = f"assets/legal/{f}"
        if entry not in names:
            problems.append(f"APK 缺 {entry}")
            continue
        if z.read(entry) != open(f, "rb").read():
            problems.append(f"{entry} 与仓库根目录文件不一致")
print("; ".join(problems) if problems else "assets/legal 与仓库文件逐字节一致")
PY
)"
    if [ "$legal_out" = "assets/legal 与仓库文件逐字节一致" ]; then
        pass 18 "许可离线副本随包" "$legal_out"
    else
        fail 18 "许可离线副本随包" "$legal_out"
    fi
else
    fail 18 "许可离线副本随包" "没有可检查的 APK"
fi

if [ -f "docs/release-notes/$VERSION.md" ]; then
    if grep -qiE '^Full Changelog|compare/v' "docs/release-notes/$VERSION.md" && [ "$(wc -l < "docs/release-notes/$VERSION.md")" -lt 8 ]; then
        fail 20 "发布说明面向用户" "docs/release-notes/$VERSION.md 只有 compare 链接"
    else
        pass 20 "发布说明面向用户" "docs/release-notes/$VERSION.md 存在（$(wc -l < "docs/release-notes/$VERSION.md") 行）"
    fi
else
    fail 20 "发布说明面向用户" "缺少 docs/release-notes/$VERSION.md"
fi

section "真机冒烟（清单第 5 项）"
if [ -z "$DEVICE_SERIAL" ]; then
    skip 5 "真机安装与启动" "未指定 --device <serial>"
elif [ "$missing" -ne 0 ]; then
    fail 5 "真机安装与启动" "没有可安装的 APK"
else
    adb=(adb -s "$DEVICE_SERIAL")
    if ! "${adb[@]}" install -r "$ARM64" >/dev/null 2>&1; then
        fail 5 "真机安装与启动" "adb install 失败（手机锁屏时会被拒绝，先解锁）"
    else
        installed_version="$("${adb[@]}" shell dumpsys package com.decompress.app 2>/dev/null | grep -m1 versionName | tr -d '\r' | awk -F= '{print $2}')"
        "${adb[@]}" shell am force-stop com.decompress.app >/dev/null 2>&1 || true
        "${adb[@]}" shell am start -W -n com.decompress.app/.MainActivity >/dev/null 2>&1 || true
        sleep 1
        focused="$("${adb[@]}" shell dumpsys window 2>/dev/null | grep -m1 mCurrentFocus || true)"
        case "$focused" in
            *com.decompress.app*) started=1 ;;
            *) started=0 ;;
        esac
        if [ "$installed_version" = "$VERSION" ] && [ "$started" -eq 1 ]; then
            pass 5 "真机安装与启动" "已安装 $installed_version 并启动到前台"
        else
            fail 5 "真机安装与启动" "安装版本=$installed_version 前台=$started"
        fi
        "${adb[@]}" shell am force-stop com.decompress.app >/dev/null 2>&1 || true
    fi
fi

if [ -n "$DEVICE_SERIAL" ] && [ "$CLEAN_DEVICE" -eq 1 ]; then
    adb=(adb -s "$DEVICE_SERIAL")
    "${adb[@]}" shell am force-stop com.decompress.app >/dev/null 2>&1 || true
    "${adb[@]}" shell 'rm -f /sdcard/window_dump.xml; rm -rf /data/local/tmp/zippest-*; rm -f /data/local/tmp/Zippest-*.apk' >/dev/null 2>&1 || true
    leftovers="$("${adb[@]}" shell 'ls /data/local/tmp 2>/dev/null | grep -i zippest; test -f /sdcard/window_dump.xml && echo window_dump.xml' | tr -d '\r')"
    if [ -z "$leftovers" ]; then
        pass 19 "清理 adb 调试痕迹" "已 force-stop，/data/local/tmp/zippest-* 与 window_dump.xml 已删除"
    else
        fail 19 "清理 adb 调试痕迹" "仍有残留：$leftovers"
    fi
fi

printf '\n报告：%s\n' "$REPORT" | tee -a "$REPORT"
printf '通过 %d ／ 失败 %d\n' "$PASSED" "$FAILED" | tee -a "$REPORT"
[ "$FAILED" -eq 0 ] || exit 1
