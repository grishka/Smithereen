function ajaxPost(uri, params, onDone, onError) {
    var xhr = new XMLHttpRequest();
    xhr.open("POST", uri);
    xhr.onload = function () {
        onDone(xhr.response);
    };
    xhr.onerror = function (ev) {
        console.log(ev);
        onError();
    };
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    var formData = [];
    for (var key in params) {
        formData.push(key + "=" + encodeURIComponent(params[key]));
    }
    formData.push("_ajax=1");
    xhr.send(formData.join("&"));
}
var userConfig = window.userConfig;
var timeZone;
if (Intl) {
    timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
}
else {
    var offset = new Date().getTimezoneOffset();
    timeZone = "GMT" + (offset > 0 ? "+" : "-") + Math.floor(offset / 60) + (offset % 60 != 0 ? (":" + (offset % 60)) : "");
}
if (timeZone != userConfig.timeZone) {
    ajaxPost("/settings/setTimezone", { tz: timeZone }, function (resp) { }, function () { });
}
//# sourceMappingURL=common.js.map