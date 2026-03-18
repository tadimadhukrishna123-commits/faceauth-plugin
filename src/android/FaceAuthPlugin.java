package com.bank.faceauth;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import org.npci.upi.security.services.CLServices;
import org.npci.upi.security.services.CLRemoteResultReceiver;
import org.npci.upi.security.services.ServiceConnectionStatusNotifier;

public class FaceAuthPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (!action.equals("faceAuth")) return false;

        this.callbackContext = callbackContext;

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);

        Activity activity = cordova.getActivity();
        String saltJson = args.getString(0);

        String keyCode = "EKYC";
        String langPref = "en_US";

        // ✅ FaceAuth credential
        String cred = "{"
                + "\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"FACE_AUTH\"}]"
                + "}";

        // ✅ IMPORTANT: Enable Aadhaar step
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

                                if (resultData == null) {
                                    callbackContext.error("Empty response");
                                    return;
                                }

                                try {

                                    String pid = "";
                                    String aadhaar = "";

                                    // ✅ PID
                                    if (resultData.containsKey("PID_DATA")) {
                                        pid = resultData.getString("PID_DATA");
                                    }

                                    // ✅ Encrypted Aadhaar
                                    if (resultData.containsKey("AADHAAR_DATA")) {
                                        aadhaar = resultData.getString("AADHAAR_DATA");
                                    }

                                    // fallback
                                    if (aadhaar == null || aadhaar.isEmpty()) {
                                        if (resultData.containsKey("encryptedAadhaar")) {
                                            aadhaar = resultData.getString("encryptedAadhaar");
                                        }
                                    }

                                    // combine response
                                    String finalResult = "{"
                                            + "\"pidData\":" + "\"" + pid + "\","
                                            + "\"encryptedAadhaar\":" + "\"" + aadhaar + "\""
                                            + "}";

                                    callbackContext.success(finalResult);

                                } catch (Exception e) {
                                    callbackContext.error(e.getMessage());
                                }
                            }
                        });

                try {

                    services.getCredential(
                            keyCode,
                            "",                 // listKeyPayload
                            cred,
                            configuration,      // ✅ IMPORTANT
                            saltJson,
                            "",                 // payInfo
                            "",                 // trust
                            langPref,
                            receiver
                    );

                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }

            @Override
            public void serviceDisconnected() {
                callbackContext.error("Service disconnected");
            }
        });

        return true;
    }
}
