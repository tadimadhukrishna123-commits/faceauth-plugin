var exec = require('cordova/exec');

exports.faceAuth = function (salt, success, error) {

    exec(
        success,
        error,
        "FaceAuthPlugin",
        "faceAuth",
        [salt]
    );
};
