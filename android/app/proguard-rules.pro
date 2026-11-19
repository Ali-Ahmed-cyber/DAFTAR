# Keep Room entities, DAOs, and generated database implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Keep SQLCipher native bindings & SupportOpenHelperFactory
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.zetetic.**

# Keep Android Biometric & Keystore helpers
-keep class androidx.biometric.** { *; }

# Keep ZXing QR/Barcode encoder classes
-keep class com.google.zxing.** { *; }

# Keep Kotlin Coroutines & Serialization metadata
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
