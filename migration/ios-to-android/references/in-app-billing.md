> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

> [!WARNING]
> **Warning:** **Anti-Premature-Exit Execution Rule for Billing** : When replacing mock purchases or integrating Google Play Billing, do NOT stop after backgrounding tests or emit passive messages like *"Waiting for tests to
> complete..."* . You MUST author the complete Kotlin `BillingClient` integration (`queryProductDetailsAsync`, purchases listener, purchase flow launcher, and a real reconnect in `onBillingServiceDisconnected()`), wire it into the ViewModel and Screen to replace mock purchase actions (deleting the mock path --- no "fallback" that still grants the purchase), and verify `./gradlew assembleDebug` exits 0 before concluding.

This reference guide maps iOS `StoreKit` constructs and workflows to their
Android equivalents using the Google Play Billing Library.

**Official Reference Links:**

- [Google Play Billing overview \| Android Developers](https://developer.android.com/google/play/billing)
- [Integrate the Google Play Billing Library into your app \| Android
  Developers](https://developer.android.com/google/play/billing/integrate)
- [Test your Google Play Billing Library integration \| Android Developers](https://developer.android.com/google/play/billing/test)

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS implementation using `StoreKit` is **Mandatory**.
- **Software Requirements:** Android Gradle Plugin (AGP) 8.0+, Kotlin 1.8+, Kotlin Coroutines (`kotlinx-coroutines-android:1.7+`), and Google Play Billing Library 8 (`com.android.billingclient:billing-ktx:8.3.0`) are **Mandatory**. Google Play rejects new apps and updates built with Billing Library 7 or earlier.
- **Account Requirements:** A Google Play Console account and configured products are **Mandatory**.

## Limitations

This skill has the following limitations:

- **Google Play Billing Focus:** Examples are tailored exclusively for the Google Play Billing Library. It does not cover alternative app stores or custom billing solutions.
- **Receipt Validation:** Focuses on the client-side flow and acknowledging purchases. Detailed server-side receipt validation is not covered in depth.

## Glossary

| iOS Term (`StoreKit`) | Android Equivalent (`Billing Library`) | Definition |
|---|---|---|
| `SKProductsRequest` | `queryProductDetailsAsync` | Querying for product information. |
| `SKProduct` | `ProductDetails` | Details about a specific product. |
| `SKPaymentQueue` | `BillingClient` | Manages transactions and purchase flows. |
| `SKPaymentTransactionObserver` | `PurchasesUpdatedListener` | Listens for transaction updates. |
| `SKPayment` | `BillingFlowParams` | Parameters for launching a purchase. |

## Setup \& Dependencies

You **Must** add the following dependencies to your `build.gradle.kts` file to
enable Google Play Billing:

```kotlin
dependencies {
   val billingVersion = "8.3.0"
   implementation("com.android.billingclient:billing-ktx:$billingVersion")
}
```

You **Must** also declare the following permission in your
`AndroidManifest.xml`:

```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

> [!CAUTION]
> **Caution:** **Billing Permission**: The billing permission is required to interact with the Google Play Billing service. Failure to declare it will prevent purchases.

## Instructions: Mapping Concepts

### 1. Initializing the BillingClient

Map iOS `SKPaymentQueue` setup to Android `BillingClient` initialization.

- **Input:** Application context and listener.
- **Output:** Connected `BillingClient`.

**iOS**
`swift
SKPaymentQueue.default().add(self)`

**Android**
\`\`\`kotlin
val purchasesUpdatedListener = PurchasesUpdatedListener { onBillingResult, onPurchases -\>
// Handle transactions here
}

val billingClient = BillingClient.newBuilder(context)
.setListener(purchasesUpdatedListener)
// Billing Library 8 removed the no-argument enablePendingPurchases().
.enablePendingPurchases(
PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
)
.build()

private var reconnectDelayMs = 1_000L
private val maxReconnectDelayMs = 15 \* 60 \* 1_000L

fun startBillingConnection() {
billingClient.startConnection(object : BillingClientStateListener {
override fun onBillingSetupFinished(onBillingResult: BillingResult) {
if (onBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
reconnectDelayMs = 1_000L // reset backoff
// The BillingClient is ready. Query products and existing purchases here.
} else {
retryBillingConnectionWithBackoff()
}
}
override fun onBillingServiceDisconnected() {
// The connection can drop at any time (Play Store update, process death).
// You MUST actually reconnect here --- a log statement alone is not retry handling.
retryBillingConnectionWithBackoff()
}
})
}

private fun retryBillingConnectionWithBackoff() {
scope.launch {
delay(reconnectDelayMs)
reconnectDelayMs = (reconnectDelayMs \* 2).coerceAtMost(maxReconnectDelayMs)
startBillingConnection()
}
}
\`\`\`

> [!IMPORTANT]
> **Important:** **Connection lifecycle is part of the migration.** `onBillingServiceDisconnected()` must trigger a reconnect (exponential backoff as above, or reconnect-on-next-request guarded by `billingClient.isReady`). Leaving it empty or log-only means purchases silently stop working after the first disconnect. Billing Library 8 also offers `BillingClient.Builder.enableAutoServiceReconnection()`, which reconnects before the next call; it doesn't replace handling `onBillingServiceDisconnected()`.

### 2. Querying Products

Map iOS `SKProductsRequest` to Android `queryProductDetailsAsync`.

- **Input:** Set of product IDs.
- **Output:** A `QueryProductDetailsResult` holding the `ProductDetails` list (and any products that couldn't be fetched).

**iOS**
\`\`\`swift
let productIDs = Set(\["com.example.product1"\])
let request = SKProductsRequest(productIdentifiers: productIDs)
request.delegate = self
request.start()

// In your SKProductsRequestDelegate conforming class
func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
let products = response.products
// Process products
}
\`\`\`

**Android**
\`\`\`kotlin
val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
.setProductList(
listOf(
QueryProductDetailsParams.Product.newBuilder()
.setProductId("com.example.product1")
.setProductType(BillingClient.ProductType.INAPP)
.build()
)
)
.build()

billingClient.queryProductDetailsAsync(queryProductDetailsParams) { onBillingResult, queryProductDetailsResult -\>
if (onBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
// Billing Library 8 returns a result object instead of a bare list.
val productDetailsList = queryProductDetailsResult.productDetailsList
// Process productDetailsList; queryProductDetailsResult.unfetchedProducts
// lists IDs Play couldn't return.
}
}
\`\`\`

### 3. Launching Purchase Flow

Map iOS adding a payment to the queue to Android `launchBillingFlow`.

- **Input:** `ProductDetails` and Activity context.
- **Output:** Launch of the Google Play billing screen.

**iOS**
`swift
let payment = SKPayment(product: product)
SKPaymentQueue.default().add(payment)`

**Android**
\`\`\`kotlin
val productDetails = // Obtained from queryProductDetailsAsync

val billingFlowParams = BillingFlowParams.newBuilder()
.setProductDetailsParamsList(
listOf(
BillingFlowParams.ProductDetailsParams.newBuilder()
.setProductDetails(productDetails)
.build()
)
)
.build()

val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
\`\`\`

### 4. Handling Transactions

Map iOS `SKPaymentTransactionObserver` to Android `PurchasesUpdatedListener`.

- **Input:** Callback from Google Play.
- **Output:** Processing of purchases.

**iOS**
`swift
func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
for transaction in transactions {
switch transaction.transactionState {
case .purchased:
// Complete transaction
case .failed:
// Handle error
default:
break
}
}
}`

**Android**
`kotlin
val purchasesUpdatedListener = PurchasesUpdatedListener { onBillingResult, onPurchases ->
when (onBillingResult.responseCode) {
BillingClient.BillingResponseCode.OK -> {
onPurchases?.forEach { handlePurchase(it) }
}
BillingClient.BillingResponseCode.USER_CANCELED -> {
// Notify user or dismiss UI
}
BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
// Evaluate purchase history via queryPurchasesAsync to grant entitlements
}
BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
retryBillingConnectionWithBackoff() // see §1
}
else -> {
// Log error and notify user of failure
}
}
}`

### Common Billing Response Codes

| Response Code | Description | Recommended Action |
|---|---|---|
| `OK` | Success. | Proceed with operation. |
| `USER_CANCELED` | User pressed back or canceled the dialog. | Notify user or dismiss UI. |
| `ITEM_ALREADY_OWNED` | Failure to purchase since item is already owned. | Evaluate the user's purchase history via `queryPurchasesAsync` to grant entitlements. |
| `DEVELOPER_ERROR` | Invalid arguments provided. | Evaluate the parameters passed to `launchBillingFlow` to ensure they are valid and correctly formatted. |
| `ITEM_UNAVAILABLE` | Product not available for purchase. | Evaluate the product ID and region availability in Google Play Console. |
| `SERVICE_DISCONNECTED` | Play Store service disconnected. | Retry connection or notify user. |
| `SERVICE_UNAVAILABLE` | Play Store service unavailable. | Notify user to try again later. |
| `BILLING_UNAVAILABLE` | Billing API version not supported for requested type. | Notify user or degrade gracefully. |
| `ERROR` | Fatal error during the API action. | Log error and notify user of failure. |
| `SERVICE_TIMEOUT` | The request has reached the timeout. | Retry the operation with exponential backoff. |
| `FEATURE_NOT_SUPPORTED` | Requested feature is not supported by Play Store on current device. | Degrade feature availability gracefully. |

### 5. Processing Purchases (Acknowledge/Consume)

Android requires acknowledging or consuming purchases, which has no direct
equivalent in basic StoreKit 1 (StoreKit 2 handles it differently).

- **Input:** `Purchase` object.
- **Output:** Finalization of purchase in Google Play.

**Logic:**

- **IF** processing high-value items or requiring high reliability, **THEN** you **Must** use server-side APIs (Google Play Developer API) for acknowledgment and consumption to protect against network failures and fraud.
- **ELSE** use the client-side APIs defined in the table below.

**Decision Table:**

| Condition | Product Type | Action |
|---|---|---|
| `purchaseState` == `PURCHASED` | Consumable (Products that are used up and can be purchased multiple times) | Call `consumeAsync` |
| `purchaseState` == `PURCHASED` | Non-Consumable (Products that are purchased once and grant permanent access) | Call `acknowledgePurchase` |
| `purchaseState` == `PENDING` | Any | Wait for update / Notify user |

**Android Code Example (Consume and Acknowledge):**
`kotlin
fun handlePurchase(purchase: Purchase) {
if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
if (!purchase.isAcknowledged) {
if (isConsumable(purchase)) {
val consumeParams = ConsumeParams.newBuilder()
.setPurchaseToken(purchase.purchaseToken)
.build()
billingClient.consumeAsync(consumeParams) { onBillingResult, onPurchaseToken ->
if (onBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
// Purchase consumed (e.g., coins added)
}
}
} else {
val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
.setPurchaseToken(purchase.purchaseToken)
.build()
billingClient.acknowledgePurchase(acknowledgePurchaseParams) { onBillingResult ->
if (onBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
// Purchase acknowledged (e.g., premium unlocked)
}
}
}
}
}
}`

### 6. Querying Existing Purchases

Map iOS checking for active transactions to Android `queryPurchasesAsync`. This
is critical for restoring purchases or checking entitlements on startup.

- **Input:** Product type (INAPP or SUBS).
- **Output:** List of active purchases.

**Android Code Example:**
\`\`\`kotlin
val params = QueryPurchasesParams.newBuilder()
.setProductType(BillingClient.ProductType.INAPP)
.build()

billingClient.queryPurchasesAsync(params) { onBillingResult, onPurchases -\>
if (onBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
for (purchase in onPurchases) {
if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
// Grant entitlement
}
}
}
}
\`\`\`

### 7. Subscription Management

Map iOS subscription handling to Android.

- **Input:** Product ID for subscription.
- **Output:** Subscription details and purchase flow.

**Decision Table for Offer Selection:**

| User State | Target Offer | Selection Criteria |
|---|---|---|
| `NEW_USER` | Free Trial | Offer tags contain "free-trial" |
| `LAPSED_USER` | Win-back | Offer tags contain "win-back" |
| `REGULAR` | Base Plan | Offer tags contain "base-plan" |

**Android Code Example (Querying Subscriptions):**
\`\`\`kotlin
val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
.setProductList(
listOf(
QueryProductDetailsParams.Product.newBuilder()
.setProductId("com.example.subscription1")
.setProductType(BillingClient.ProductType.SUBS)
.build()
)
)
.build()

billingClient.queryProductDetailsAsync(queryProductDetailsParams) { onBillingResult, queryProductDetailsResult -\>
if (onBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
// Process subscription details
for (productDetails in queryProductDetailsResult.productDetailsList) {
val offerDetails = productDetails.subscriptionOfferDetails
// Selection logic based on eligibility
val selectedOffer = offerDetails?.find { offer -\>
when (getUserEligibility()) {
UserEligibility.NEW_USER -\> offer.tags.contains("free-trial")
UserEligibility.LAPSED_USER -\> offer.tags.contains("win-back")
UserEligibility.REGULAR -\> offer.tags.contains("base-plan")
}
} ?: offerDetails?.firstOrNull()

           val offerToken = selectedOffer?.offerToken

           // Launch purchase flow
           val billingFlowParams = BillingFlowParams.newBuilder()
               .setProductDetailsParamsList(
                   listOf(
                       BillingFlowParams.ProductDetailsParams.newBuilder()
                           .setProductDetails(productDetails)
                           .setOfferToken(offerToken ?: "")
                           .build()
                   )
               )
               .build()

           billingClient.launchBillingFlow(activity, billingFlowParams)
       }

}
}
\`\`\`

### 8. Testing Strategies

You **Must** test your billing integration thoroughly before release.

- **License Testers:** Set up license testers in the Google Play Console to test with real payment methods without being charged.
- **Static Test IDs:** Use static product IDs provided by Google Play for testing basic flows.

**Static Test IDs:**

- `android.test.purchased`: Simulates a successful purchase.
- `android.test.canceled`: Simulates a canceled purchase.
- `android.test.item_unavailable`: Simulates an unavailable product.

**Android Code Example (Testing with Static ID):**
`kotlin
val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
.setProductList(
listOf(
QueryProductDetailsParams.Product.newBuilder()
.setProductId("android.test.purchased")
.setProductType(BillingClient.ProductType.INAPP)
.build()
)
)
.build()`

### 9. Price Formatting

You **Must** display the localized price to the user.

- **Input:** `ProductDetails` object.
- **Output:** Localized price string.

**Android Code Example:**
`kotlin
val offerDetails = productDetails.oneTimePurchaseOfferDetails
val formattedPrice = offerDetails?.formattedPrice ?: "N/A"
// Display formattedPrice in UI`

