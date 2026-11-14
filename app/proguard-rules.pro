# Add project specific ProGuard rules here.
# Keep app classes
-keep class com.amintum.bestedgingpanel.** { *; }
-keepclassmembers class com.amintum.bestedgingpanel.** { *; }

# Keep ViewBinding
-keep class com.amintum.bestedgingpanel.databinding.** { *; }

# Skydoves ColorPickerView
-keep class com.skydoves.colorpickerview.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.GeneratedAppGlideModule
-keepclassmembers public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
