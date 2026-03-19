var exec = require('cordova/exec');

// EXISTING (FaceAuth)
exports.faceAuth = function (salt, success, error) {
    exec(success, error, "FaceAuthPlugin", "faceAuth", [salt]);
};

// ✅ NEW (Aadhaar only)
exports.startAadhaar = function (salt, success, error) {
    exec(success, error, "FaceAuthPlugin", "startAadhaar", [salt]);
};

// ✅ OPTIONAL (Full eKYC)
exports.startEkyc = function (salt, success, error) {
    exec(success, error, "FaceAuthPlugin", "startEkyc", [salt]);
};
