package com.desire.widget.billing;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import com.desire.widget.util.PreferenceManager;

import java.util.Collections;
import java.util.List;

/**
 * Wraps Play Billing for a single one-time "Pro unlock" in-app product. Entitlement is cached in
 * {@link PreferenceManager} ({@code isPremium}) so the rest of the app can gate Pro widgets without
 * knowing about billing. Fails safe: if billing is unavailable the app simply stays on the free
 * tier. (Requires a matching {@code pro_unlock} managed product in the Play Console to transact.)
 */
public class BillingManager {
    public static final String PRODUCT_PRO = "pro_unlock";

    public interface Listener {
        void onProStateChanged(boolean isPro);
    }

    private static BillingManager instance;

    private final Context app;
    private final BillingClient client;
    private ProductDetails proDetails;
    private Listener listener;

    private BillingManager(Context context) {
        this.app = context.getApplicationContext();
        this.client = BillingClient.newBuilder(app)
                .setListener(this::onPurchasesUpdated)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
        connect();
    }

    public static synchronized BillingManager getInstance(Context context) {
        if (instance == null) instance = new BillingManager(context);
        return instance;
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public boolean isPro() {
        return PreferenceManager.getInstance(app).isPremium();
    }

    private void connect() {
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProduct();
                    queryOwned();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Best-effort; next getInstance/usage will reconnect on demand if needed.
            }
        });
    }

    private void queryProduct() {
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PRODUCT_PRO)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()))
                .build();
        client.queryProductDetailsAsync(params, (result, details) -> {
            if (details != null && !details.isEmpty()) proDetails = details.get(0);
        });
    }

    /** Restores entitlement for users who already bought Pro. */
    public void queryOwned() {
        client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (result, purchases) -> {
                    boolean owned = false;
                    if (purchases != null) {
                        for (Purchase p : purchases) {
                            if (p.getProducts().contains(PRODUCT_PRO)
                                    && p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                owned = true;
                                acknowledge(p);
                            }
                        }
                    }
                    setPro(owned || isPro());
                });
    }

    public void launchPurchase(Activity activity) {
        if (proDetails == null) {
            // Not ready (offline or product not configured) — try to refresh and bail gracefully.
            queryProduct();
            return;
        }
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(proDetails)
                                .build()))
                .build();
        client.launchBillingFlow(activity, params);
    }

    private void onPurchasesUpdated(@NonNull BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) {
                if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    acknowledge(p);
                    setPro(true);
                }
            }
        }
    }

    private void acknowledge(Purchase p) {
        if (p.isAcknowledged()) return;
        client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(),
                r -> { /* no-op */ });
    }

    private void setPro(boolean pro) {
        boolean was = isPro();
        PreferenceManager.getInstance(app).setPremium(pro);
        if (pro != was && listener != null) listener.onProStateChanged(pro);
    }
}
