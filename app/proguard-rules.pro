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

# Native engine spec models are (de)serialized by Gson via reflection — keep their fields.
-keep class com.desire.widget.engine.model.** { *; }
-keep class com.desire.widget.engine.data.** { *; }

# ZXing (QR generation)
-dontwarn com.google.zxing.**
-keep class com.google.zxing.** { *; }

# Play Billing
-keep class com.android.billingclient.** { *; }

# Play services location
-dontwarn com.google.android.gms.**
