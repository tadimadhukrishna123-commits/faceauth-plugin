package com.bank.faceauth;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import org.apache.cordova.*;
import org.json.JSONArray;

import org.npci.upi.security.services.*;

public class FaceAuthPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        this.callbackContext = callbackContext;

        if (action.equals("startEkyc")) {

            Activity activity = cordova.getActivity();
            String saltJson = args.optString(0);

            startEkycFlow(activity, saltJson);

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

            return true;
        }

        return false;
    }

    private void startEkycFlow(Activity activity, String saltJson) {

        // 🔥 Aadhaar + FaceAuth flow
        String cred = "{\"CredAllowed\":[{\"type\":\"AADHAAR\",\"subtype\":\"FACE_AUTH\"}]}";

        // 🔥 IMPORTANT CONFIG (this enables Aadhaar screen)
        String configuration = "{"
                + "\"aadhaarConsent\":\"Y\","
                + "\"mode\":\"SELF\""
                + "}";

        CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

            @Override
            public void serviceConnected(CLServices services) {

                CLRemoteResultReceiver receiver =
                        new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {

                                try {

                                    Log.d("EKYC_DEBUG", "ResultCode: " + resultCode);
                                    Log.d("EKYC_DEBUG", "Bundle: " + resultData);

                                    if (resultData == null) {
                                        callbackContext.error("Empty resultData");
                                        return;
                                    }

                                    // 🔥 Return FULL response (no parsing issues)
                                    String fullResponse = resultData.toString();

                                    callbackContext.success(fullResponse);

                                } catch (Exception e) {
                                    callbackContext.error("Error: " + e.getMessage());
                                }
                            }
                        });

                services.getCredential(
                        "EKYC",        // keyCode
                        "",            // listKeyPayload
                        cred,          // Aadhaar + FaceAuth
                        configuration,// 🔥 FIX (IMPORTANT)
                        saltJson,      // salt from JS
                        "",            // payInfo
                        "",            // trust
                        "en_US",       // language
                        receiver
                );
            }

            @Override
            public void serviceDisconnected() {
                callbackContext.error("Service disconnected");
            }
        });
    }
}
