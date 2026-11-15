#!/usr/bin/env python3
"""检查 strings.xml / plurals 里是否有 %1 这种缺 $ 的占位符。"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FILES = [
    ROOT / "android/app/src/main/res/values/strings.xml",
    ROOT / "android/app/src/main/res/values-zh/strings.xml",
]
PATTERN = re.compile(r"%(\d+)(?![\$0-9])")

bad = []
for path in FILES:
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        for match in PATTERN.finditer(line):
            bad.append(f"{path.name}:{number}: {match.group(0)} -> {line.strip()[:90]}")

if bad:
    print("占位符缺少 $：")
    print("\n".join(bad))
    sys.exit(1)
print("占位符检查通过")
