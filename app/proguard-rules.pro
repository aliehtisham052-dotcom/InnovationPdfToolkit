# ML Kit and Play Services ship their own consumer rules, so no manual keeps are needed for them.
# Kotlin coroutines / AndroidX / Compose rules also come from their consumer files.

# DataStore preference keys are referenced by name only through our own code, so nothing to keep.

# Keep the enums used for tool/mode selection, since they are looked up by name in a few places.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
