package com.bank.faceauth;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import org.npci.upi.security.services.CLRemoteResultReceiver;
import org.npci.upi.security.services.CLServices;
import org.npci.upi.security.services.Constant;
import org.npci.upi.security.services.ServiceConnectionStatusNotifier;

public class FaceAuthPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        this.callbackContext = callbackContext;

        try {
            String cred = args.getString(0);
            String salt = args.getString(1);

            // Add deviceId
            salt = enrichSalt(salt);

            // Initialize SDK
            initSDK();

            if ("startAadhaar".equals(action)) {
                startAadhaar(cred, salt);
                return true;
            }

            if ("faceAuth".equals(action)) {
                faceAuth(cred, salt);
                return true;
            }

        } catch (Exception e) {
            callbackContext.error("EXECUTE ERROR: " + e.getMessage());
        }

        return false;
    }

    // 🔹 Add deviceId into salt
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
                public void serviceDisconnected() {}
            });
        }
    }

    // 🔹 Aadhaar Capture
    private void startAadhaar(String cred, String salt) {

        if (Constant.clServices == null) {
            callbackContext.error("SDK NOT INITIALIZED");
            return;
        }

        try {
            Constant.clServices.getCredential(
                    "EKYC",
                    "",
                    cred,
                    "",
                    salt,
                    "",
                    "",
                    "en_US",
                    getReceiver("AADHAAR")
            );
        } catch (Exception e) {
            callbackContext.error("AADHAAR ERROR: " + e.getMessage());
        }
    }

    // 🔹 FaceAuth Capture
    private void faceAuth(String cred, String salt) {

        if (Constant.clServices == null) {
            callbackContext.error("SDK NOT INITIALIZED");
            return;
        }

        try {
            Constant.clServices.getCredential(
                    "EKYC",
                    "",
                    cred,
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

    // 🔹 Handle SDK response
    private CLRemoteResultReceiver getReceiver(final String type) {

        return new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {

                try {
                    String result = resultData != null ? resultData.toString() : "";

                    // Aadhaar success
                    if ("AADHAAR".equals(type) && resultCode == 2) {
                        callbackContext.success("AADHAAR_SUCCESS:" + result);
                    }

                    // Face success
                    else if ("FACE".equals(type) && resultCode == 1) {
                        callbackContext.success("FACE_SUCCESS:" + result);
                    }

                    // Error
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
