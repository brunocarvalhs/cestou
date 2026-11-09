# Remote Config Rollout Standard Operating Procedure (SOP)

**Last Updated:** 2026-11-08  
**Owner:** DevOps / Platform Team  
**Frequency:** Every new feature or config change

---

## 🚀 Normal Rollout Process

### Phase 1: DEV (Local)

```
1. Write config + default in RemoteConfigKeys.kt + RemoteConfigDefaults.kt
2. Add local defaults (no Firebase yet)
3. Test offline: simulate network failure, verify fallback works
4. Test cache: verify 12h TTL behavior
5. Code review: lint, detekt, tests must pass
```

### Phase 2: STAGING (Firebase Staging Project)

```
1. Merge to develop branch
2. Firebase Console (Staging):
   a. Add parameter with default value (0% users see new value)
   b. Create condition: "Disabled" or random_percent <= 0
3. QA: Build app, point to Staging Firebase
4. Test with real Firebase:
   - Fetch config
   - Verify value applied
   - Change value → verify app reloads within 5s
   - Check GA4 event logged
   - Check Crashlytics tag set
```

### Phase 3: CANARY ROLLOUT (5% → 25% → 50% → 100%)

When app version released to Play Store:

#### **Week 1: Canary 5%**
```
Firebase Console (Production):
- Condition: random_percent <= 5
- Targets: ~5% of active users
- Monitor: GA4 adoption, Crashlytics crash rate, event logs
```

**Monitoring Checklist:**
- [ ] GA4: config shows in "Config Adoption Curve" (5% adoption)
- [ ] Crashlytics: crash rate stable (no spike > 0.1%)
- [ ] No error reports in Firebase Console
- [ ] Rollback-ready (have v1.0 config saved)

#### **Week 2: Canary 25%**
```
Firebase Console:
- Condition: random_percent <= 25
- Expand to 25% of active users
- Key: Only expand if Week 1 metrics clean
```

#### **Week 3: Staged 50%**
```
Firebase Console:
- Condition: country in ['BR', 'US'] && random_percent <= 50
- Regional rollout (primary markets first)
- Rationale: Faster iteration if issues in specific region
```

#### **Week 4: Full 100%**
```
Firebase Console:
- Remove condition or set random_percent <= 100
- All users now see new config
- Celebration 🎉
```

---

## 🆘 Incident Response (Crash Spike)

### Alert Triggered

**Condition:** Crashlytics reports >0.1% crash rate increase after config push

**Timeline:** **30 minutes max to resolve**

### Triage (5 min)

```
1. Firebase Console: View crashes
2. Identify: Which stack trace? Remote config version tag?
3. Link: Does crash correlate with config change?
   - If yes → it's the config
   - If no → investigate separately
```

### Action (10 min)

**Option A: Disable Toggle**
```
Firebase Console → Parameter → Edit Condition
- Change: value = true → value = false
- OR: random_percent <= 25 → random_percent <= 0
- Save
```

**Option B: Revert to Previous Version**
```
Firebase Console → Manage Versions → Rollback to v1.0.0
- If entire config set was buggy
```

### Verify (5 min)

```
1. Monitor: Crash rate drops back to baseline
2. Analytics: Check GA4 — did new value stop propagating?
3. Logs: Confirm old code path is executing
```

### Post-Mortem (1 hour)

```
1. Why did it crash?
   - Missing null check?
   - Logic error?
   - Unexpected data type?

2. Why didn't tests catch it?
   - Missing scenario in tests?
   - Edge case in production not in testing?

3. What changes?
   - Add test case
   - Add validation
   - Add Crashlytics breadcrumb
```

---

## 📋 Checklist: New Config Addition

Use this before pushing ANY new config to Firebase.

- [ ] **Code Ready**
  - [ ] RemoteConfigKeys.kt: constant defined
  - [ ] RemoteConfigDefaults.kt: default value set
  - [ ] Code: FeatureFlagService.isEnabled(key) used
  - [ ] Lint/Detekt: zero findings
  - [ ] Tests: offline, cache, fetch scenarios covered

- [ ] **Documentation**
  - [ ] KDoc in RemoteConfigKeys explains purpose, dependencies, rollout plan
  - [ ] GA4 event name confirmed (config_key_changed)
  - [ ] Crashlytics tag: remote_config_version set

- [ ] **Analytics Readiness**
  - [ ] GA4 event: config_value_changed has [config_key, old_value, new_value]
  - [ ] Crashlytics: configured to tag all crashes with config_version
  - [ ] GA4 Exploration ready: adoption curve query written

- [ ] **Firebase Setup (Staging)**
  - [ ] Parameter added to Staging config
  - [ ] Default value set (safe default)
  - [ ] Condition created (e.g., 0% or disabled)
  - [ ] QA tested on Staging Firebase

