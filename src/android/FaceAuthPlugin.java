package com.bank.faceauth;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import org.npci.upi.security.services.CLRemoteResultReceiver;
import org.npci.upi.security.services.CLServices;
import org.npci.upi.security.services.Constant;
import org.npci.upi.security.services.ServiceConnectionStatusNotifier;

public class FaceAuthPlugin extends CordovaPlugin {

    private static final String TAG = "FaceAuthPlugin";

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        this.callbackContext = callbackContext;

        try {
            Log.d(TAG, "Action: " + action);

            String cred = args.getString(0);
            String salt = args.getString(1);

            // Inject deviceId
            salt = enrichSalt(salt);

            // Init SDK
            initSDK();

            if ("startAadhaar".equals(action)) {
                startAadhaar(cred, salt);
                return true;
            } 
            else if ("faceAuth".equals(action)) {
                faceAuth(cred, salt);
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Execute Error", e);
            callbackContext.error("EXECUTE ERROR: " + e.getMessage());
        }

        return false;
    }

    // 🔹 Add deviceId dynamically
    private String enrichSalt(String salt) throws Exception {

        JSONObject json = new JSONObject(salt);

        String deviceId = Settings.Secure.getString(
                cordova.getActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        json.put("deviceId", deviceId);

        Log.d(TAG, "Salt with deviceId: " + json.toString());

        return json.toString();
    }

    // 🔹 Initialize SDK
    private void initSDK() {

        if (Constant.clServices == null) {

            Log.d(TAG, "Initializing SDK...");

            CLServices.initService(cordova.getActivity(), new ServiceConnectionStatusNotifier() {

                @Override
                public void serviceConnected(CLServices services) {
                    Constant.clServices = services;
                    Log.d(TAG, "SDK Connected");
                }

                @Override
                public void serviceDisconnected() {
                    Log.d(TAG, "SDK Disconnected");
                }
            });
        }
    }

    // 🔹 Aadhaar Capture
    private void startAadhaar(String cred, String salt) {

        try {
            Log.d(TAG, "Calling Aadhaar SDK...");

            Constant.clServices.getCredential(
                    "EKYC",     // keyCode
                    "",         // xmlPayload
                    cred,       // controls (AADHAR)
                    "",         // config
                    salt,       // salt
                    "",         // trust
                    "",         // payInfo
                    "en_US",    // language
                    getReceiver("AADHAAR")
            );

        } catch (Exception e) {
            Log.e(TAG, "Aadhaar Error", e);
            callbackContext.error("AADHAAR ERROR: " + e.getMessage());
        }
    }

    // 🔹 FaceAuth Capture
    private void faceAuth(String cred, String salt) {

        try {
            Log.d(TAG, "Calling FaceAuth SDK...");

            Constant.clServices.getCredential(
                    "EKYC",
                    "",
                    cred,       // controls (FACE_AUTH)
                    "",
                    salt,
                    "",
                    "",
                    "en_US",
                    getReceiver("FACE")
            );

        } catch (Exception e) {
            Log.e(TAG, "FaceAuth Error", e);
            callbackContext.error("FACE ERROR: " + e.getMessage());
        }
    }

    // 🔹 Handle SDK response
    private CLRemoteResultReceiver getReceiver(final String type) {

        return new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {

                try {

                    Log.d(TAG, "ResultCode: " + resultCode);

                    String result = "";

                    if (resultData != null) {
                        result = resultData.toString();
                    }

                    Log.d(TAG, "ResultData: " + result);

                    // Aadhaar Success
                    if ("AADHAAR".equals(type) && resultCode == 2) {
                        callbackContext.success("AADHAAR_SUCCESS:" + result);
                    }

                    // Face Success
                    else if ("FACE".equals(type) && resultCode == 1) {
                        callbackContext.success("FACE_SUCCESS:" + result);
                    }

                    // Error
                    else {
                        callbackContext.error("FAILED CODE: " + resultCode + " DATA: " + result);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Receiver Error", e);
                    callbackContext.error("RECEIVER ERROR: " + e.getMessage());
                }
            }
        });
    }
}
