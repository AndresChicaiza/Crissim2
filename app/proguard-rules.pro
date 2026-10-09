# CrisisSim ProGuard Rules
# Keep Room entities
-keep class com.example.crissim.data.local.entity.** { *; }
# Keep Gson serialization models
-keep class com.example.crissim.domain.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
