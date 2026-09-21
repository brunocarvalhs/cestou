# --- Cestou AI Agent (Reflection-based Tool Discovery) ---
# Preserve annotations used for reflection-based AI tool discovery
-keepattributes *Annotation*
-keep @br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction class * { *; }
-keep @br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter class * { *; }
-keepclassmembers class * {
    @br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction *;
    @br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter *;
}
