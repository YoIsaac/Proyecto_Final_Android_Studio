# ProGuard rules for EcoConnect 3.0 AAA

# Keep Room entities and DAOs
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
    @androidx.room.Database *;
}

# Keep SQLCipher
-keep class net.zetetic.database.sqlcipher.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep student name in models
-keepclassmembers class com.example.proyectofinal.data.** { *; }
