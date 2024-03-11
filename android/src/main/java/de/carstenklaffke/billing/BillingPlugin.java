package de.carstenklaffke.billing;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin()
public class BillingPlugin extends Plugin {

    private BillingClient billingClient;

    @PluginMethod()
    public void querySkuDetails(final PluginCall call) {
        final String productId = call.getString("product");

        billingClient = BillingClient.newBuilder(bridge.getActivity())
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> list) {

                    }
                })
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<String> skuList = new ArrayList<>();
                    skuList.add(productId);
                    String type = call.getString("type", "INAPP").equals("SUBS") ? BillingClient.SkuType.SUBS : BillingClient.SkuType.INAPP;
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(type);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                        JSObject ret = null;
                                        try {
                                            ret = new JSObject(skuDetailsList.get(0).getOriginalJson());
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        call.resolve(ret);
                                    } else{
                                        call.reject("error");
                                    }
                                }
                            });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    @PluginMethod()
    public void launchBillingFlow(final PluginCall call) {
        final String productId = call.getString("product");

        billingClient = BillingClient.newBuilder(bridge.getActivity())
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                                && purchases != null) {
                            if (purchases != null && purchases.size() > 0) {
                                JSObject ret = null;
                                try {
                                    Purchase purchase = purchases.get(0);
                                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                        acknowledgeAndConsumePurchase(purchase, call);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                            call.reject("canceled");
                        } else {
                            call.reject("error");
                        }
                    }
                })
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<String> skuList = new ArrayList<>();
                    skuList.add(productId);
                    String type = call.getString("type", "INAPP").equals("SUBS") ? BillingClient.SkuType.SUBS : BillingClient.SkuType.INAPP;
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(type);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                .setSkuDetails(skuDetailsList.get(0))
                                                .build();
                                        billingClient.launchBillingFlow(bridge.getActivity(), billingFlowParams);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    private void acknowledgeAndConsumePurchase(Purchase purchase, PluginCall call) {
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams acknowledgePurchaseParams =
                    AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
            AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        consumePurchase(purchase, call);
                    } else {
                        call.reject("error acknowledging purchase");
                    }
                }
            };
            billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
        } else {
            consumePurchase(purchase, call);
        }
    }

    private void consumePurchase(Purchase purchase, PluginCall call) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        ConsumeResponseListener consumeResponseListener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String outToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    JSObject ret = new JSObject();
                    ret.put("message", "Product consumed successfully");
                    call.resolve(ret);
                } else {
                    call.reject("error consuming purchase");
                }
            }
        };

        billingClient.consumeAsync(consumeParams, consumeResponseListener);
    }
}
