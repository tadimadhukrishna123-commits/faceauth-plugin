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
    private String encryptedAadhaar = "";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        this.callbackContext = callbackContext;
        Activity activity = cordova.getActivity();
        String saltJson = args.optString(0);

        if (action.equals("startEkyc")) {

            startAadhaarFlow(activity, saltJson);

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

            return true;
        }

        return false;
    }

    private void startAadhaarFlow(Activity activity, String saltJson) {

        String cred = "{\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"AADHAR_NUMBER_AUTH\"}]}";

        CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

            @Override
            public void serviceConnected(CLServices services) {

                CLRemoteResultReceiver receiver =
                        new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {

                                if (resultCode == 2) { // Aadhaar success

                                    encryptedAadhaar = resultData.getString("AADHAAR_DATA");

                                    // 🔥 Now call FaceAuth
                                    startFaceAuth(services, saltJson);
                                }
                            }
                        });

                services.getCredential("EKYC", "", cred, "", saltJson, "", "", "en_US", receiver);
            }

            @Override
            public void serviceDisconnected() {
                callbackContext.error("Service disconnected");
            }
        });
    }

    private void startFaceAuth(CLServices services, String saltJson) {

        String cred = "{\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"FACE_AUTH\"}]}";

        CLRemoteResultReceiver receiver =
                new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {

                        try {
                            String pid = resultData.getString("PID_DATA");

                            String finalResult = "{"
                                    + "\"encryptedAadhaar\":\"" + encryptedAadhaar + "\","
                                    + "\"pidData\":\"" + pid + "\""
                                    + "}";

                            callbackContext.success(finalResult);

                        } catch (Exception e) {
                            callbackContext.error(e.getMessage());
                        }
                    }
                });

        services.getCredential("EKYC", "", cred, "", saltJson, "", "", "en_US", receiver);
    }
}
