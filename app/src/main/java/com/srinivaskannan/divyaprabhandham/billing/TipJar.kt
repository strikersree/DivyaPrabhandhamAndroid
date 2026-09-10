package com.srinivaskannan.divyaprabhandham.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import java.lang.ref.WeakReference

/** One tip tier, as Play reports it. */
data class TipProduct(
    val id: String,
    val title: String,
    val price: String,
    internal val details: ProductDetails,
)

/**
 * Billing, on Google Play — tips and the ad-free unlock together, since both
 * are the same billing relationship (one Play account, one BillingClient
 * connection) even though they behave differently once purchased.
 *
 * StoreKit's equivalent on iOS is a handful of consumable tip tiers plus one
 * non-consumable ad-free unlock; this is the same idea, and — matching iOS —
 * anyone who has ever tipped is granted ad-free access too rather than
 * needing to buy it separately (see [AppState.isAdFree]).
 *
 * Tips are **consumable**, so someone who wants to give twice can — but
 * [AppState.recordTip] keeps the first date, so "supporter since" means the
 * first time they were kind, not the most recent. The ad-free unlock is
 * **non-consumable**: bought once, acknowledged (never consumed, since it
 * shouldn't be purchasable again), and restored on every launch via
 * [queryExistingPurchases] — Play doesn't proactively push existing
 * entitlements outside the purchase flow itself, so without this, a
 * reinstall or a second device could show ads to someone who already paid,
 * until whatever synced supporter/purchase state happened to catch up.
 *
 * SETUP REQUIRED (see README): create these four product IDs in the Play
 * Console — three consumable, one non-consumable. Until they exist, both
 * [products] and [adFreeProduct] stay empty/null and the relevant screens
 * say so rather than showing dead buttons.
 */
class TipJar(context: Context, private val appState: AppState) {

    var products by mutableStateOf<List<TipProduct>>(emptyList())
        private set

    var adFreeProduct by mutableStateOf<TipProduct?>(null)
        private set

    private var activityRef: WeakReference<Activity>? = null

    private val purchasesUpdated = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return@PurchasesUpdatedListener
        purchases?.forEach { handlePurchase(it) }
    }

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdated)
        // PBL 8 removed the no-arg enablePendingPurchases(). Per the official
        // migration guide, the old call was functionally equivalent to exactly
        // this — one-time products only, which is all this class sells.
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        // Recommended in the PBL 8 migration guide: re-establish the service
        // connection automatically if an API call happens while disconnected,
        // instead of the call simply failing with SERVICE_DISCONNECTED.
        .enableAutoServiceReconnection()
        .build()

    fun connect(activity: Activity) {
        activityRef = WeakReference(activity)
        if (client.isReady) {
            queryProducts()
            queryExistingPurchases()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryExistingPurchases()
                }
            }

            // Deliberately not retried on a timer: neither the tip jar nor
            // the ad-free restore reaching Play is an error worth bothering
            // anyone about. The next launch tries again.
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ALL_PRODUCT_IDS.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                },
            )
            .build()

        // PBL 8 changed onProductDetailsResponse's signature: the second
        // argument is now a QueryProductDetailsResult (which also carries
        // per-product status for items that couldn't be fetched) rather than
        // a plain List<ProductDetails>. The fetched list is read off it.
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val fetched = queryResult.productDetailsList.mapNotNull { product ->
                val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                    ?: return@mapNotNull null
                TipProduct(product.productId, product.title, price, product)
            }
            products = fetched
                .filter { it.id in TIP_PRODUCT_IDS }
                // Cheapest first, so the smallest gesture is the easiest one.
                .sortedBy { TIP_PRODUCT_IDS.indexOf(it.id) }
            adFreeProduct = fetched.find { it.id == AD_FREE_PRODUCT_ID }
        }
    }

    /**
     * Restores the ad-free unlock if Play already shows it as owned — a
     * purchase made on another device, or one that survived a reinstall.
     * Also finishes acknowledging it if that never completed (an
     * unacknowledged one-time purchase is auto-refunded by Play after three
     * days, so this matters, not just tidiness).
     */
    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            purchases.forEach { handlePurchase(it) }
        }
    }

    fun purchase(product: TipProduct) {
        val activity = activityRef?.get() ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product.details)
                        .build(),
                ),
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (AD_FREE_PRODUCT_ID in purchase.products) {
            appState.recordAdFreePurchase()
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                client.acknowledgePurchase(params) { _ -> }
            }
            return
        }
        appState.recordTip()
        // Consume it, so the same tier can be given again later.
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.consumeAsync(params) { _, _ -> }
    }

    companion object {
        private val TIP_PRODUCT_IDS = listOf(
            "tip_small",
            "tip_medium",
            "tip_large",
        )
        const val AD_FREE_PRODUCT_ID = "ad_free_unlock"
        private val ALL_PRODUCT_IDS = TIP_PRODUCT_IDS + AD_FREE_PRODUCT_ID
    }
}
