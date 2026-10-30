# kotlinx.serialization generated serializers are referenced reflectively by name.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class dev.haxzie.smsbridge.net.** {
    *** Companion;
}
-keepclasseswithmembers class dev.haxzie.smsbridge.net.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# BouncyCastle registers algorithms reflectively through its provider classes.
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
