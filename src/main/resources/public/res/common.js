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
        var buttonBar = document.createElement("div");
        buttonBar.className = "boxButtonBar";
        content.appendChild(buttonBar);
        this.buttonBar = buttonBar;
        this.updateButtonBar();
        return content;
    };
    Box.prototype.setContent = function (content) {
        if (this.contentWrap.hasChildNodes) {
            for (var i = 0; i < this.contentWrap.children.length; i++)
                this.contentWrap.firstChild.remove();
        }
        this.contentWrap.appendChild(content);
    };
    Box.prototype.getButton = function (index) {
        return this.buttonBar.children[index];
    };
    Box.prototype.setButtons = function (buttonTitles, onButtonClick) {
        this.buttonTitles = buttonTitles;
        this.onButtonClick = onButtonClick;
        this.updateButtonBar();
    };
    Box.prototype.updateButtonBar = function () {
        if (this.buttonTitles.length) {
            show(this.buttonBar);
            while (this.buttonBar.firstChild)
                this.buttonBar.lastChild.remove();
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
                this.buttonBar.appendChild(btn);
            }
        }
        else {
            hide(this.buttonBar);
        }
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
var FileUploadBox = /** @class */ (function (_super) {
    __extends(FileUploadBox, _super);
    function FileUploadBox(title, message) {
        if (message === void 0) { message = null; }
        var _this = _super.call(this, title, [lang("cancel")], function (idx) {
            this.dismiss();
        }) || this;
        _this.acceptMultiple = false;
        var content = ce("div");
        if (!message)
            message = lang("drag_or_choose_file");
        content.innerHTML = "<div class='inner'>" + message + "<br/><form><input type='file' id='fileUploadBoxInput' accept='image/*'/><label for='fileUploadBoxInput' class='button'>" + lang("choose_file") + "</label></form></div><div class='dropOverlay'>" + lang("drop_files_here") + "</div>";
        content.className = "fileUploadBoxContent";
        _this.setContent(content);
        _this.fileField = content.querySelector("input[type=file]");
        _this.dragOverlay = content.querySelector(".dropOverlay");
        _this.dragOverlay.addEventListener("dragenter", function (ev) {
            this.dragOverlay.classList.add("over");
        }.bind(_this), false);
        _this.dragOverlay.addEventListener("dragleave", function (ev) {
            this.dragOverlay.classList.remove("over");
        }.bind(_this), false);
        content.addEventListener("drop", function (ev) {
            ev.preventDefault();
            this.dragOverlay.classList.remove("over");
            this.handleFiles(ev.dataTransfer.files);
        }.bind(_this), false);
        _this.fileField.addEventListener("change", function (ev) {
            this.handleFiles(this.fileField.files);
            this.fileField.form.reset();
        }.bind(_this), false);
        return _this;
    }
    FileUploadBox.prototype.handleFiles = function (files) {
        for (var i = 0; i < files.length; i++) {
            var f = files[i];
            if (f.type.indexOf("image/") == 0) {
                this.handleFile(f);
                if (!this.acceptMultiple)
                    return;
            }
        }
    };
    return FileUploadBox;
}(Box));
var ProfilePictureBox = /** @class */ (function (_super) {
    __extends(ProfilePictureBox, _super);
    function ProfilePictureBox() {
        var _this = _super.call(this, lang("update_profile_picture")) || this;
        _this.file = null;
        _this.areaSelector = null;
        return _this;
    }
    ProfilePictureBox.prototype.handleFile = function (file) {
        var _this = this;
        this.file = file;
        var objURL = URL.createObjectURL(file);
        var img = ce("img");
        img.onload = function () {
            var ratio = img.naturalWidth / img.naturalHeight;
            if (ratio > 2.5) {
                new MessageBox(lang("error"), lang("picture_too_wide"), lang("ok")).show();
                return;
            }
            else if (ratio < 0.25) {
                new MessageBox(lang("error"), lang("picture_too_narrow"), lang("ok")).show();
                return;
            }
            var content = ce("div");
            content.innerText = lang("profile_pic_select_square_version");
            content.align = "center";
            var imgWrap = ce("div");
            imgWrap.className = "profilePictureBoxImgWrap";
            imgWrap.appendChild(img);
            content.appendChild(ce("br"));
            content.appendChild(imgWrap);
            _this.setContent(content);
            _this.setButtons([lang("save"), lang("cancel")], function (idx) {
                if (idx == 1) {
                    _this.dismiss();
                    return;
                }
                var area = _this.areaSelector.getSelectedArea();
                var contW = imgWrap.clientWidth;
                var contH = imgWrap.clientHeight;
                var x1 = area.x / contW;
                var y1 = area.y / contH;
                var x2 = (area.x + area.w) / contW;
                var y2 = (area.y + area.h) / contH;
                _this.areaSelector.setEnabled(false);
                _this.upload(x1, y1, x2, y2);
            });
            _this.areaSelector = new ImageAreaSelector(imgWrap, true);
            var w = imgWrap.clientWidth;
            var h = imgWrap.clientHeight;
            if (w > h) {
                _this.areaSelector.setSelectedArea(Math.round(w / 2 - h / 2), 0, h, h);
            }
            else {
                _this.areaSelector.setSelectedArea(0, 0, w, w);
            }
        };
        img.onerror = function () {
            new MessageBox(lang("error"), lang("error_loading_picture"), lang("ok")).show();
        };
        img.src = objURL;
    };
    ProfilePictureBox.prototype.upload = function (x1, y1, x2, y2) {
        var _this = this;
        var btn = this.getButton(0);
        btn.setAttribute("disabled", "");
        this.getButton(1).setAttribute("disabled", "");
        btn.classList.add("loading");
        setGlobalLoading(true);
        ajaxUpload("/settings/updateProfilePicture?x1=" + x1 + "&y1=" + y1 + "&x2=" + x2 + "&y2=" + y2, "pic", this.file, function (resp) {
            _this.dismiss();
            setGlobalLoading(false);
            return false;
        });
    };
    return ProfilePictureBox;
}(FileUploadBox));
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
Array.prototype.remove = function (item) {
    var index = this.indexOf(item);
    if (index == -1)
        return;
    this.splice(index, 1);
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
function ajaxUpload(uri, fieldName, file, onDone, onError, onProgress) {
    if (onDone === void 0) { onDone = null; }
    if (onError === void 0) { onError = null; }
    if (onProgress === void 0) { onProgress = null; }
    var formData = new FormData();
    formData.append(fieldName, file);
    var xhr = new XMLHttpRequest();
    if (uri.indexOf("?") != -1)
        uri += "&";
    else
        uri += "?";
    uri += "_ajax=1&csrf=" + userConfig.csrf;
    xhr.open("POST", uri);
    xhr.onload = function () {
        var resp = xhr.response;
        if (onDone) {
            if (onDone(resp))
                return;
        }
        if (resp instanceof Array) {
            for (var i = 0; i < resp.length; i++) {
                applyServerCommand(resp[i]);
            }
        }
    }.bind(this);
    xhr.onerror = function (ev) {
        console.log(ev);
        if (onError)
            onError();
    };
    xhr.upload.onprogress = function (ev) {
        // pbarInner.style.transform="scaleX("+(ev.loaded/ev.total)+")";
        if (onProgress)
            onProgress(ev.loaded / ev.total);
    };
    xhr.responseType = "json";
    xhr.send(formData);
}
function hide(el) {
    el.style.display = "none";
}
function hideAnimated(el) {
    var f = function () {
        el.style.animation = "";
        el.style.display = "none";
        el.removeEventListener("animationend", f);
    };
    el.addEventListener("animationend", f);
    el.style.animation = "fadeOut 0.2s ease";
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
        if (el.type != "radio" || (el.type == "radio" && el.checked))
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
        case "setAttr":
            {
                var id = cmd.id;
                var value = cmd.v;
                var name = cmd.n;
                var el = document.getElementById(id);
                if (el) {
                    el.setAttribute(name, value);
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
        case "addClass":
            {
                var el = document.getElementById(cmd.id);
                if (!el)
                    return;
                el.classList.add(cmd.cl);
            }
            break;
        case "remClass":
            {
                var el = document.getElementById(cmd.id);
                if (!el)
                    return;
                el.classList.remove(cmd.cl);
            }
            break;
        case "refresh":
            location.reload();
            break;
    }
}
function showPostReplyForm(id) {
    var form = document.getElementById("wallPostForm_reply");
    var replies = document.getElementById("postReplies" + id);
    replies.insertAdjacentElement("afterbegin", form);
    postForms["wallPostForm_reply"].setupForReplyTo(id);
    return false;
}
function highlightComment(id) {
    var existing = document.querySelectorAll(".highlight");
    for (var i = 0; i < existing.length; i++)
        existing[i].classList.remove("highlight");
    ge("post" + id).classList.add("highlight");
    window.location.hash = "#comment" + id;
    return false;
}
function likeOnClick(btn) {
    if (btn.hasAttribute("in_progress"))
        return false;
    var objType = btn.getAttribute("data-obj-type");
    var objID = btn.getAttribute("data-obj-id");
    var liked = btn.classList.contains("liked");
    var counter = ge("likeCounter" + objType.substring(0, 1).toUpperCase() + objType.substring(1) + objID);
    var count = parseInt(counter.innerText);
    if (!liked) {
        counter.innerText = count + 1;
        btn.classList.add("liked");
        if (count == 0)
            show(counter);
        if (btn.popover) {
            if (!btn.popover.isShown())
                btn.popover.show();
            var title = btn.popover.getTitle();
            btn.popover.setTitle(btn.customData.altPopoverTitle);
            btn.customData.altPopoverTitle = title;
        }
    }
    else {
        counter.innerText = count - 1;
        btn.classList.remove("liked");
        if (count == 1) {
            hide(counter);
            if (btn.popover) {
                btn.popover.hide();
            }
        }
        if (btn.popover) {
            var title = btn.popover.getTitle();
            btn.popover.setTitle(btn.customData.altPopoverTitle);
            btn.customData.altPopoverTitle = title;
        }
    }
    btn.setAttribute("in_progress", "");
    ajaxGet(btn.href, function (resp) {
        btn.removeAttribute("in_progress");
        if (resp instanceof Array) {
            for (var i = 0; i < resp.length; i++) {
                applyServerCommand(resp[i]);
            }
        }
    }, function () {
        btn.removeAttribute("in_progress");
        new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
        if (liked) {
            counter.innerText = count + 1;
            btn.classList.add("liked");
            if (count == 0)
                show(counter);
        }
        else {
            counter.innerText = count - 1;
            btn.classList.remove("liked");
            if (count == 1)
                hide(counter);
        }
    });
    return false;
}
function likeOnMouseChange(wrap, entered) {
    var btn = wrap.querySelector(".like");
    var objID = btn.getAttribute("data-obj-id");
    var objType = btn.getAttribute("data-obj-type");
    var ev = event;
    var popover = btn.popover;
    if (entered) {
        if (!btn.customData)
            btn.customData = {};
        btn.customData.popoverTimeout = setTimeout(function () {
            delete btn.customData.popoverTimeout;
            ajaxGet(btn.getAttribute("data-popover-url"), function (resp) {
                if (!popover) {
                    popover = new Popover(wrap.querySelector(".popoverPlace"));
                    btn.popover = popover;
                }
                popover.setTitle(resp.title);
                popover.setContent(resp.content);
                btn.customData.altPopoverTitle = resp.altTitle;
                if (resp.show)
                    popover.show(ev.offsetX, ev.offsetY);
                for (var i = 0; i < resp.actions.length; i++) {
                    applyServerCommand(resp.actions[i]);
                }
            }, function () {
                if (popover)
                    popover.show(ev.offsetX, ev.offsetY);
            });
        }, 500);
    }
    else {
        if (btn.customData.popoverTimeout) {
            clearTimeout(btn.customData.popoverTimeout);
            delete btn.customData.popoverTimeout;
        }
        else if (popover) {
            popover.hide();
        }
    }
}
var ImageAreaSelector = /** @class */ (function () {
    function ImageAreaSelector(parentEl, square) {
        if (square === void 0) { square = false; }
        this.curTarget = null;
        this.enabled = true;
        this.parentEl = parentEl;
        this.container = ce("div");
        this.container.className = "imageAreaSelector";
        this.parentEl.appendChild(this.container);
        this.scrimTop = this.makeDiv("scrim");
        this.scrimTop.style.top = "0";
        this.container.appendChild(this.scrimTop);
        this.scrimBottom = this.makeDiv("scrim");
        this.scrimBottom.style.bottom = "0";
        this.container.appendChild(this.scrimBottom);
        this.scrimLeft = this.makeDiv("scrim");
        this.scrimLeft.style.left = this.scrimLeft.style.top = this.scrimLeft.style.bottom = "0";
        this.container.appendChild(this.scrimLeft);
        this.scrimRight = this.makeDiv("scrim");
        this.scrimRight.style.right = this.scrimRight.style.top = this.scrimRight.style.bottom = "0";
        this.container.appendChild(this.scrimRight);
        this.selected = ce("div");
        this.selected.className = "selected";
        this.container.appendChild(this.selected);
        this.container.addEventListener("mousedown", this.onMouseDown.bind(this), false);
        this.container.addEventListener("dragstart", function (ev) { ev.preventDefault(); }, false);
        var markerCont = ce("div");
        markerCont.className = "markers";
        this.selected.appendChild(markerCont);
        markerCont.appendChild(this.markerTL = this.makeDiv("marker tl"));
        markerCont.appendChild(this.markerTR = this.makeDiv("marker tr"));
        markerCont.appendChild(this.markerBL = this.makeDiv("marker bl"));
        markerCont.appendChild(this.markerBR = this.makeDiv("marker br"));
        if (!square) {
            markerCont.appendChild(this.edgeTop = this.makeDiv("edge top"));
            markerCont.appendChild(this.edgeBottom = this.makeDiv("edge bottom"));
            markerCont.appendChild(this.edgeLeft = this.makeDiv("edge left"));
            markerCont.appendChild(this.edgeRight = this.makeDiv("edge right"));
        }
        this.square = square;
    }
    ImageAreaSelector.prototype.makeDiv = function (cls) {
        var el = ce("div");
        el.className = cls;
        return el;
    };
    ImageAreaSelector.prototype.setSelectedArea = function (x, y, w, h) {
        this.curX = x;
        this.curY = y;
        this.curW = w;
        this.curH = h;
        this.updateStyles();
    };
    ImageAreaSelector.prototype.getSelectedArea = function () {
        return { x: this.curX, y: this.curY, w: this.curW, h: this.curH };
    };
    ImageAreaSelector.prototype.setEnabled = function (enabled) {
        this.enabled = enabled;
    };
    ImageAreaSelector.prototype.updateStyles = function () {
        var contW = Math.round(this.container.clientWidth);
        var contH = Math.round(this.container.clientHeight);
        // Round to avoid rendering artifacts
        var x = Math.round(this.curX);
        var y = Math.round(this.curY);
        var w = Math.round(this.curW);
        var h = Math.round(this.curH);
        this.selected.style.left = x + "px";
        this.selected.style.top = y + "px";
        this.selected.style.width = w + "px";
        this.selected.style.height = h + "px";
        this.scrimTop.style.left = x + "px";
        this.scrimTop.style.width = w + "px";
        this.scrimTop.style.height = y + "px";
        this.scrimBottom.style.left = x + "px";
        this.scrimBottom.style.width = w + "px";
        this.scrimBottom.style.height = (contH - y - h + 1) + "px";
        this.scrimLeft.style.width = x + "px";
        this.scrimRight.style.width = (contW - x - w) + "px";
    };
    ImageAreaSelector.prototype.onMouseDown = function (ev) {
        if (!this.enabled)
            return;
        this.curTarget = ev.target;
        this.downX = ev.clientX;
        this.downY = ev.clientY;
        this.downSelectedX = this.curX;
        this.downSelectedY = this.curY;
        this.downSelectedW = this.curW;
        this.downSelectedH = this.curH;
        window.addEventListener("mouseup", this.mouseUpListener = this.onMouseUp.bind(this), false);
        window.addEventListener("mousemove", this.mouseMoveListener = this.onMouseMove.bind(this), false);
    };
    ImageAreaSelector.prototype.onMouseUp = function (ev) {
        this.curTarget = null;
        window.removeEventListener("mouseup", this.mouseUpListener);
        window.removeEventListener("mousemove", this.mouseMoveListener);
    };
    ImageAreaSelector.prototype.onMouseMove = function (ev) {
        if (!this.curTarget)
            return;
        var dX = ev.clientX - this.downX;
        var dY = ev.clientY - this.downY;
        var contW = this.container.clientWidth;
        var contH = this.container.clientHeight;
        if (this.curTarget == this.selected) {
            this.curX = Math.max(0, Math.min(this.downSelectedX + dX, contW - this.curW));
            this.curY = Math.max(0, Math.min(this.downSelectedY + dY, contH - this.curH));
            this.updateStyles();
        }
        else if (this.curTarget == this.edgeRight) {
            this.curW = Math.max(30, Math.min(this.downSelectedW + dX, contW - this.curX));
            this.updateStyles();
        }
        else if (this.curTarget == this.edgeBottom) {
            this.curH = Math.max(30, Math.min(this.downSelectedH + dY, contH - this.curY));
            this.updateStyles();
        }
        else if (this.curTarget == this.markerBR) {
            this.curW = Math.max(30, Math.min(this.downSelectedW + dX, contW - this.curX));
            this.curH = Math.max(30, Math.min(this.downSelectedH + dY, contH - this.curY));
            if (this.square) {
                this.curW = this.curH = Math.min(this.curH, this.curW);
            }
            this.updateStyles();
        }
        else if (this.curTarget == this.edgeTop) {
            var prevH = this.curH;
            this.curH = Math.max(30, Math.min(this.downSelectedH - dY, this.curY + this.curH));
            this.curY += prevH - this.curH;
            this.updateStyles();
        }
        else if (this.curTarget == this.edgeLeft) {
            var prevW = this.curW;
            this.curW = Math.max(30, Math.min(this.downSelectedW - dX, this.curX + this.curW));
            this.curX += prevW - this.curW;
            this.updateStyles();
        }
        else if (this.curTarget == this.markerTL) {
            var prevW = this.curW;
            this.curW = Math.max(30, Math.min(this.downSelectedW - dX, this.curX + this.curW));
            var prevH = this.curH;
            this.curH = Math.max(30, Math.min(this.downSelectedH - dY, this.curY + this.curH));
            if (this.square) {
                this.curW = this.curH = Math.min(this.curH, this.curW);
            }
            this.curX += prevW - this.curW;
            this.curY += prevH - this.curH;
            this.updateStyles();
        }
        else if (this.curTarget == this.markerTR) {
            this.curW = Math.max(30, Math.min(this.downSelectedW + dX, contW - this.curX));
            var prevH = this.curH;
            this.curH = Math.max(30, Math.min(this.downSelectedH - dY, this.curY + this.curH));
            if (this.square) {
                this.curW = this.curH = Math.min(this.curH, this.curW);
            }
            this.curY += prevH - this.curH;
            this.updateStyles();
        }
        else if (this.curTarget == this.markerBL) {
            this.curH = Math.max(30, Math.min(this.downSelectedH + dY, contH - this.curY));
            var prevW = this.curW;
            this.curW = Math.max(30, Math.min(this.downSelectedW - dX, this.curX + this.curW));
            if (this.square) {
                this.curW = this.curH = Math.min(this.curH, this.curW);
            }
            this.curX += prevW - this.curW;
            this.updateStyles();
        }
    };
    return ImageAreaSelector;
}());
var PostForm = /** @class */ (function () {
    function PostForm(el) {
        this.attachmentIDs = [];
        this.currentReplyName = "";
        this.id = el.getAttribute("data-unique-id");
        this.root = el;
        this.input = ge("postFormText_" + this.id);
        this.form = el.getElementsByTagName("form")[0];
        this.dragOverlay = el.querySelector(".dropOverlay");
        this.attachContainer = ge("postFormAttachments_" + this.id);
        this.fileField = ge("uploadField_" + this.id);
        this.attachField = el.querySelector("input[name=attachments]");
        this.replyToField = ge("postFormReplyTo_" + this.id);
        this.form.addEventListener("submit", this.onFormSubmit.bind(this), false);
        this.input.addEventListener("keydown", this.onInputKeyDown.bind(this), false);
        this.input.addEventListener("paste", this.onInputPaste.bind(this), false);
        if (this.input.hasAttribute("data-reply-name")) {
            this.currentReplyName = this.input.getAttribute("data-reply-name");
        }
        this.dragOverlay.addEventListener("dragenter", function (ev) {
            this.dragOverlay.classList.add("over");
        }.bind(this), false);
        this.dragOverlay.addEventListener("dragleave", function (ev) {
            this.dragOverlay.classList.remove("over");
        }.bind(this), false);
        this.root.addEventListener("drop", this.onDrop.bind(this), false);
        this.fileField.addEventListener("change", function (ev) {
            this.handleFiles(this.fileField.files);
            this.fileField.form.reset();
        }.bind(this), false);
        if (this.attachContainer.children.length) {
            for (var i = 0; i < this.attachContainer.children.length; i++) {
                var attach = this.attachContainer.children[i];
                var aid = attach.getAttribute("data-id");
                this.attachmentIDs.push(aid);
                attach.querySelector(".deleteBtn").onclick = function (ev) {
                    ev.preventDefault();
                    this.deleteAttachment(aid);
                }.bind(this);
            }
        }
        window.addEventListener("beforeunload", function (ev) {
            if ((this.input.value.length > 0 && this.input.value != this.currentReplyName) || this.attachmentIDs.length > 0) {
                var msg = lang("confirm_discard_post_draft");
                (ev || window.event).returnValue = msg;
                return msg;
            }
        }.bind(this));
    }
    PostForm.prototype.onFormSubmit = function (ev) {
        ev.preventDefault();
        this.send();
    };
    PostForm.prototype.onInputKeyDown = function (ev) {
        if (ev.keyCode == 13 && (isApple ? ev.metaKey : ev.ctrlKey)) {
            this.send();
        }
    };
    PostForm.prototype.onInputPaste = function (ev) {
        if (ev.clipboardData.files.length) {
            ev.preventDefault();
            this.handleFiles(ev.clipboardData.files);
        }
    };
    PostForm.prototype.onDrop = function (ev) {
        ev.preventDefault();
        this.dragOverlay.classList.remove("over");
        this.handleFiles(ev.dataTransfer.files);
    };
    PostForm.prototype.handleFiles = function (files) {
        for (var i = 0; i < files.length; i++) {
            var f = files[i];
            if (f.type.indexOf("image/") == 0) {
                this.uploadFile(f);
            }
        }
    };
    PostForm.prototype.uploadFile = function (f) {
        var objURL = URL.createObjectURL(f);
        var cont = ce("div");
        cont.className = "attachment uploading";
        var img = ce("img");
        img.src = objURL;
        cont.appendChild(img);
        var scrim = ce("div");
        scrim.className = "scrim";
        cont.appendChild(scrim);
        var pbar = ce("div");
        pbar.className = "progressBarFrame";
        var pbarInner = ce("div");
        pbarInner.className = "progressBar";
        pbar.appendChild(pbarInner);
        cont.appendChild(pbar);
        var del = ce("a");
        del.className = "deleteBtn";
        del.title = lang("delete");
        del.href = "javascript:void(0)";
        cont.appendChild(del);
        pbarInner.style.transform = "scaleX(0)";
        this.attachContainer.appendChild(cont);
        var formData = new FormData();
        formData.append("file", f);
        var xhr = new XMLHttpRequest();
        xhr.open("POST", "/system/upload/postPhoto?_ajax=1&csrf=" + userConfig.csrf);
        xhr.onload = function () {
            cont.classList.remove("uploading");
            var resp = xhr.response;
            del.href = "/system/deleteDraftAttachment?id=" + resp.id;
            img.outerHTML = '<picture><source srcset="' + resp.thumbs.webp + '" type="image/webp"/><source srcset="' + resp.thumbs.jpeg + '" type="image/jpeg"/><img src="' + resp.thumbs.jpeg + '"/></picture>';
            del.onclick = function (ev) {
                ev.preventDefault();
                this.deleteAttachment(resp.id);
            }.bind(this);
            cont.id = "attachment_" + resp.id;
            this.attachmentIDs.push(resp.id);
            this.attachField.value = this.attachmentIDs.join(",");
        }.bind(this);
        xhr.onerror = function (ev) {
            console.log(ev);
        };
        xhr.upload.onprogress = function (ev) {
            pbarInner.style.transform = "scaleX(" + (ev.loaded / ev.total) + ")";
        };
        xhr.responseType = "json";
        xhr.send(formData);
        del.onclick = function () {
            xhr.abort();
            cont.parentNode.removeChild(cont);
        };
    };
    PostForm.prototype.deleteAttachment = function (id) {
        var el = ge("attachment_" + id);
        el.parentNode.removeChild(el);
        ajaxGet("/system/deleteDraftAttachment?id=" + id, function () { }, function () { });
        this.attachmentIDs.remove(id);
        this.attachField.value = this.attachmentIDs.join(",");
    };
    PostForm.prototype.send = function () {
        if (this.input.value.length == 0 && this.attachmentIDs.length == 0)
            return;
        ajaxSubmitForm(this.form, function () {
            this.attachmentIDs = [];
            this.attachField.value = "";
        }.bind(this));
    };
    PostForm.prototype.setupForReplyTo = function (id) {
        this.replyToField.value = id + "";
        var name = document.getElementById("post" + id).getAttribute("data-reply-name");
        if (name) {
            if (this.input.value.length == 0 || (this.input.value == this.currentReplyName)) {
                this.input.value = name + ", ";
            }
            this.currentReplyName = name + ", ";
        }
        this.input.focus();
    };
    return PostForm;
}());
///<reference path="./PostForm.ts"/>
var ge = document.getElementById.bind(document);
var ce = document.createElement.bind(document);
// Use Cmd instead of Ctrl on Apple devices.
var isApple = navigator.platform.indexOf("Mac") == 0 || navigator.platform == "iPhone" || navigator.platform == "iPad" || navigator.platform == "iPod touch";
var postForms = {};
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
document.querySelectorAll(".wallPostForm").forEach(function (el) {
    postForms[el.id] = new PostForm(el);
});
var dragTimeout = -1;
var dragEventCount = 0;
document.body.addEventListener("dragenter", function (ev) {
    if (ev.dataTransfer.types.indexOf("Files") != -1)
        document.body.classList.add("fileIsBeingDragged");
    ev.preventDefault();
    dragEventCount++;
    if (dragTimeout != -1) {
        clearTimeout(dragTimeout);
        dragTimeout = -1;
    }
}, false);
document.body.addEventListener("dragover", function (ev) {
    ev.preventDefault();
}, false);
document.body.addEventListener("dragleave", function (ev) {
    dragEventCount--;
    if (dragEventCount == 0 && dragTimeout == -1) {
        dragTimeout = setTimeout(function () {
            dragTimeout = -1;
            document.body.classList.remove("fileIsBeingDragged");
            dragEventCount = 0;
        }, 100);
    }
}, false);
document.body.addEventListener("drop", function (ev) {
    if (dragTimeout != -1)
        clearTimeout(dragTimeout);
    dragTimeout = -1;
    dragEventCount = 0;
    document.body.classList.remove("fileIsBeingDragged");
}, false);
///<reference path="./Main.ts"/>
var Popover = /** @class */ (function () {
    function Popover(wrap) {
        this.shown = false;
        this.root = wrap.querySelector(".popover");
        if (!this.root) {
            this.root = ce("div");
            this.root.className = "popover aboveAnchor";
            hide(this.root);
            wrap.appendChild(this.root);
            this.header = ce("div");
            this.header.className = "popoverHeader";
            this.root.appendChild(this.header);
            this.content = ce("div");
            this.content.className = "popoverContent";
            this.root.appendChild(this.content);
            this.arrow = ce("div");
            this.arrow.className = "popoverArrow";
            this.root.appendChild(this.arrow);
        }
    }
    Popover.prototype.show = function (x, y) {
        if (x === void 0) { x = -1; }
        if (y === void 0) { y = -1; }
        this.shown = true;
        show(this.root);
        var anchor = this.root.parentElement;
        var anchorRect = anchor.getBoundingClientRect();
        this.root.classList.remove("belowAnchor", "aboveAnchor");
        if (this.root.offsetHeight > anchorRect.top) {
            this.root.classList.add("belowAnchor");
            this.root.style.top = "";
        }
        else {
            this.root.classList.add("aboveAnchor");
            this.root.style.top = "-" + (this.root.offsetHeight) + "px";
        }
    };
    Popover.prototype.hide = function () {
        this.shown = false;
        hideAnimated(this.root);
    };
    Popover.prototype.setTitle = function (title) {
        this.header.innerHTML = title;
    };
    Popover.prototype.setContent = function (content) {
        this.content.innerHTML = content;
    };
    Popover.prototype.getTitle = function () {
        return this.header.innerHTML;
    };
    Popover.prototype.isShown = function () {
        return this.shown;
    };
    return Popover;
}());
//# sourceMappingURL=common.js.map