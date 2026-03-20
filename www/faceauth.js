var exec = require('cordova/exec');

exports.startAadhaar = function (mobile, success, error) {

    var cred = JSON.stringify({
        CredAllowed: [{
            type: "BIOMETRIC",
            subtype: "AADHAR_NUMBER_AUTH"
        }]
    });

    var salt = JSON.stringify({
        appId: "com.bank.app",
        credType: ["aadharNumberAuth"],
        deviceId: "",
        mobileNumber: mobile,
        txnId: ["TXN123456"],
        random: Math.random().toString(36).substring(2)
    });

    exec(success, error, "FaceAuthPlugin", "startAadhaar", [cred, salt]);
};

exports.faceAuth = function (mobile, success, error) {

    var cred = JSON.stringify({
        CredAllowed: [{
            type: "BIOMETRIC",
            subtype: "FACE_AUTH"
        }]
    });

    var salt = JSON.stringify({
        appId: "com.bank.app",
        credType: ["faceAuth"],
        deviceId: "",
        mobileNumber: mobile,
        txnId: ["TXN123456"],
        random: Math.random().toString(36).substring(2)
    });

    exec(success, error, "FaceAuthPlugin", "faceAuth", [cred, salt]);
};
