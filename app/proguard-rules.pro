# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.Dao { *; }

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Compose
-keep class androidx.compose.** { *; }

# Alarmes e Receivers
-keep class com.jonataprezende.medreminder.alarm.** { *; }
-keep class com.jonataprezende.medreminder.util.** { *; }