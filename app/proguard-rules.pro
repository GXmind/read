# Production hardening. Android components declared in the manifest are kept
# by the default optimized rules; application internals remain obfuscatable.
-allowaccessmodification
-repackageclasses 'com.codex.yuedu.internal'
-renamesourcefileattribute Hidden
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
