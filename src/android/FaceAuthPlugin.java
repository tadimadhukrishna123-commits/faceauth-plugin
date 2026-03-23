package com.bank.faceauth;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

import org.npci.upi.security.services.CLServices;
import org.npci.upi.security.services.CLRemoteResultReceiver;
import org.npci.upi.security.services.ServiceConnectionStatusNotifier;

public class FaceAuthPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    // ✅ SINGLETON SERVICE INSTANCE
    private static CLServices clServices = null;
    private static boolean isServiceInitialized = false;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (!action.equals("faceAuth")) {
            return false;
        }

        this.callbackContext = callbackContext;

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);

        Activity activity = cordova.getActivity();
        String saltJson = args.getString(0);

        try {

            // ✅ If already initialized → reuse
            if (isServiceInitialized && clServices != null) {
                callGetCredential(clServices, saltJson);
                return true;
            }

            // ✅ Initialize ONLY ONCE
            CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

                @Override
                public void serviceConnected(CLServices services) {

                    clServices = services;
                    isServiceInitialized = true;

                    callGetCredential(services, saltJson);
                }

                @Override
                public void serviceDisconnected() {
                    callbackContext.error("Service disconnected");

                    clServices = null;
                    isServiceInitialized = false;
                }
            });

        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }

        return true;
    }

    // ✅ COMMON METHOD FOR AADHAAR + FACE
    private void callGetCredential(CLServices services, String saltJson) {

        try {

            String cred = "{\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"FACE_AUTH\"}]}";

            CLRemoteResultReceiver receiver =
                    new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {

                            try {

                                if (resultData == null) {
                                    callbackContext.error("Empty response");
                                    return;
                                }

                                String result;

                                if (resultData.containsKey("PID_DATA")) {
                                    result = resultData.getString("PID_DATA");
                                }
                                else if (resultData.containsKey("encryptedPid")) {
                                    result = resultData.getString("encryptedPid");
                                }
                                else {
                                    result = resultData.toString();
                                }

                                callbackContext.success(result);

                                // ❌ DO NOT RELEASE SERVICE HERE

                            } catch (Exception e) {
                                callbackContext.error(e.getMessage());
                            }
                        }
                    });

            services.getCredential(
                    "EKYC",
                    "",
                    cred,
                    "",
                    saltJson,
                    "",
                    "",
                    "en_US",
                    receiver
            );

        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }
}
