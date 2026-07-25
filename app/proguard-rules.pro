# kotlinx.serialization generates serializers as companion-object members and
# looks them up reflectively at the entry points. R8 cannot see that, so the
# @Serializable classes and their generated serializers have to be kept or the
# entire corpus fails to parse in a release build — and only in a release build,
# which is the worst way to find out.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.srinivaskannan.divyaprabhandham.**$$serializer { *; }
-keepclassmembers class com.srinivaskannan.divyaprabhandham.** {
    *** Companion;
}
-keepclasseswithmembers class com.srinivaskannan.divyaprabhandham.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Play Billing's listener callbacks are invoked reflectively from the library.
-keep class com.android.billingclient.api.** { *; }
