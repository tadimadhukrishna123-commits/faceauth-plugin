var exec = require('cordova/exec');

/**
 * OLD (optional) - FaceAuth only
 */
exports.faceAuth = function (salt, success, error) {

    exec(
        success || function(res){ console.log("FaceAuth success:", res); },
        error || function(err){ console.error("FaceAuth error:", err); },
        "FaceAuthPlugin",
        "faceAuth",
        [salt]
    );
};


/**
 * ✅ NEW - FULL eKYC (Aadhaar + FaceAuth)
 */
exports.startEkyc = function (salt, success, error) {

    if (!salt) {
        console.error("Salt is required");
        if (error) error("Salt is required");
        return;
    }

    exec(
        success || function(res){ console.log("eKYC success:", res); },
        error || function(err){ console.error("eKYC error:", err); },
        "FaceAuthPlugin",
        "startEkyc",   // 👈 IMPORTANT (new action)
        [salt]
    );
};