### 10. Server-Side Integration \& Subscription Lifecycle

To build a production-ready billing system, you **Must** supplement client-side
code with server-side management.

### Real-Time Developer Notifications (RTDN)

You **Must** use Real-Time Developer Notifications (RTDN) to handle subscription
lifecycle events reliably.

**Logic:**

- **IF** a subscription event occurs (renewal, cancellation, grace period), **THEN** Google sends a server-to-server notification to your backend.
- You **Must** process these notifications to update user entitlements, rather than relying on client-side polling or app launches.

**RTDN Notification Types:**

You **Must** handle the following RTDN notification types on your backend:

| Notification Type | Meaning | Mandatory Action |
|---|---|---|
| `SUBSCRIPTION_RENEWED` | Subscription successfully renewed. | Extend user entitlement period. |
| `SUBSCRIPTION_CANCELED` | User cancelled subscription. | Update UI to show cancellation state, but retain access until period ends. |
| `SUBSCRIPTION_IN_GRACE_PERIOD` | Payment failed, in grace period. | Retain access, prompt user to fix payment. |
| `SUBSCRIPTION_ON_HOLD` | Grace period expired, payment still failed. | Suspend access, guide user to fix payment. |
| `SUBSCRIPTION_RECOVERED` | Payment fixed during hold/grace. | Restore access. |
| `SUBSCRIPTION_REVOKED` | Entitlement revoked by Google or user refunded. | Immediately revoke access. |

