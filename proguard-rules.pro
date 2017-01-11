# jsoup
-keeppackagenames org.jsoup.nodes

# Okio
-dontwarn okio.**

# Providers methods which are accessed via reflection
-keepclassmembers class * extends pw.thedrhax.mosmetro.authenticator.Provider {
    <init>(android.content.Context);
    public static boolean match(pw.thedrhax.mosmetro.httpclient.Client);
}
