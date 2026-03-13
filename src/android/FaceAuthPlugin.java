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

    private static final String TAG = "FaceAuthPlugin";
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (!action.equals("faceAuth")) {
            return false;
        }

        Log.d(TAG, "FaceAuth action triggered");

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

            Log.d(TAG, "Transaction ID: " + txnId);
            Log.d(TAG, "Device ID: " + deviceId);
            Log.d(TAG, "Salt JSON: " + salt);

            CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

                @Override
                public void serviceConnected(CLServices services) {

                    Log.d(TAG, "NPCI SDK service connected");

                    CLRemoteResultReceiver receiver =
                            new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                                @Override
                                protected void onReceiveResult(int resultCode, Bundle resultData) {

                                    Log.d(TAG, "FaceAuth Result Code: " + resultCode);
                                    Log.d(TAG, "FaceAuth Result Bundle: " + resultData);

                                    if (resultData != null) {

                                        try {

                                            // Handle SDK error response
                                            if (resultData.containsKey("errorCode")) {

                                                String errorCode = String.valueOf(resultData.get("errorCode"));
                                                String errorMsg = String.valueOf(resultData.get("error"));

                                                Log.e(TAG, "FaceAuth Error: " + errorCode + " - " + errorMsg);

                                                callbackContext.error(
                                                        "FaceAuth Error: " + errorCode + " - " + errorMsg
                                                );
                                                return;
                                            }

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

                                            Log.d(TAG, "FaceAuth Success Result: " + result);

                                            callbackContext.success(result);

                                        } catch (Exception e) {

                                            Log.e(TAG, "Result parsing error: " + e.getMessage());
                                            callbackContext.error(e.getMessage());

                                        }

                                    } else {

                                        Log.e(TAG, "Empty result from FaceAuth SDK");
                                        callbackContext.error("Empty result from FaceAuth SDK");

                                    }
                                }

                            });

                    try {

                        Log.d(TAG, "Calling NPCI getCredential()");

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

                        Log.e(TAG, "getCredential() failed: " + e.getMessage());
                        callbackContext.error(e.getMessage());

                    }

                }

                @Override
                public void serviceDisconnected() {

                    Log.e(TAG, "NPCI SDK service disconnected");
                    callbackContext.error("NPCI SDK service disconnected");

                }

            });

        } catch (Exception e) {

            Log.e(TAG, "FaceAuthPlugin exception: " + e.getMessage());
            callbackContext.error(e.getMessage());

        }

        return true;
    }
}
