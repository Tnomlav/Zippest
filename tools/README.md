# Test fixtures

The files in this directory are used for local and emulator regression tests.

- `sample-archive-2.zip` - ZIP browsing and extraction
- `sample-7z.7z` - 7Z browsing and extraction
- `sample-7z-encrypted.7z` - encrypted-header 7Z with password `Secret42`
- `sample-rar.rar` - RAR4 extraction
- `sample-rar-password.rar` - RAR5 extraction with password `junrar`
- `sample-rar-encrypted-header.rar` - RAR5 encrypted headers with password `junrar`
- `sample-tar-gz.tar.gz` - TAR.GZ browsing and extraction
- `sample-tar-bz2.tar.bz2` - TAR.BZ2 browsing and extraction
- `sample-tar-xz.tar.xz` - TAR.XZ browsing and extraction
- `sample-single.bz2` - single-file BZ2 extraction
- `sample-single.xz` - single-file XZ extraction
- `encrypted-secret.zip` - AES ZIP extraction with password `Secret42`

The RAR fixtures are copied from the Junrar project's test resources so the
Android application can be checked against known-good RAR4 and RAR5 archives.

# Release gate

`tools/preflight.sh` runs the machine-checkable half of the 20-item pre-release
checklist (chapter 5 of the product guide): version vs. tag, monotonic
versionCode, lint, unit tests, ABI set, 16 KB alignment, signature, the
permission allow-list in `tools/allowed-permissions.txt`, network-call surface,
SHA256SUMS, link reachability, bundled offline licence copies and the release
notes file.

```bash
tools/preflight.sh                            # build + all checks
tools/preflight.sh --skip-build               # reuse existing artifacts
tools/preflight.sh --device <serial> --clean  # + install/launch smoke, clean adb traces
tools/preflight.sh --no-links                 # skip network checks
```

`tools/release.sh` calls it before tagging, and CI runs it again on the tag
(`--skip-build --no-links`). Manual steps stay manual: encrypted/split/GBK/1 GB
samples, background survival, low storage and uninstall/reinstall.
