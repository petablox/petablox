// Author: Patrick Mutchler
// This script manages the note taking system 

var notes = {};
var currentMethod = "";
var app = "test"; // TODO - make this work for independent apps

/**** Public interface ****/

// Displays notes for each callee of a given callsite
function showNotes(){
	$(".callee-link").each( function(){
		var chordSig = $(this).attr("chordSig");
		var notes = getNotes(chordSig);
		var html = "</br> <pre>" + notes + "</pre>"
		
		$(this).after(html)
	});
}


// Opens the note editing panel for a given method
function editNotes(method){
	saveNotes();
	
	showElement("#notes_pane")
	hideElement("#notes_viewer")
	showElement("#notes_editor")
	
	text = getNotes(method);
	$("#editor").val(text);
	
	// need to save current method for when we save notes
	currentMethod = method; 
}

// Saves the notes in the note editing panel
function saveNotes(){
	if(currentMethod !== ""){
		notes[currentMethod] = $("#editor").val();
	}
}

/**** Private helper functions ****/

// Saves the current notes in local storage. Notes are saved with timestamps so
// they can be deleted after they expire.
function exportNotes(){
	saveNotes();
	
	notes_str = localStorage.getItem("stamp_notes");
	all_notes = notes_str !== null ? JSON.parse(notes_str) : {};

	all_notes[app] = {
		"notes": JSON.stringify(notes),
		"date": new Date().toString()
	};
	localStorage.setItem("stamp_notes", JSON.stringify(all_notes));
	console.log("Saved: " + JSON.stringify(all_notes))
}

// Retrieves saved notes from local storage.
function importNotes(){
	notes_str = localStorage.getItem("stamp_notes");
	all_notes = notes_str !== null ? JSON.parse(notes_str) : {};
	notes = all_notes[app] ? JSON.parse(all_notes[app]["notes"]) : {};
} 

function getNotes(method){
	if(notes[method])
		return notes[method];
	return "";
}

function clearStorage(){
	localStorage.removeItem("stamp_notes");
	notes = {}
}

/**** DOM management ****/

function showElement(id){
	$(id).addClass("notes_show");
	$(id).removeClass("notes_hide");
}

function hideElement(id){
	$(id).addClass("notes_hide");
	$(id).removeClass("notes_show");
	saveNotes();
}

// Imports notes from local storage when the page is first loaded
// and exports notes to local storage when the page is closed.
$(document).ready(function() {
	window.onunload = exportNotes;
	importNotes();
});


