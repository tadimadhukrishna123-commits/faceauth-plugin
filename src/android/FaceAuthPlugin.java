package com.bank.faceauth;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;

import org.npci.upi.security.services.*;

public class FaceAuthPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        this.callbackContext = callbackContext;

        try {
            String cred = args.getString(0);
            String salt = args.getString(1);

            // Add deviceId dynamically
            salt = enrichSalt(salt);

            // Initialize SDK
            initSDK();

            if (action.equals("startAadhaar")) {
                startAadhaar(cred, salt);
                return true;
            } 
            else if (action.equals("faceAuth")) {
                faceAuth(cred, salt);
                return true;
            }

        } catch (Exception e) {
            callbackContext.error("ERROR: " + e.getMessage());
        }

        return false;
    }

    // 🔹 Inject deviceId into salt
    private String enrichSalt(String salt) throws Exception {

        JSONObject json = new JSONObject(salt);

        String deviceId = Settings.Secure.getString(
                cordova.getActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        json.put("deviceId", deviceId);

        return json.toString();
    }

    // 🔹 Initialize SDK
    private void initSDK() {

        if (Constant.clServices == null) {
            CLServices.initService(cordova.getActivity(), new ServiceConnectionStatusNotifier() {

                @Override
                public void serviceConnected(CLServices services) {
                    Constant.clServices = services;
                }

                @Override
                public void serviceDisconnected() {
                }
            });
        }
    }

    // 🔹 Aadhaar Capture
    private void startAadhaar(String cred, String salt) {

        try {

            Constant.clServices.getCredential(
                    "EKYC",              // keyCode
                    "",                  // xmlPayload
                    cred,                // controls (Aadhaar)
                    "",                  // config
                    salt,                // salt
                    "",                  // trust
                    "",                  // payInfo
                    "en_US",             // language
                    getReceiver("AADHAAR")
            );

        } catch (Exception e) {
            callbackContext.error("AADHAAR ERROR: " + e.getMessage());
        }
    }

    // 🔹 FaceAuth Capture
    private void faceAuth(String cred, String salt) {

        try {

            Constant.clServices.getCredential(
                    "EKYC",
                    "",
                    cred,                // controls (FaceAuth)
                    "",
                    salt,
                    "",
                    "",
                    "en_US",
                    getReceiver("FACE")
            );

        } catch (Exception e) {
            callbackContext.error("FACE ERROR: " + e.getMessage());
        }
    }

    // 🔹 Result Handler
    private CLRemoteResultReceiver getReceiver(String type) {

        return new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {

                try {

                    String result = "";

                    if (resultData != null) {
                        result = resultData.toString();
                    }

                    // Aadhaar success
                    if (type.equals("AADHAAR") && resultCode == 2) {
                        callbackContext.success("AADHAAR_SUCCESS:" + result);
                    }

                    // Face success
                    else if (type.equals("FACE") && resultCode == 1) {
                        callbackContext.success("FACE_SUCCESS:" + result);
                    }

                    // Failure
                    else {
                        callbackContext.error("FAILED CODE: " + resultCode);
                    }

                } catch (Exception e) {
                    callbackContext.error("RECEIVER ERROR: " + e.getMessage());
                }
            }
        });
    }
}
