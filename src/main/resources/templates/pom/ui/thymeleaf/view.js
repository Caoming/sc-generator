var edit = function (id) {
    modalPop()
};

var add = function () {
    modalPop()
};

var del =function (id) {

};

var modalPop = function () {
    var modal ='${varModelName}Pop';
    $('#'+modal).modal('toggle')
};