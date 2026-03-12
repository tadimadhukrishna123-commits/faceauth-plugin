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

        Log.d("FaceAuthPlugin", "FaceAuth action triggered");

        this.callbackContext = callbackContext;

        Activity activity = cordova.getActivity();
        String salt = args.getString(0);

        activity.runOnUiThread(() -> {

            try {

                String cred = "{\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"FACE_AUTH\"}]}";
                String keyCode = "EKYC";
                String langPref = "en_US";

                String txnId = String.valueOf(System.currentTimeMillis());

                String deviceId = Settings.Secure.getString(
                        activity.getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );

                Log.d("FaceAuthPlugin", "Transaction ID: " + txnId);
                Log.d("FaceAuthPlugin", "Device ID: " + deviceId);

                CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

                    @Override
                    public void serviceConnected(CLServices services) {

                        Log.d("FaceAuthPlugin", "RD service connected");

                        CLServices clServices = services;

                        CLRemoteResultReceiver receiver =
                                new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                                    @Override
                                    protected void onReceiveResult(int resultCode, Bundle resultData) {

                                        Log.d("FaceAuthPlugin", "RD Result Code: " + resultCode);
                                        Log.d("FaceAuthPlugin", "RD Result Bundle: " + resultData);

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

                                            Log.d("FaceAuthPlugin", "FaceAuth result: " + result);

                                            if (FaceAuthPlugin.this.callbackContext != null) {
                                                FaceAuthPlugin.this.callbackContext.success(result);
                                            }

                                        } else {

                                            Log.e("FaceAuthPlugin", "Empty result from FaceAuth");

                                            if (FaceAuthPlugin.this.callbackContext != null) {
                                                FaceAuthPlugin.this.callbackContext.error("Empty result from FaceAuth");
                                            }
                                        }
                                    }

                                });

                        try {

                            Log.d("FaceAuthPlugin", "Calling getCredential...");

                            String payer = "user@upi";
                            String saltJson = salt;

                            clServices.getCredential(
                                    keyCode,
                                    txnId,
                                    cred,
                                    payer,
                                    saltJson,
                                    deviceId,
                                    "ANDROID",
                                    langPref,
                                    receiver
                            );

                        } catch (Exception e) {

                            Log.e("FaceAuthPlugin", "getCredential failed: " + e.getMessage());

                            if (FaceAuthPlugin.this.callbackContext != null) {
                                FaceAuthPlugin.this.callbackContext.error(e.getMessage());
                            }

                        }
                    }

                    @Override
                    public void serviceDisconnected() {

                        Log.e("FaceAuthPlugin", "SDK service disconnected");

                        if (FaceAuthPlugin.this.callbackContext != null) {
                            FaceAuthPlugin.this.callbackContext.error("SDK service disconnected");
                        }

                    }

                });

            } catch (Exception e) {

                Log.e("FaceAuthPlugin", "Exception: " + e.getMessage());

                if (FaceAuthPlugin.this.callbackContext != null) {
                    FaceAuthPlugin.this.callbackContext.error(e.getMessage());
                }

            }

        });

        return true;
    }
}
