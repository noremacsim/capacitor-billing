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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin()
public class BillingPlugin extends Plugin {

    private BillingClient billingClient;

    @PluginMethod()
    public void queryAllSkuDetails(final PluginCall call) {
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
                    // Query all products
                    List<String> skuList = new ArrayList<>();
                    skuList.add("all"); // Just a placeholder, you might have different logic for retrieving all products
                    String type = call.getString("type", "INAPP").equals("SUBS") ? BillingClient.SkuType.SUBS : BillingClient.SkuType.INAPP;
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(type);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                        if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                            JSONArray skuDetailsArray = new JSONArray();
                                            for (SkuDetails skuDetails : skuDetailsList) {
                                                skuDetailsArray.put(skuDetails.getOriginalJson());
                                            }
                                            JSArray ret = new JSArray(skuDetailsArray.toString());
                                            call.resolve(ret);
                                        } else {
                                            // Log a message indicating that no SKU details were retrieved
                                            Log.e("BillingPlugin", "No SKU details retrieved");
                                            call.reject("No SKU details retrieved");
                                        }
                                    } else {
                                        // Log the billing result in case of an error
                                        Log.e("BillingPlugin", "Error querying SKU details. Response code: " + billingResult.getResponseCode());
                                        call.reject("Error querying SKU details. Response code: " + billingResult.getResponseCode());
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
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                        try {
                                            JSObject ret = new JSObject(skuDetailsList.get(0).getOriginalJson());
                                            call.resolve(ret);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            call.reject("error parsing SKU details JSON: " + e.getMessage());
                                        }
                                    } else {
                                        // Log a message indicating that no SKU details were retrieved
                                        Log.e("BillingPlugin", "No SKU details retrieved");
                                        call.reject("No SKU details retrieved");
                                    }
                                } else {
                                    // Log the billing result in case of an error
                                    Log.e("BillingPlugin", "Error querying SKU details. Response code: " + billingResult.getResponseCode());
                                    call.reject("Error querying SKU details. Response code: " + billingResult.getResponseCode());
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
                                        ret = new JSObject(purchase.getOriginalJson());
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

    @PluginMethod()
    public void closeBillingClient(final PluginCall call) {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
            billingClient = null;
        }
        call.resolve();
    }
}
