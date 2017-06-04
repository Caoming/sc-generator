var INVALID_CLASS_NAME = /[^0-9a-zA-Z\_\$]/;
var INVALID_PACKAGE_NAME = /[^0-9a-zA-Z\_\$\.]/;
$(document).ready(function() {

    $("#targetpackage").keyup(function(e) {
        if (!$(this).val() || INVALID_PACKAGE_NAME.test($(this).val())) {
            $(this).parents(".control-group").addClass("error");
        } else {
            $(this).parents(".control-group").removeClass("error");
        }
    });

    $("#classname").keyup(function(e) {
        if (!$(this).val() || INVALID_CLASS_NAME.test($(this).val())) {
            $(this).parents(".control-group").addClass("error");
        } else {
            $(this).parents(".control-group").removeClass("error");
        }
    });

    // var schemaTextArea = document.getElementById("schema");
    // var myCodeMirror= CodeMirror.fromTextArea(schemaTextArea, {
    //     mode: {name: "javascript", json: true},
    //     lineNumbers: true,
    //     matchBrackets: true,
    //     onChange : function() {
    //         if (!$("#download-zip-link").hasClass("hide")) {
    //             $("#download-zip-link").addClass("hide");
    //         }
    //     }
    // });

    var container = document.getElementById('jsoneditor');

    var options = {
        mode: 'code',
        modes: ['code', 'form', 'text', 'tree', 'view'], // allowed modes
        onError: function (err) {
            alert(err.toString());
        },
        onModeChange: function (newMode, oldMode) {
            console.log('Mode switched from', oldMode, 'to', newMode);
        }
    };

    var json = {
        "type":"object",
        "properties": {
            "foo": {
                "type": "string"
            },
            "bar": {
                "type": "integer"
            },
            "baz": {
                "type": "boolean"
            }
        }
    };

    var editor = new JSONEditor(container, options, json);

    function cmainFrame() {
        var hmain = document.getElementById("jsoneditor");
        var bheight = document.documentElement.clientHeight;
        // hmain.style.width = '100%';
        hmain.style.height = (bheight - 250) + 'px';
    }

    cmainFrame();
    window.onresize = function () {
        cmainFrame();
    };

    $("#form").submit(function(e) {
        e.preventDefault();
        return false;
    });

    $("#zip-button").click(function(e) {

        $("#zip-button").button("loading");
        $(".alert").alert("close");
        $("#download-zip-link").addClass("hide");
        schemaTextArea.value = myCodeMirror.getValue();

        $.ajax({
            url: "generator",
            type: "POST",
            data: $("#form").serialize(),
            success: function(data) {
                $("#download-zip-link").attr("href", "data:application/zip;base64," + data);
                $("#download-zip-link").attr("download", $("#classname").val() + "-sources.zip");
                $("#download-zip-link").text($("#classname").val() + "-sources.zip");
                $("#download-zip-link").removeClass("hide");

                $("#zip-button").button("reset");
            },
            error: function(xhr) {
                $("#zip-button").button("reset");
                $("#alert-area").prepend($("<div class='alert alert-error fade in' data-alert>" +
                    "<button type='button' class='close' data-dismiss='alert'>×</button>" +
                    "<strong>There's a problem:</strong> " + xhr.responseText +
                    "</div>"));
            }
        });
    });

    $("#preview-button").click(function(e) {

        $("#preview-button").button("loading");
        $(".alert").alert("close");
        // schemaTextArea.value = myCodeMirror.getValue();
        $("#schema").val(editor.getText());

        $.ajax({
            url: "generator/preview",
            type: "POST",
            data: $("#form").serialize(),
            success: function(data) {
                $("#preview-button").button("reset");
                CodeMirror.runMode(data,
                    "text/x-java",
                    document.getElementById("preview"),
                    {indentUnit:4});
                $("#preview-modal").modal();
            },
            error: function(xhr) {
                $("#preview-button").button("reset");
                $("#alert-area").append($("<div class='alert alert-error fade in' data-alert>" +
                    "<button type='button' class='close' data-dismiss='alert'>×</button>" +
                    "<strong>There's a problem:</strong> " + xhr.responseText +
                    "</div>"));
            }
        });
    });

});