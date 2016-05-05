$(function ()  
{ $(".mparameter[rel=popover]").popover({ 
        trigger : "hover",
        position: "bottom", 
        html : true,
        title : "Values", 
        content : function() {
            return $(this).find("span.hidden-popover").text();
        }
    });
  $(".mparameter").click(function (e) {
      e.preventDefault();
    });
  $(".mname").click(function (e) {
      e.preventDefault();
      showSource($(this).data("file"), 'false', $(this).data("linenum"), 'instCallReports1');
    });
  $("a.callsite-position").click(function (e) {
      e.preventDefault();
      showSource($(this).data("file"), 'false', $(this).data("linenum"), 'instCallReports2');
    });
  $(".method-plus-button").click(function (e) {
      e.preventDefault();
      $(this).children("i").toggleClass("icon-plus");
      $(this).children("i").toggleClass("icon-minus");
    });
});

$(document).ready(function(){
    $('.collapse')
        .on('shown', function(evnt) {
            $(evnt.target).addClass('collapse-visible');
        })
        .on('hide', function(evnt) {
            $(evnt.target).removeClass('collapse-visible');
        })
    ;
});
