var exec = require('cordova/exec');

// Aadhaar
exports.startAadhaar = function (mobileNumber, success, error) {

    var cred = JSON.stringify({
        CredAllowed: [{
            type: "BIOMETRIC",
            subtype: "AADHAR_NUMBER_AUTH"
        }]
    });

    var salt = buildSalt("aadharNumberAuth", mobileNumber);

    exec(success, error, "FaceAuthPlugin", "startAadhaar", [cred, salt]);
};


// FaceAuth
exports.faceAuth = function (mobileNumber, success, error) {

    var cred = JSON.stringify({
        CredAllowed: [{
            type: "BIOMETRIC",
            subtype: "FACE_AUTH"
        }]
    });

    var salt = buildSalt("faceAuth", mobileNumber);

    exec(success, error, "FaceAuthPlugin", "faceAuth", [cred, salt]);
};


// Salt builder
function buildSalt(type, mobileNumber) {
    return JSON.stringify({
        appId: "com.bank.app",
        credType: [type],
        deviceId: "", // Java lo fill avthundi
        mobileNumber: mobileNumber,
        txnId: ["TXN123456"],
        random: generateRandom()
    });
}

// Random
function generateRandom() {
    return Math.random().toString(36).substring(2, 18);
}
