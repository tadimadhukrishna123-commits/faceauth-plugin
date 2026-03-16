package com.bank.faceauth;

import android.app.Activity;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;

public class FaceAuthPlugin extends CordovaPlugin {

    private static final int FACE_AUTH_REQUEST = 1001;
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        if (action.equals("faceAuth")) {

            this.callbackContext = callbackContext;

            try {

                String salt = args.getString(0);

                String pidOptions =
                        "<PidOptions ver=\"1.0\">" +
                        "<Opts env=\"S\" fCount=\"1\" fType=\"2\" iCount=\"0\" iType=\"0\" pCount=\"0\" format=\"0\" pidVer=\"2.0\" timeout=\"10000\"/>" +
                        "</PidOptions>";

                Intent intent = new Intent("in.gov.uidai.rdservice.face.CAPTURE");

                intent.putExtra("PID_OPTIONS", pidOptions);
                intent.putExtra("salt", salt);

                cordova.startActivityForResult(this, intent, FACE_AUTH_REQUEST);

            } catch (Exception e) {

                callbackContext.error(e.getMessage());

            }

            return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == FACE_AUTH_REQUEST) {

            if (resultCode == Activity.RESULT_OK) {

                String pidData = data.getStringExtra("PID_DATA");

                if (callbackContext != null) {
                    callbackContext.success(pidData);
                }

            } else {

                if (callbackContext != null) {
                    callbackContext.error("Face Authentication Cancelled");
                }

            }
        }
    }
}
