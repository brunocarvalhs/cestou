---
name: firestore-serializable-models
description: Verify every data-layer *Model class used with NetworkService (Firestore) is annotated @Serializable. Use when adding, reviewing, or debugging a data/model class in any core/* or feature/* module, or when a repository silently returns an empty list/null despite Firestore having data.
---

# Firestore-backed models must be `@Serializable`

## The bug this prevents

`NetworkManager.getResponse()` (`core/data/.../network/NetworkManager.kt`) resolves the
`KSerializer` for a response type reflectively:

```kotlin
private fun <T : Any> getSerializer(response: KClass<T>, responseType: KType?): KSerializer<Any?> {
    return (responseType?.let { json.serializersModule.serializer(it) }
        ?: json.serializersModule.serializer(response.java)) as KSerializer<Any?>
}
```

If the target class (or any class it's composed of) is **not** annotated `@kotlinx.serialization.Serializable`,
this throws `SerializationException: Serializer for class 'X' is not found`. That exception is caught inside
`getResponse()` and turned into a `null` return via `handleFallback()` — there is no crash, no error surfaced to
the UI, and no log the user will see outside Logcat's `Network` tag. The repository method that called it (e.g.
`ShoppingRepositoryImpl.getAll()` / `observeAll()`) then treats the `null` as "no data" and returns an empty list.

Symptom in the field: the write to Firestore succeeds (visible in the Firebase console, correct fields, correct
`users` array), but the app shows nothing — permanently, even after reinstalling — because every read of that
collection silently discards its results. This is exactly what happened to `ShoppingModel`
(`feature/shopping/.../data/model/ShoppingModel.kt`): every sibling model (`ProductModel`, `CommonProductModel`,
`UserModel`) was `@Serializable`, but `ShoppingModel` was missing the annotation, so `List<ShoppingModel>` could
never be deserialized.

## What to check

Whenever you touch a class under a `data/model` package in `core/*` or `feature/*` that is passed as the
`response`/reified type to `NetworkService.make<T>()` or `NetworkService.observe<T>()`:

1. The class itself must be annotated `@Serializable` (`import kotlinx.serialization.Serializable`).
2. Every non-primitive property type it references (nested data classes, enums) must also be `@Serializable`.
3. The module's `build.gradle.kts` must apply `alias(libs.plugins.kotlin.serialization)` — without the compiler
   plugin, `@Serializable` has no generated serializer either. Check existing `*Model` siblings in the same
   module for the established pattern before adding a new one.

## How to verify

- Each `feature/*` module has a Konsist `FeatureStructureTest` under `src/test/.../architecture/`. It includes
  (or should include) a check that every class in that feature's `data.model` package is annotated
  `@Serializable` — run it (`./gradlew :feature:<name>:testDebugUnitTest`) after adding a new model.
  `feature/shopping`'s version of this test is the reference implementation.
- Prefer also adding a direct regression test that mirrors the runtime lookup, e.g.:

```kotlin
val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
val serializer = json.serializersModule.serializer(typeOf<List<ShoppingModel>>())
```

  If the annotation is missing, this line throws the exact `SerializationException` that would otherwise only
  surface in Logcat at runtime — see `feature/shopping/.../data/model/ShoppingModelTest.kt`.

## Do not

- Do not assume a green compile means serialization works — `@Serializable` is required at the class
  declaration; a missing annotation is a valid, silently-broken compile.
- Do not rely on manual `toMap()`/`toDomain()` mapper functions as proof the model round-trips through
  `NetworkService` — those bypass kotlinx.serialization entirely and will pass even when the reflective
  serializer lookup used by `NetworkManager` is broken.
