# Ktor Client Content Negotiation & Serialization
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class io.ktor.client.plugins.contentnegotiation.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-keep interface io.ktor.client.plugins.contentnegotiation.** { *; }
-dontwarn io.ktor.client.plugins.contentnegotiation.**
