package com.bank.faceauth;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import org.apache.cordova.*;
import org.json.JSONArray;

import org.npci.upi.security.services.*;

public class FaceAuthPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        this.callbackContext = callbackContext;

        if (action.equals("startEkyc")) {

            Activity activity = cordova.getActivity();
            String saltJson = args.optString(0);

            startEkycFlow(activity, saltJson);

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

            return true;
        }

        return false;
    }

    private void startEkycFlow(Activity activity, String saltJson) {

        // ✅ Correct config
        String cred = "{\"CredAllowed\":[{\"type\":\"AADHAAR\",\"subtype\":\"FACE_AUTH\"}]}";

        CLServices.initService(activity, new ServiceConnectionStatusNotifier() {

            @Override
            public void serviceConnected(CLServices services) {

                CLRemoteResultReceiver receiver =
                        new CLRemoteResultReceiver(new ResultReceiver(new Handler()) {

                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {

                                try {

                                    Log.d("EKYC_DEBUG", "ResultCode: " + resultCode);
                                    Log.d("EKYC_DEBUG", "Bundle: " + resultData);

                                    String encryptedAadhaar = "";
                                    String pidData = "";

                                    if (resultData != null) {

                                        // Aadhaar
                                        if (resultData.containsKey("AADHAAR_DATA")) {
                                            encryptedAadhaar = resultData.getString("AADHAAR_DATA");
                                        }
                                        else if (resultData.containsKey("encryptedAadhaar")) {
                                            encryptedAadhaar = resultData.getString("encryptedAadhaar");
                                        }

                                        // PID
                                        if (resultData.containsKey("PID_DATA")) {
                                            pidData = resultData.getString("PID_DATA");
                                        }
                                        else if (resultData.containsKey("PID_DATA_XML")) {
                                            pidData = resultData.getString("PID_DATA_XML");
                                        }
                                        else if (resultData.containsKey("encryptedPid")) {
                                            pidData = resultData.getString("encryptedPid");
                                        }
                                    }

                                    String finalResult = "{"
                                            + "\"encryptedAadhaar\":\"" + encryptedAadhaar + "\","
                                            + "\"pidData\":\"" + pidData + "\""
                                            + "}";

                                    callbackContext.success(finalResult);

                                } catch (Exception e) {
                                    callbackContext.error("Error: " + e.getMessage());
                                }
                            }
                        });

                services.getCredential(
                        "EKYC",
                        "",
                        cred,
                        "",
                        saltJson,
                        "",
                        "",
                        "en_US",
                        receiver
                );
            }

            @Override
            public void serviceDisconnected() {
                callbackContext.error("Service disconnected");
            }
        });
    }
}
