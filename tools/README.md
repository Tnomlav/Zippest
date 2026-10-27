# Test fixtures

The files in this directory are used for local and emulator regression tests.

- `sample-archive-2.zip` - ZIP browsing and extraction
- `sample-7z.7z` - 7Z browsing and extraction
- `sample-rar.rar` - RAR4 extraction
- `sample-rar-password.rar` - RAR5 extraction with password `junrar`
- `sample-tar-gz.tar.gz` - TAR.GZ browsing and extraction
- `sample-tar-bz2.tar.bz2` - TAR.BZ2 browsing and extraction
- `sample-tar-xz.tar.xz` - TAR.XZ browsing and extraction
- `sample-single.bz2` - single-file BZ2 extraction
- `sample-single.xz` - single-file XZ extraction
- `encrypted-secret.zip` - AES ZIP extraction with password `Secret42`

The RAR fixtures are copied from the Junrar project's test resources so the
Android application can be checked against known-good RAR4 and RAR5 archives.
