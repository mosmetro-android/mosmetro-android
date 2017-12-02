# Don't obfuscate but strip out unused code.
-dontobfuscate
-optimizations !code/allocation/variable

# jsoup
-keeppackagenames org.jsoup.nodes

# Okio
-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
