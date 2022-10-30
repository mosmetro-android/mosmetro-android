# Don't obfuscate but strip out unused code.
-dontobfuscate
-optimizations !code/allocation/variable

# Keep some fields of BuildConfig for Sentry
-keep class pw.thedrhax.mosmetro.BuildConfig { 
    java.lang.String BRANCH_NAME;
    java.lang.Integer BUILD_NUMBER;
}

# jsoup
-keeppackagenames org.jsoup.nodes

# Okio
-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
