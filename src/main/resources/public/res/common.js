var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var LayerManager = /** @class */ (function () {
    function LayerManager() {
        this.stack = [];
        this.scrim = document.createElement("div");
        this.scrim.id = "layerScrim";
        hide(this.scrim);
        document.body.appendChild(this.scrim);
        var container = document.createElement("div");
        container.id = "layerContainer";
        hide(container);
        this.layerContainer = container;
        document.body.appendChild(container);
    }
    LayerManager.getInstance = function () {
        if (!LayerManager.instance) {
            LayerManager.instance = new LayerManager();
        }
        return LayerManager.instance;
    };
    LayerManager.prototype.show = function (layer) {
        if (this.stack.length == 0) {
            show(this.scrim);
            show(this.layerContainer);
        }
        else {
            var prevLayer = this.stack[this.stack.length - 1];
            hide(prevLayer.getContent());
            prevLayer.onHidden();
        }
        this.stack.push(layer);
        var layerContent = layer.getContent();
        this.layerContainer.appendChild(layerContent);
        layer.onShown();
    };
    LayerManager.prototype.dismiss = function (layer) {
        var i = this.stack.indexOf(layer);
        if (i == -1)
            return;
        var layerContent = layer.getContent();
        if (isVisible(layerContent)) {
            layer.onHidden();
        }
        this.layerContainer.removeChild(layerContent);
        if (i == this.stack.length - 1) {
            this.stack.pop();
            if (this.stack.length) {
                var newLayer = this.stack[this.stack.length - 1];
                show(newLayer.getContent());
                newLayer.onShown();
            }
        }
        else {
            this.stack.splice(i, 1);
        }
        if (this.stack.length == 0) {
            hide(this.scrim);
            hide(this.layerContainer);
        }
    };
    return LayerManager;
}());
var BaseLayer = /** @class */ (function () {
    function BaseLayer() {
    }
    BaseLayer.prototype.show = function () {
        if (!this.content) {
            this.content = document.createElement("div");
            this.content.className = "layerContent";
            var contentView = this.onCreateContentView();
            this.content.appendChild(contentView);
        }
        LayerManager.getInstance().show(this);
    };
    BaseLayer.prototype.dismiss = function () {
        LayerManager.getInstance().dismiss(this);
    };
    BaseLayer.prototype.getContent = function () {
        return this.content;
    };
    BaseLayer.prototype.onShown = function () { };
    BaseLayer.prototype.onHidden = function () { };
    return BaseLayer;
}());
var Box = /** @class */ (function (_super) {
    __extends(Box, _super);
    function Box(title, buttonTitles, onButtonClick) {
        if (buttonTitles === void 0) { buttonTitles = []; }
        if (onButtonClick === void 0) { onButtonClick = null; }
        var _this = _super.call(this) || this;
        _this.title = title;
        _this.buttonTitles = buttonTitles;
        _this.onButtonClick = onButtonClick;
        var contentWrap = document.createElement("div");
        contentWrap.className = "boxContent";
        _this.contentWrap = contentWrap;
        return _this;
    }
    Box.prototype.onCreateContentView = function () {
        var content = document.createElement("div");
        content.className = "boxLayer";
        var titleBar = document.createElement("div");
        titleBar.innerText = this.title;
        titleBar.className = "boxTitleBar";
        this.titleBar = titleBar;
        content.appendChild(titleBar);
        content.appendChild(this.contentWrap);
        if (this.buttonTitles.length) {
            var buttonBar = document.createElement("div");
            buttonBar.className = "boxButtonBar";
            for (var i = 0; i < this.buttonTitles.length; i++) {
                var btn = document.createElement("input");
                btn.type = "button";
                btn.value = this.buttonTitles[i];
                if (i > 0) {
                    btn.className = "secondary";
                }
                if (this.onButtonClick) {
                    btn.onclick = this.onButtonClick.bind(this, i);
                }
                else {
                    btn.onclick = this.dismiss.bind(this);
                }
                buttonBar.appendChild(btn);
            }
            content.appendChild(buttonBar);
            this.buttonBar = buttonBar;
        }
        return content;
    };
    Box.prototype.setContent = function (content) {
        this.contentWrap.appendChild(content);
    };
    Box.prototype.getButton = function (index) {
        return this.buttonBar.children[index];
    };
    return Box;
}(BaseLayer));
var ConfirmBox = /** @class */ (function (_super) {
    __extends(ConfirmBox, _super);
    function ConfirmBox(title, msg, onConfirmed) {
        var _this = _super.call(this, title, [lang("yes"), lang("no")], function (idx) {
            if (idx == 0) {
                onConfirmed();
            }
            else {
                this.dismiss();
            }
        }) || this;
        var content = document.createElement("div");
        content.innerHTML = msg;
        _this.setContent(content);
        return _this;
    }
    return ConfirmBox;
}(Box));
var MessageBox = /** @class */ (function (_super) {
    __extends(MessageBox, _super);
    function MessageBox(title, msg, btn) {
        var _this = _super.call(this, title, [btn]) || this;
        var content = document.createElement("div");
        content.innerHTML = msg;
        _this.setContent(content);
        return _this;
    }
    return MessageBox;
}(Box));
var FormBox = /** @class */ (function (_super) {
    __extends(FormBox, _super);
    function FormBox(title, c, btn, act) {
        var _this = _super.call(this, title, [btn, lang("cancel")], function (idx) {
            if (idx == 0) {
                var btn = this.getButton(0);
                btn.setAttribute("disabled", "");
                this.getButton(1).setAttribute("disabled", "");
                btn.classList.add("loading");
                setGlobalLoading(true);
                ajaxSubmitForm(this.form, this.dismiss.bind(this));
            }
            else {
                this.dismiss();
            }
        }) || this;
        var content = document.createElement("div");
        _this.form = document.createElement("form");
        _this.form.innerHTML = c;
        _this.form.action = act;
        content.appendChild(_this.form);
        _this.setContent(content);
        return _this;
    }
    return FormBox;
}(Box));
var submittingForm = null;
String.prototype.format = function () {
    var args = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
    }
    var currentIndex = 0;
    return this.replace(/%(?:(\d+)\$)?([ds%])/gm, function (match, g1, g2) {
        if (g2 == "%")
            return "%";
        var index = g1 ? (parseInt(g1) - 1) : currentIndex;
        currentIndex++;
        switch (g2) {
            case "d":
                return Number(args[index]);
            case "s":
                return args[index].toString().escapeHTML();
        }
    });
};
String.prototype.escapeHTML = function () {
    var el = document.createElement("span");
    el.innerText = this;
    return el.innerHTML;
};
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
    xhr.responseType = "json";
    xhr.send(formData.join("&"));
}
function ajaxGet(uri, onDone, onError) {
    var xhr = new XMLHttpRequest();
    if (uri.indexOf("?") != -1)
        uri += "&_ajax=1";
    else
        uri += "?_ajax=1";
    xhr.open("GET", uri);
    xhr.onload = function () {
        onDone(xhr.response);
    };
    xhr.onerror = function (ev) {
        console.log(ev);
        onError();
    };
    xhr.responseType = "json";
    xhr.send();
}
function hide(el) {
    el.style.display = "none";
}
function show(el) {
    el.style.display = "";
}
function isVisible(el) {
    return el.style.display != "none";
}
function lang(key) {
    if (!(key instanceof Array))
        return langKeys[key] ? langKeys[key] : key;
    var _key = key[0];
    if (!langKeys[_key])
        return key.toString().escapeHTML();
    return langKeys[_key].format(key.slice(1));
}
function setGlobalLoading(loading) {
    document.body.style.cursor = loading ? "progress" : "";
}
function ajaxConfirm(titleKey, msgKey, url, params) {
    if (params === void 0) { params = {}; }
    var box;
    box = new ConfirmBox(lang(titleKey), lang(msgKey), function () {
        var btn = box.getButton(0);
        btn.setAttribute("disabled", "");
        box.getButton(1).setAttribute("disabled", "");
        btn.classList.add("loading");
        setGlobalLoading(true);
        params.csrf = userConfig.csrf;
        ajaxPost(url, params, function (resp) {
            setGlobalLoading(false);
            box.dismiss();
            if (resp instanceof Array) {
                for (var i = 0; i < resp.length; i++) {
                    applyServerCommand(resp[i]);
                }
            }
        }, function () {
            setGlobalLoading(false);
            box.dismiss();
            new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
        });
    });
    box.show();
    return false;
}
function ajaxSubmitForm(form, onDone) {
    if (onDone === void 0) { onDone = null; }
    if (submittingForm)
        return false;
    submittingForm = form;
    var submitBtn = form.querySelector("input[type=submit]");
    if (submitBtn)
        submitBtn.classList.add("loading");
    setGlobalLoading(true);
    var data = {};
    var elems = form.elements;
    for (var i = 0; i < elems.length; i++) {
        var el = elems[i];
        if (!el.name)
            continue;
        data[el.name] = el.value;
    }
    data.csrf = userConfig.csrf;
    ajaxPost(form.action, data, function (resp) {
        submittingForm = null;
        if (submitBtn)
            submitBtn.classList.remove("loading");
        setGlobalLoading(false);
        if (resp instanceof Array) {
            for (var i = 0; i < resp.length; i++) {
                applyServerCommand(resp[i]);
            }
        }
        if (onDone)
            onDone();
    }, function () {
        submittingForm = null;
        if (submitBtn)
            submitBtn.classList.remove("loading");
        setGlobalLoading(false);
        new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
        if (onDone)
            onDone();
    });
    return false;
}
function ajaxFollowLink(link) {
    if (link.getAttribute("data-ajax")) {
        setGlobalLoading(true);
        ajaxGet(link.href, function (resp) {
            setGlobalLoading(false);
            if (resp instanceof Array) {
                for (var i = 0; i < resp.length; i++) {
                    applyServerCommand(resp[i]);
                }
            }
        }, function () {
            setGlobalLoading(false);
            new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
        });
        return true;
    }
    return false;
}
function applyServerCommand(cmd) {
    switch (cmd.a) {
        case "remove":
            {
                var ids = cmd.ids;
                for (var i = 0; i < ids.length; i++) {
                    var el = document.getElementById(ids[i]);
                    if (el) {
                        el.parentNode.removeChild(el);
                    }
                }
            }
            break;
        case "setContent":
            {
                var id = cmd.id;
                var content = cmd.c;
                var el = document.getElementById(id);
                if (el) {
                    el.innerHTML = content;
                }
            }
            break;
        case "msgBox":
            new MessageBox(cmd.t, cmd.m, cmd.b).show();
            break;
        case "formBox":
            new FormBox(cmd.t, cmd.m, cmd.b, cmd.fa).show();
            break;
        case "show":
            {
                var ids = cmd.ids;
                for (var i = 0; i < ids.length; i++) {
                    var el = document.getElementById(ids[i]);
                    if (el) {
                        show(el);
                    }
                }
            }
            break;
        case "hide":
            {
                var ids = cmd.ids;
                for (var i = 0; i < ids.length; i++) {
                    var el = document.getElementById(ids[i]);
                    if (el) {
                        hide(el);
                    }
                }
            }
            break;
        case "insert":
            {
                var el = document.getElementById(cmd.id);
                if (!el)
                    return;
                var mode = { "bb": "beforeBegin", "ab": "afterBegin", "be": "beforeEnd", "ae": "afterEnd" }[cmd.m];
                el.insertAdjacentHTML(mode, cmd.c);
            }
            break;
        case "setValue":
            {
                var el = document.getElementById(cmd.id);
                if (!el)
                    return;
                el.value = cmd.v;
            }
            break;
        case "refresh":
            location.reload();
            break;
    }
}
function showPostReplyForm(id) {
    var form = document.getElementById("wallPostForm");
    var replies = document.getElementById("postReplies" + id);
    replies.insertAdjacentElement("afterbegin", form);
    var hidden = document.getElementById("postFormReplyTo");
    hidden.value = id + "";
    var field = document.getElementById("postFormText");
    var name = document.getElementById("post" + id).getAttribute("data-reply-name");
    if (name) {
        if (field.value.length == 0 || (field.hasAttribute("data-reply-name") && field.value == field.getAttribute("data-reply-name"))) {
            field.value = name + ", ";
            field.setAttribute("data-reply-name", name + ", ");
        }
    }
    field.focus();
    return false;
}
var timeZone;
if (window["Intl"]) {
    timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
}
else {
    var offset = new Date().getTimezoneOffset();
    timeZone = "GMT" + (offset > 0 ? "+" : "") + Math.floor(offset / 60) + (offset % 60 != 0 ? (":" + (offset % 60)) : "");
}
if (!userConfig || !userConfig["timeZone"] || timeZone != userConfig.timeZone) {
    ajaxPost("/settings/setTimezone", { tz: timeZone }, function (resp) { }, function () { });
}
document.body.addEventListener("click", function (ev) {
    if (ev.target.tagName == "A") {
        if (ajaxFollowLink(ev.target)) {
            ev.preventDefault();
        }
    }
}, false);
//# sourceMappingURL=common.js.map