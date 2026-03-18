package com.bank.faceauth;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

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

            // Keep callback alive (important)
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

            return true;
        }

        return false;
    }

    private void startEkycFlow(Activity activity, String saltJson) {

        // 🔥 Correct cred config (VERY IMPORTANT)
        String cred = "{\"CredAllowed\":[{\"type\":\"AADHAAR\",\"subtype\":\"FACE_AUTH\"}]}";

        CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

            @Override
            public void serviceConnected(CLServices services) {

                CLRemoteResultReceiver receiver =
                        new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {

                                try {

                                    // 🔍 Debug logs (optional)
                                    // Log.d("EKYC", "ResultCode: " + resultCode);

                                    String encryptedAadhaar = resultData.getString("AADHAAR_DATA");
                                    String pidData = resultData.getString("PID_DATA");

                                    if (encryptedAadhaar == null) encryptedAadhaar = "";
                                    if (pidData == null) pidData = "";

                                    // Final response
                                    String finalResult = "{"
                                            + "\"encryptedAadhaar\":\"" + encryptedAadhaar + "\","
                                            + "\"pidData\":\"" + pidData + "\""
                                            + "}";

                                    callbackContext.success(finalResult);

                                } catch (Exception e) {
                                    callbackContext.error("Error: " + e.getMessage());
                                }
                            }
                        });

                // 🔥 Main SDK call
                services.getCredential(
                        "EKYC",        // keyCode
                        "",            // listKeyPayload
                        cred,          // credAllowed
                        "",            // configuration
                        saltJson,      // salt (from JS)
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
