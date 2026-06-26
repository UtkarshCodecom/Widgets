# Keep SerializedName fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep data models
-keep class com.desire.widget.data.model.** { *; }
-keep class com.desire.widget.data.local.entity.** { *; }

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }

# Lottie
-keep class com.airbnb.lottie.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
