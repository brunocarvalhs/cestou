# Remote Config Template: Add New Configuration

Use this template when adding a new feature flag or variable to Remote Config.

---

## Step 1: Define in Code

### RemoteConfigKeys.kt

```kotlin
object FeatureFlagKeys {
    // CATEGORY: <Funcionalidade>
    
    /**
     * <Breve descrição do que faz>.
     * 
     * **Purpose:** <Por que existe / problema que resolve>
     * **Dependencies:** <Outros toggles que precisam estar ON>
     *   - If none, write "None"
     *   - Example: "Requer AI_OPENROUTER_ENABLED ou AI_GEMINI_ENABLED"
     * **Default (Offline):** on/off
     * **Rollout Strategy:** 
     *   - Week 1: Canary 5% (random_percent <= 5)
     *   - Week 2: Canary 25% (random_percent <= 25)
     *   - Week 3: Staged 50% (country in ['BR', 'US'])
     *   - Week 4: Full 100%
     * **Monitoring:** 
     *   - GA4 event: config_value_changed
     *   - Crashlytics tag: remote_config_version
     *   - Dashboard: adoption curve in GA4 Explorations
     * **Rollback Trigger:** Crash rate > 0.1% or feature completely broken
     * **SDD:** sdd-XXX (reference)
     */
    const val NEW_FEATURE_ENABLED = "feat_new_feature"
}
```

### RemoteConfigDefaults.kt

```kotlin
object RemoteConfigDefaults {
    // CATEGORY: <Funcionalidade>
    
    // Choose safe default (usually true for best UX)
    // If feature causes issues, disable via Firebase (not code)
    const val NEW_FEATURE_ENABLED_DEFAULT = true  // ON by default
}
```

---

## Step 2: Implement Usage in Code

```kotlin
// Inject FeatureFlagService where needed
class MyViewModel @Inject constructor(
    private val featureFlagService: FeatureFlagService,
    private val remoteConfigAnalytics: RemoteConfigAnalytics,
) : ViewModel() {
    
    fun initializeFeature() {
        val isEnabled = featureFlagService.isEnabled(
            FeatureFlagKeys.NEW_FEATURE_ENABLED
        )
        
        if (isEnabled) {
            // New behavior
            startNewFeature()
        } else {
            // Fallback (old behavior or disabled state)
            startOldFeature()
        }
    }
}
```

---

## Step 3: Add Analytics Tracking

### GA4: Track Config Change

```kotlin
// When config value changes from false → true (or vice versa)
remoteConfigAnalytics.logConfigValueChanged(
    configKey = "feat_new_feature",
    oldValue = "false",
    newValue = "true",
    source = "remote_config"  // or "fallback_default", "local_cache"
)
```

### GA4: Track A/B Test (if applicable)

```kotlin
// If testing two variants
remoteConfigAnalytics.logABTestVariant(
    testName = "new_feature_ui_test",
    variantId = "variant_a_control",  // or variant_b_new
    userCohort = "control"  // or "treatment"
)
```

---

## Step 4: Write Tests

### Test: Offline Fallback

```kotlin
@Test
fun `new feature uses default when offline`() = runTest {
    // Mock Firebase offline
    coEvery { firebaseRemoteConfig.fetch() } throws IOException("Network error")
    
    // Initialize service
    val service = FeatureFlagService(remoteConfig)
    
    // Should use default
    val result = service.isEnabled(FeatureFlagKeys.NEW_FEATURE_ENABLED)
    assert(result == RemoteConfigDefaults.NEW_FEATURE_ENABLED_DEFAULT)
}
```

### Test: Cache

```kotlin
@Test
fun `config cached for 12 hours`() = runTest {
    // Fetch at time T
    val service = RemoteConfigService(remoteConfig, dataStore)
    service.fetchAndCache()
    
    // Verify cache TTL = 12 hours
    val cachedTime = dataStore.getLastFetchTime()
    val currentTime = System.currentTimeMillis()
    val ageMins = (currentTime - cachedTime) / (1000 * 60)
    
    assert(ageMins < 12 * 60)
}
```

### Test: Sync on Foreground

```kotlin
@Test
fun `sync triggered when app foreground after 12h`() = runTest {
    // Initial fetch at time T
    // Simulate 13 hours passing
    // App comes to foreground
    
    // Should fetch again (cache stale)
    coEvery { firebaseRemoteConfig.fetch() } returns Unit
    val service = RemoteConfigService(remoteConfig, dataStore, syncStrategy)
    service.onAppForeground()
    
    coVerify { firebaseRemoteConfig.fetch() }
}
```

