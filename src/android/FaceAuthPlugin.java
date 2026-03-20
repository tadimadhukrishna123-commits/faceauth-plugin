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

    private static final String TAG = "FaceAuthPlugin";
    private CallbackContext callbackContext;

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

            String keyCode = "EKYC";
            String langPref = "en_US";

            // ✅ FACE AUTH ONLY (OLD WORKING)
            String cred = "{\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"FACE_AUTH\"}]}";

            CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

                @Override
                public void serviceConnected(CLServices services) {

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
                                        else if (resultData.containsKey("PID_DATA_XML")) {
                                            result = resultData.getString("PID_DATA_XML");
                                        }
                                        else if (resultData.containsKey("encryptedPid")) {
                                            result = resultData.getString("encryptedPid");
                                        }
                                        else {
                                            result = resultData.toString();
                                        }

                                        callbackContext.success(result);

                                    } catch (Exception e) {
                                        callbackContext.error(e.getMessage());
                                    }
                                }

                            });

                    services.getCredential(
                            keyCode,
                            "",
                            cred,
                            "",   // ❌ NO configuration (old working)
                            saltJson,
                            "",
                            "",
                            langPref,
                            receiver
                    );
                }

                @Override
                public void serviceDisconnected() {
                    callbackContext.error("Service disconnected");
                }
            });

        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }

        return true;
    }
}
