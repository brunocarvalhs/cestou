plugins {
    id("howmuch.android.library")
    id("howmuch.android.hilt")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.core.billing"
}

dependencies {
    implementation(project(":core:domain"))

    // api, not implementation: PlayBillingSubscriptionRepository's public purchase()/refresh()
    // return billing-ktx types (BillingResult), which feature:subscription needs on its own
    // compile classpath to call them.
    api(libs.billing.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.timber)
}
