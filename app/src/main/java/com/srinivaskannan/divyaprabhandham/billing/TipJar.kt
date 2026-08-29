package com.srinivaskannan.divyaprabhandham.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * The tip jar, on Google Play Billing.
 *
 * StoreKit's equivalent on iOS is a handful of consumable products; this is the
 * same idea. Tips are **consumable** rather than one-off purchases so that
 * someone who wants to give twice can — but [AppState.recordTip] keeps the
 * first date, so "supporter since" means the first time they were kind, not the
 * most recent.
 *
 * SETUP REQUIRED (see README): create these three product IDs in the Play
 * Console as consumable in-app products. Until they exist, [products] stays
 * empty and the tip screen says so rather than showing dead buttons.
 */
class TipJar(context: Context, private val appState: AppState) {

    var products by mutableStateOf<List<TipProduct>>(emptyList())
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
        // this — one-time products only, which is all the tip jar sells.
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
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) queryProducts()
            }

            // Deliberately not retried on a timer: a tip jar that cannot reach
            // Play is not an error worth bothering anyone about. The next
            // launch tries again.
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                PRODUCT_IDS.map { id ->
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
            products = queryResult.productDetailsList
                .mapNotNull { product ->
                    val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                        ?: return@mapNotNull null
                    TipProduct(product.productId, product.title, price, product)
                }
                // Cheapest first, so the smallest gesture is the easiest one.
                .sortedBy { PRODUCT_IDS.indexOf(it.id) }
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
        appState.recordTip()
        // Consume it, so the same tier can be given again later.
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.consumeAsync(params) { _, _ -> }
    }

    companion object {
        private val PRODUCT_IDS = listOf(
            "tip_small",
            "tip_medium",
            "tip_large",
        )
    }
}
