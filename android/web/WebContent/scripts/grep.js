// Author: Patrick Mutchler
// This script manages the grep feature

function grep(query){
	$.get(
		'/stamp/html/grep.jsp?query=' + query,
		function (data){
			$("#rightbar").html(data);
		}
	);
}

function goto(i){
	ele = $("#grep" + i);

	loc = ele.attr("loc");
	num = ele.attr("num");
	showSource(loc, false, num, false);
}

// Pressing 'enter' in the grep input field
$(document).ready(function(){ 
	$("#grep-input").keyup(function (e){
		if (e.keyCode == 13) { 
			grep($("#grep-input").val()); 
		}
	});
});