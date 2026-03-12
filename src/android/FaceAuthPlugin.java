package com.bank.faceauth;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import org.npci.upi.security.services.CLServices;
import org.npci.upi.security.services.CLRemoteResultReceiver;
import org.npci.upi.security.services.ServiceConnectionStatusNotifier;

public class FaceAuthPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (!action.equals("faceAuth")) {
            return false;
        }

        this.callbackContext = callbackContext;

        Activity activity = cordova.getActivity();
        String salt = args.getString(0);

        try {

            String cred = "{\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"FACE_AUTH\"}]}";
            String keyCode = "EKYC";
            String langPref = "en_US";
            String txnId = String.valueOf(System.currentTimeMillis());

            String deviceId = Settings.Secure.getString(
                    activity.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

                @Override
                public void serviceConnected(CLServices services) {

                    CLRemoteResultReceiver receiver =
                            new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                                @Override
                                protected void onReceiveResult(int resultCode, Bundle resultData) {

                                    if (resultData != null) {

                                        String result;

                                        if (resultData.containsKey("PID_DATA")) {
                                            result = resultData.getString("PID_DATA");
                                        }
                                        else if (resultData.containsKey("PID_DATA_XML")) {
                                            result = resultData.getString("PID_DATA_XML");
                                        }
                                        else if (resultData.containsKey("RESULT")) {
                                            result = resultData.getString("RESULT");
                                        }
                                        else {
                                            result = resultData.toString();
                                        }

                                        callbackContext.success(result);

                                    } else {
                                        callbackContext.error("Empty FaceAuth result");
                                    }

                                }
                            });

                    try {

                        services.getCredential(
                                keyCode,
                                txnId,
                                cred,
                                "user@upi",
                                salt,
                                deviceId,
                                "ANDROID",
                                langPref,
                                receiver
                        );

                    } catch (Exception e) {
                        callbackContext.error(e.getMessage());
                    }
                }

                @Override
                public void serviceDisconnected() {
                    callbackContext.error("RD service disconnected");
                }

            });

        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }

        return true;
    }
}
