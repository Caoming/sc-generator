var executeSqlPop = function () {
    $('#executeSqlPop').modal('toggle')
    // var CodeMirrorEditor = null;
    // if(CodeMirrorEditor==null){
    //     var textarea = document.getElementById('editor');
    //     CodeMirrorEditor = CodeMirror.fromTextArea(textarea, {
    //         lineNumbers: true,     // 显示行数
    //         indentUnit: 4,         // 缩进单位为4
    //         styleActiveLine: true, // 当前行背景高亮
    //         matchBrackets: true,   // 括号匹配
    //         extraKeys: {"Ctrl": "autocomplete"},
    //         mode: "text/x-sql",
    //         lineWrapping: true,    // 自动换行
    //         autocomplete: true,
    //         theme: 'monokai'      // 使用monokai模版
    //     });
    // }
};

var checkLanguage = function () {
    if($("#projectLanguage").val()!=='java'){
        $("#databaseOperationLayer ").find("option").eq(1).removeAttr("selected");
        $("#databaseOperationLayer ").find("option").eq(0).attr("selected","selected");
        $("#databaseOperationLayer").val("jpa");
    }else{
        $("#databaseOperationLayer ").find("option").eq(0).removeAttr("selected");
        $("#databaseOperationLayer ").find("option").eq(1).attr("selected","selected");
        $("#databaseOperationLayer").val("mybatis");
    }
};
