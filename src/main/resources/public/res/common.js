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
var TestLayer = /** @class */ (function (_super) {
    __extends(TestLayer, _super);
    function TestLayer() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    TestLayer.prototype.onCreateContentView = function () {
        var el = document.createElement("div");
        var text = "";
        for (var i = 0; i < 5; i++) {
            text += "test layer " + i + "<br/>";
        }
        el.innerHTML = text;
        el.style.width = "500px";
        return el;
    };
    return TestLayer;
}(BaseLayer));
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
        content.innerText = msg;
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
        content.innerText = msg;
        _this.setContent(content);
        return _this;
    }
    return MessageBox;
}(Box));
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
    return langKeys[key] ? langKeys[key] : key;
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
        });
    });
    box.show();
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
                        console.log("removing:", el);
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
    }
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
//# sourceMappingURL=common.js.map