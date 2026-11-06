param(
    [switch]$SkipBuild,
    [switch]$SkipPush
)

# 一键发版：构建 -> 归档产物与校验和 -> 打 tag -> 推送（tag 会触发 CI 自动发 Release）。
# 用法：& tools\release.ps1
# 前提：工作区干净（仅允许 design/ 下未跟踪图片），当前分支为 main。

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    function Invoke-Step {
        param([string]$Label, [scriptblock]$Action)
        $previous = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            & $Action 2>&1 | ForEach-Object { Write-Host $_ }
        } finally {
            $ErrorActionPreference = $previous
        }
        if ($LASTEXITCODE -ne 0) {
            throw "$Label 失败（exit $LASTEXITCODE）"
        }
    }

    $gradleProps = Get-Content 'android\app\build.gradle.kts' -Raw
    if ($gradleProps -notmatch 'versionName\s*=\s*"([^"]+)"') {
        throw '无法从 build.gradle.kts 读取 versionName'
    }
    $version = $Matches[1]
    $tag = "v$version"

    if (git tag --list $tag) {
        throw "tag $tag 已存在；先提升 versionName 再发版"
    }
    $branch = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($branch -ne 'main') {
        throw "当前分支是 $branch，切回 main 再发版"
    }
    $dirty = git status --porcelain | Where-Object { $_ -notmatch '^\?\? design/' }
    if ($dirty) {
        throw "工作区不干净，先提交这些改动：`n$($dirty -join "`n")"
    }

    if (-not $SkipBuild) {
        $env:JAVA_HOME = 'C:\Android Studio\jbr'
        Push-Location android
        try {
            Invoke-Step 'assembleRelease' { & .\gradlew.bat :app:assembleRelease --console=plain }
            # AGP 不允许 ABI 拆分与 AAB 同时构建，AAB 单独跑并关掉拆分
            Invoke-Step 'bundleRelease' { & .\gradlew.bat -PzippestBundle :app:bundleRelease --console=plain }
        } finally {
            Pop-Location
        }
    }

    $apkDir = 'android\app\build\outputs\apk\release'
    $aab = 'android\app\build\outputs\bundle\release\app-release.aab'
    $out = 'release'
    New-Item -ItemType Directory -Force $out | Out-Null
    Copy-Item "$apkDir\app-arm64-v8a-release.apk" "$out\Zippest-$version-arm64-v8a.apk" -Force
    Copy-Item "$apkDir\app-armeabi-v7a-release.apk" "$out\Zippest-$version-armeabi-v7a.apk" -Force
    Copy-Item "$apkDir\app-universal-release.apk" "$out\Zippest-$version-universal.apk" -Force
    Copy-Item $aab "$out\Zippest-$version.aab" -Force

    $files = Get-ChildItem $out -Filter "Zippest-$version*"
    $sums = foreach ($file in $files) {
        $hash = (Get-FileHash $file.FullName -Algorithm SHA256).Hash.ToLower()
        "$hash  $($file.Name)"
    }
    $sums | Set-Content "$out\SHA256SUMS-$version.txt" -Encoding ASCII

    git tag -a $tag -m "Zippest $version"
    if (-not $SkipPush) {
        $env:GIT_TERMINAL_PROMPT = '0'
        Invoke-Step '推送 main' { git push origin main }
        Invoke-Step "推送 $tag" { git push origin $tag }
    }

    Write-Host ""
    Write-Host "已发布 $tag" -ForegroundColor Green
    $files | ForEach-Object {
        Write-Host ("  {0}  {1:N0} B" -f $_.Name, $_.Length)
    }
    Write-Host "  SHA256SUMS-$version.txt"
    if ($SkipPush) {
        Write-Host "（-SkipPush：未推送，CI 不会触发）" -ForegroundColor Yellow
    } else {
        Write-Host "已推送，等待 GitHub Actions 自动构建并发版" -ForegroundColor Green
    }
} finally {
    Pop-Location
}
