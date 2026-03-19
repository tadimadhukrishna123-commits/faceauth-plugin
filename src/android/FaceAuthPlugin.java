package com.bank.ekyc;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Base64;

import org.apache.cordova.*;
import org.json.JSONArray;

import java.security.SecureRandom;

import org.npci.upi.security.services.*;

public class EkycPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        if (action.equals("captureAadhaar")) {
            captureAadhaar();
            return true;
        } else if (action.equals("captureFaceAuth")) {
            captureFaceAuth();
            return true;
        }

        return false;
    }

    // 🔹 INIT SDK
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

    // 🔹 RANDOM GENERATOR
    private String getRandom() throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    // 🔹 COMMON RECEIVER
    private CLRemoteResultReceiver getReceiver(String type) {
        return new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                try {
                    String result = resultData != null ? resultData.toString() : "";

                    if (type.equals("AADHAAR") && resultCode == 2) {
                        callbackContext.success("AADHAAR_SUCCESS:" + result);
                    } else if (type.equals("FACE") && resultCode == 1) {
                        callbackContext.success("FACE_SUCCESS:" + result);
                    } else {
                        callbackContext.error("FAILED: " + resultCode);
                    }

                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    // 🔹 AADHAAR METHOD
    private void captureAadhaar() {
        try {
            initSDK();

            String random = getRandom();

            String cred = "{\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"AADHAR_NUMBER_AUTH\"}]}";

            String salt = "{"
                    + "\"appId\":\"com.bank.app\","
                    + "\"credType\":[\"aadharNumberAuth\"],"
                    + "\"deviceId\":\"1234567890\","
                    + "\"mobileNumber\":\"919999999999\","
                    + "\"txnId\":[\"TXN123456\"],"
                    + "\"random\":\"" + random + "\""
                    + "}";

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
            callbackContext.error(e.getMessage());
        }
    }

    // 🔹 FACE AUTH METHOD
    private void captureFaceAuth() {
        try {
            initSDK();

            String random = getRandom();

            String cred = "{\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"FACE_AUTH\"}]}";

            String salt = "{"
                    + "\"appId\":\"com.bank.app\","
                    + "\"credType\":[\"faceAuth\"],"
                    + "\"deviceId\":\"1234567890\","
                    + "\"mobileNumber\":\"919999999999\","
                    + "\"txnId\":[\"TXN123456\"],"
                    + "\"random\":\"" + random + "\""
                    + "}";

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
            callbackContext.error(e.getMessage());
        }
    }
}