- [ ] **Rollout Plan**
  - [ ] Week 1 target: 5% (condition: random_percent <= 5)
  - [ ] Week 2 target: 25% (condition: random_percent <= 25)
  - [ ] Week 3 target: 50% (regional: BR + US)
  - [ ] Week 4 target: 100% (no condition)
  - [ ] Escalation contact identified (if crash spike)

- [ ] **Code Review**
  - [ ] Approved by tech lead
  - [ ] All comments resolved
  - [ ] Ready to merge to develop

---

## 📊 GA4 Monitoring Queries

### Query 1: Config Adoption Curve (Real-Time)

```
Event: config_value_changed
Dimensions: event_parameter(config_key), event_date
Metrics: user_count
Chart: Line
Period: Last 30 days
```

**What to see:**
- Line starts at 5% (Week 1)
- Steps up to 25% (Week 2)
- Steps up to 50% (Week 3)
- Reaches 100% (Week 4)

---

### Query 2: Crash Correlation

```
Event: crash
Dimensions: event_parameter(remote_config_version)
Metrics: event_count
Breakdown: by remote_config_version
Period: Last 7 days
```

**What to see:**
- Crash count stable when config version unchanged
- Spike when new version deployed
- Drop when rolled back or disabled

---

## 🔄 A/B Testing Workflow

### Setup

```
1. Define test: "ab_test_ui_layout_enabled"
2. Variant A (Control): random_user_id % 2 == 0 → false
3. Variant B (New): random_user_id % 2 == 1 → true
4. Duration: 2 weeks typical
5. Metrics: engagement_time, crash_rate, user_retention
```

### Monitoring

```
GA4 Exploration:
- Segment by: ab_variant_id (variant_a, variant_b)
- Compare: engagement_time_msec, user_count
- Result: If B > A by >5%, rollout B to 100%
```

### Conclusion

```
1. If B wins: set value=true for all users
2. If A wins: keep value=false
3. If tie: keep either (no strong signal)
```

---

## 📞 Escalation

| Scenario | Action | Contact |
|---|---|---|
| **Crash spike >1%** | Disable toggle immediately | On-call engineer |
| **Config fetch timeout** | Verify Firebase Console status | Platform team |
| **Cache corruption** | Clear DataStore, restart app | DevOps |
| **GA4 event not logged** | Check analytics service | Analytics team |

---

## ✅ Success Criteria

- [ ] Config deployed without user-facing interruption
- [ ] Rollout follows canary schedule (no 0%→100% jumps)
- [ ] GA4 adoption curve visible and monotonic
- [ ] Crashlytics tags all crashes with config_version
- [ ] Rollback possible in <30 minutes if needed
- [ ] A/B test results clear after 2 weeks

---

## 📝 Example: Deploy SearchProduct Function

```
Timeline: 2026-11-15 (Monday)

1. DEVELOP (2026-11-08):
   - Implement SearchProductFunction
   - Add to RemoteConfigKeys: APP_FUNCTIONS_SEARCH_PRODUCT_ENABLED = "feat_app_functions_search_product"
   - Add to RemoteConfigDefaults: APP_FUNCTIONS_SEARCH_PRODUCT_ENABLED_DEFAULT = true
   - Add tests (offline, fetch, cache)
   - PR review + merge
   - ✓ Code ready

2. STAGING (2026-11-10):
   - Firebase Staging: Add parameter, condition 0%
   - QA tests with Staging Firebase
   - GA4 event verified
   - Crashlytics tag verified
   - ✓ QA ready

3. PRODUCTION WEEK 1 (2026-11-15):
   - App v2.3.0 released to Play Store
   - Firebase Production: Set condition random_percent <= 5
   - Monitor GA4: adoption curve starts at 5%
   - Check Crashlytics: crash rate stable
   - ✓ Canary 5% active

4. PRODUCTION WEEK 2 (2026-11-22):
   - GA4 check: 5% adoption stable, no crashes
   - Firebase: Update condition random_percent <= 25
   - ✓ Canary 25% active

5. PRODUCTION WEEK 3 (2026-11-29):
   - GA4 check: 25% adoption stable, no crashes
   - Firebase: Update condition country in ['BR', 'US'] && random_percent <= 50
   - ✓ Staged 50% active (BR + US)

6. PRODUCTION WEEK 4 (2026-12-06):
   - GA4 check: 50% adoption stable, no crashes
   - Firebase: Remove condition (or set to 100%)
   - ✓ Full 100% rollout complete

Result: SearchProduct available to all users in under 4 weeks, with safety checks at each step.
```
