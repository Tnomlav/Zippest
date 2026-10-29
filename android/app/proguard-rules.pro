# JNA resolves native method names and Structure fields reflectively.
-dontwarn java.awt.**
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { *; }
-keep interface com.decompress.app.data.ZstdCodec$ZstdLibrary { *; }
-keep class com.decompress.app.data.ZstdCodec$ZstdBuffer { *; }