---

## Step 5: Firebase Console Setup (Staging First)

### Add Parameter (Staging Project)

1. Firebase Console → Remote Config (Staging)
2. Click "Add parameter"
3. Fill:
   - **Parameter key:** `feat_new_feature`
   - **Description:** "New feature enabled (sdd-XXX)"
   - **Value type:** Boolean
   - **Default value:** `true`

### Create Initial Condition (0% Users)

1. Click "Create condition"
2. Name: `Disabled`
3. Targeting rule: Leave empty or set to `random_percent <= 0`
4. Value: `true` (will be overridden by condition)
5. Save

### Test with QA

```
1. Build app pointing to Staging Firebase
2. Fetch config: Should get false (0% condition)
3. Change Firebase value → refresh app → verify change within 5s
4. Check GA4: event logged?
5. Check Crashlytics: tag set?
```

---

## Step 6: Firebase Console Setup (Production)

### Copy Staging → Production

1. Firebase Console → Remote Config (Production)
2. Add same parameter as Staging
3. Set default to `true`
4. Create condition: `Disabled` (random_percent <= 0)

### Week 1: Canary 5%

1. Edit condition: `Disabled` → `Canary Wave 1 (5%)`
2. Targeting rule: `random_percent <= 5`
3. Value: `true`
4. Save

Monitor for 1 week. If stable, proceed.

### Week 2: Canary 25%

1. Edit condition
2. Targeting rule: `random_percent <= 25`
3. Save
4. Monitor for 1 week

### Week 3: Staged 50%

1. Edit condition
2. Targeting rule: `country in ['BR', 'US'] && random_percent <= 50`
3. Save
4. Monitor for 1 week

### Week 4: Full 100%

1. Edit condition
2. Targeting rule: Remove or set `random_percent <= 100`
3. Save
4. Celebrate! 🎉

---

## Step 7: Monitoring Dashboard

### GA4: Create Adoption Curve Exploration

1. GA4 → Explorations → New exploration
2. Title: "Config Adoption: feat_new_feature"
3. Dimensions: `event_parameter(config_key)`, `event_date`
4. Metrics: `user_count`
5. Filter: `config_key == "feat_new_feature"`
6. Chart: Line chart
7. Save

### Expected Shape

```
% of Users Seeing Value=true
|
100% |                    _____ Full 100%
  80% |                 /
  50% |              _/  Staged 50%
  25% |            /
  5%  |__________/  Canary 5%
  0%  |____________
       Week1 Week2 Week3 Week4
```

---

## Step 8: Code Review & Checklist

Before merging to main:

- [ ] RemoteConfigKeys.kt: constant + KDoc with dependencies
- [ ] RemoteConfigDefaults.kt: default value + reasoning
- [ ] Implementation: FeatureFlagService.isEnabled() used correctly
- [ ] Tests: offline, cache, sync scenarios
- [ ] GA4 events: config_value_changed logged
- [ ] Crashlytics: remote_config_version tag set
- [ ] Documentation: rollout plan written (this template filled out)
- [ ] Lint/Detekt: zero findings
- [ ] Firebase Staging: parameter added, tested by QA

---

## Rollback Checklist (If Needed)

If crash rate > 0.1%:

- [ ] **Identify:** Crashlytics shows crash correlated with remote_config_version tag
- [ ] **Act (10 min max):**
  - Firebase Console → Parameter → Edit Condition
  - Set condition value to `false` OR random_percent <= 0
  - Save
- [ ] **Verify (5 min):**
  - Check crash rate drops back to baseline
  - GA4: verify no new users seeing true value
- [ ] **Post-mortem:** Why? Add test case to prevent recurrence.

---

## Example: Complete Flow

```
2026-11-08:
✓ Code: new feature + tests + docs
✓ Staging Firebase: parameter added, QA tested
✓ Code review: merged to develop

2026-11-15:
✓ App v2.3.0 released
✓ Production Firebase: Canary 5% enabled
✓ GA4 adopted adoption curve starts

2026-11-22:
✓ Week 1 metrics clean: 5%, no crashes
✓ Production Firebase: Canary 25% enabled

2026-11-29:
✓ Week 2 metrics clean: 25%, no crashes
✓ Production Firebase: Staged 50% (BR+US)

2026-12-06:
✓ Week 3 metrics clean: 50%, no crashes
✓ Production Firebase: Full 100%
✓ Feature available to all users ✓

```
