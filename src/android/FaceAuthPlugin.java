package com.bank.faceauth;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

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

        Log.d(TAG, "FaceAuth request received");

        this.callbackContext = callbackContext;

        // keep callback alive
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);

        Activity activity = cordova.getActivity();
        String saltJson = args.getString(0);

        try {

            String keyCode = "EKYC";
            String langPref = "en_US";

            String cred = "{\"CredAllowed\":[{\"type\":\"BIOMETRIC\",\"subtype\":\"FACE_AUTH\"}],\"env\":\"PP\"}";

            CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

                @Override
                public void serviceConnected(CLServices services) {

                    Log.d(TAG, "NPCI SDK connected");

                    CLRemoteResultReceiver receiver =
                            new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                                @Override
                                protected void onReceiveResult(int resultCode, Bundle resultData) {

                                    Log.d(TAG, "Result code: " + resultCode);
                                    Log.d(TAG, "Result bundle: " + resultData);

                                    if (resultData == null) {
                                        callbackContext.error("Empty response from SDK");
                                        return;
                                    }

                                    try {

                                        String result;

                                        if (resultData.containsKey("PID_DATA")) {
                                            result = resultData.getString("PID_DATA");
                                        }
                                        else if (resultData.containsKey("PID_DATA_XML")) {
                                            result = resultData.getString("PID_DATA_XML");
                                        }
                                        else {
                                            result = resultData.toString();
                                        }

                                        callbackContext.success(result);

                                    }
                                    catch (Exception e) {

                                        Log.e(TAG, "Result parsing error: " + e.getMessage());
                                        callbackContext.error(e.getMessage());

                                    }

                                }

                            });

                    try {

                        services.getCredential(
                                keyCode,
                                "",
                                cred,
                                "",
                                saltJson,
                                "",
                                "",
                                langPref,
                                receiver
                        );

                    }
                    catch (Exception e) {

                        Log.e(TAG, "getCredential error: " + e.getMessage());
                        callbackContext.error(e.getMessage());

                    }

                }

                @Override
                public void serviceDisconnected() {

                    Log.e(TAG, "NPCI service disconnected");
                    callbackContext.error("SDK disconnected");

                }

            });

        }
        catch (Exception e) {

            Log.e(TAG, "Plugin error: " + e.getMessage());
            callbackContext.error(e.getMessage());

        }

        return true;
    }
}
