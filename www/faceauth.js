var exec = require('cordova/exec');

exports.faceAuth = function (salt, success, error) {

    if (!salt) {
        console.error("Salt is required");
        if (error) error("Salt is required");
        return;
    }

    if (!success) {
        success = function (res) {
            console.log("FaceAuth success:", res);
        };
    }

    if (!error) {
        error = function (err) {
            console.error("FaceAuth error:", err);
        };
    }

    console.log("Calling FaceAuthPlugin with salt:", salt);

    exec(
        success,
        error,
        "FaceAuthPlugin",
        "faceAuth",
        [salt]
    );
};
