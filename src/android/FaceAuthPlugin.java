package com.bank.faceauth;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;

public class FaceAuthPlugin extends CordovaPlugin {

    private static final String TAG = "FaceAuthPlugin";
    private static final int FACE_AUTH_REQUEST = 1001;

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        if ("faceAuth".equals(action)) {

            this.callbackContext = callbackContext;

            try {

                String salt = args.getString(0);
                Log.d(TAG, "Salt received: " + salt);

                startFaceCapture(salt);

            } catch (Exception e) {

                Log.e(TAG, "Error starting FaceAuth", e);
                callbackContext.error(e.getMessage());
            }

            return true;
        }

        return false;
    }

    private void startFaceCapture(String salt) {

        try {

            String pidOptions =
                    "<PidOptions ver=\"1.0\">" +
                    "<Opts env=\"P\" fCount=\"1\" fType=\"2\" iCount=\"0\" iType=\"0\" pCount=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"10000\" otp=\"\" wadh=\"\" posh=\"UNKNOWN\"/>" +
                    "</PidOptions>";

            Intent intent = new Intent("in.gov.uidai.rdservice.face.CAPTURE");

            intent.putExtra("PID_OPTIONS", pidOptions);
            intent.putExtra("salt", salt);

            cordova.startActivityForResult(this, intent, FACE_AUTH_REQUEST);

        } catch (Exception e) {

            Log.e(TAG, "Face capture start error", e);

            if (callbackContext != null) {
                callbackContext.error("Unable to start face capture: " + e.getMessage());
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == FACE_AUTH_REQUEST) {

            try {

                if (data != null) {

                    String pidData = data.getStringExtra("PID_DATA");

                    if (pidData != null && !pidData.isEmpty()) {

                        Log.d(TAG, "PID_DATA received");

                        if (callbackContext != null) {
                            callbackContext.success(pidData);
                        }

                        return;
                    }
                }

                Log.e(TAG, "Face authentication failed or cancelled");

                if (callbackContext != null) {
                    callbackContext.error("Face Authentication Failed or Cancelled");
                }

            } catch (Exception e) {

                Log.e(TAG, "Error reading result", e);

                if (callbackContext != null) {
                    callbackContext.error(e.getMessage());
                }
            }
        }
    }
}