### Subscription Lifecycle States

You **Must** handle states like Grace Period and Account Hold to prevent revenue
loss and ensure a good user experience.

**Logic:**

- **IF** a user enters a **Grace Period** (payment failed but subscription active), **THEN** you **Must** retain access to the subscription features and prompt them to update payment.
- **IF** a user enters **Account Hold** (grace period expired, payment still failed), **THEN** you **Must** suspend access and guide the user to Google Play to resolve payment issues.

## Antipatterns

### DO

- **DO** check if the `BillingClient` is ready before calling any operations.
- **DO** acknowledge all non-consumable purchases within 3 days, otherwise Google Play will automatically refund the purchase.
- **DO** handle pending purchases (which include payment methods like cash, bank transfer, and convenience store payments) as required by Google Play policies.

### DON'T

- **DON'T** validate receipts on the client side. Always use a secure backend server to validate purchase tokens via the Google Play Developer API. Within `handlePurchase`, after acknowledging or consuming, you MUST send `purchase.purchaseToken` to your secure backend for validation and to record the entitlement.
- **DON'T** hardcode product IDs; fetch them from a remote config or backend if possible.

### NEVER

- **NEVER** assume a purchase is complete without checking `purchase.purchaseState == Purchase.PurchaseState.PURCHASED`.
- **NEVER** ignore the `PurchasesUpdatedListener` callback; it is the source of truth for transactions.
- **NEVER** keep a mock/simulated purchase path as a "fallback" when replacing mock purchases. If the `BillingClient` is not ready, the Activity is null, or the product details are missing, the purchase call must fail (return `false` / surface an error state and trigger a reconnect) --- it must **not** grant the entitlement, flip `isPro`, or return success. Delete the original mock purchase code rather than wrapping it in an `else` branch.
- **NEVER** leave `onBillingServiceDisconnected()` empty or log-only; it must reconnect (see §1).