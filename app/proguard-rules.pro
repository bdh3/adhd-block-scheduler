# ProGuard rules for the app module.
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in Android Studio's build.gradle file.
#
# Add any project specific keep options here:
#
# If you use reflection but not R8 full mode, you might need to manually keep
# constructors of your data classes in order to be able to deserialize them.
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keep class * extends androidx.versionedparcelable.VersionedParcelable
-keepclassmembers,allowobfuscation class * {
    @androidx.versionedparcelable.ParcelField *;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}
