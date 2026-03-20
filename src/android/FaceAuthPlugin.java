package com.bank.faceauth;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;

import org.npci.upi.security.services.*;

public class FaceAuthPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;
    private static final String TAG = "FaceAuthPlugin";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        this.callbackContext = callbackContext;

        try {
            String cred = args.getString(0);
            String salt = args.getString(1);

            salt = enrichSalt(salt);
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
            callbackContext.error("ERROR: " + e.getMessage());
        }

        return false;
    }

    private String enrichSalt(String salt) throws Exception {

        JSONObject json = new JSONObject(salt);

        String deviceId = Settings.Secure.getString(
                cordova.getActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        json.put("deviceId", deviceId);

        return json.toString();
    }

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

    private void startAadhaar(String cred, String salt) {

        Constant.clServices.getCredential(
                "EKYC", "", cred, "", salt, "", "", "en_US",
                getReceiver("AADHAAR")
        );
    }

    private void faceAuth(String cred, String salt) {

        Constant.clServices.getCredential(
                "EKYC", "", cred, "", salt, "", "", "en_US",
                getReceiver("FACE")
        );
    }

    private CLRemoteResultReceiver getReceiver(String type) {

        return new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {

                String result = resultData != null ? resultData.toString() : "";

                if ("AADHAAR".equals(type) && resultCode == 2) {
                    callbackContext.success("AADHAAR_SUCCESS:" + result);
                }
                else if ("FACE".equals(type) && resultCode == 1) {
                    callbackContext.success("FACE_SUCCESS:" + result);
                }
                else {
                    callbackContext.error("FAILED:" + resultCode);
                }
            }
        });
    }
}
