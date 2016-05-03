// Author: Patrick Mutchler
// This script manages recording navigation and the back button.
// Later this script might manage all navigation so we can have
// some modular code.

var stack = [];
var forwardStack = [];

/**** Public methods ****/ 

// Navs to the source location specified by file and ln
function showSource(file, isModelFlag, ln, useJimple){
	recordNav(file, ln)
	internalShowSource(file, isModelFlag, ln, useJimple)
}

function recordNav(file, ln){
	console.log("Recording nav");
	console.log(file + ": " + ln);
	
	// Don't push the same loc on the stack more than once
	/*if(stack.length > 0){
		curr = stack[stack.length - 1];
		if(curr !== undefined && curr["file"] === file && curr["line"] === ln){
			return;
		}
	}*/
	
	forwardStack = [];
	stack.push({
		"file": file,
		"line": ln
	});
	
	manageButtons();
}

function back(){
	if(stack.length > 1){
		curr = stack.pop();
		forwardStack.push(curr);
		
		loc = stack[stack.length - 1];
		internalShowSource(loc["file"], "false", loc["line"], "false");
	}
	manageButtons();
}

function forward(){
	if(forwardStack.length > 0){
		loc = forwardStack.pop();
		stack.push(loc);
		internalShowSource(loc["file"], "false", loc["line"], "false");
	}
	manageButtons();
}


/**** Private methods ****/
function manageButtons(){
	if(stack.length > 1){
		enableBack();
	} else {
		disableBack();
	}

	if(forwardStack.length > 0){
		enableForward();
	} else {
		disableForward();
	}
}

function disableBack(){
	$("#back-button").attr('src', "/stamp/res/backdisabled.png");
}

function enableBack(){
	$("#back-button").attr('src', "/stamp/res/back.png");
}

function disableForward(){
	$("#forward-button").attr('src', "/stamp/res/forwarddisabled.png");
}

function enableForward(){
	$("#forward-button").attr('src', "/stamp/res/forward.png");
}

// Moved from index.jsp by Patrick for sanity
// Navs to the source location specified by file and ln
// We keep the actual navigation in a private method so that we can have calls
// that add to the nav stack or don't. This requires less change to the code
// than adding a new parameter.
function internalShowSource(file, isModelFlag, ln, useJimple){
	// file: com/foo/bar/Baz.java
	// title: Baz.java
	var tabTitle = file.substring(file.lastIndexOf('/') + 1);
	
	var onTabLoad = function (href) {
		$.ajax({
			type: "POST",
			url: "/stamp/html/viewSource.jsp",
			data: { 
				filepath: file, 
				lineNum: ln, 
				isModel: isModelFlag,
				useJimple: useJimple 
			}
		}).done(function (response) {
			showCode(response, href);
			highlightLine(ln, href);
		})
	};
			    
	var onTabDisplay = function (href) {
		var highlightedLine = idToHighlightedLine[href];
		if(typeof highlightedLine !== "undefined"){
			$('#'+href+' ol li:nth-child('+highlightedLine+')').css('backgroundColor','');
		}
		highlightLine(ln, href);
	};
			    
	showContentTab(file, tabTitle, onTabLoad, onTabDisplay);
};

// Moved from index.jsp by Patrick for sanity
function showContentTab(tabUniqueName, tabDisplayName, onTabLoad, onTabDisplay){
    if(!tabUniqueName){
        return;
	}
	
	$('#codetabs').show();
	var href = tabNameToId[tabUniqueName];
	
	// If this tab is not already open
	if(typeof href === "undefined"){
        //add a new tab
        tabCount = $("#codetabs li").size(); 
        var id = totalFilesOpened++;
        href = 'filetab'+id;
        tabNameToId[tabUniqueName] = href;

        var tabTitle = tabDisplayName;
        $('#codetabs').append('<li><a href="#'+href+'" data-toggle="tab">'+tabTitle+'<button class="btn btn-link" id="closetab'+href+'">x</button></a></li>');
        $('#codetabs a:last button').on('click', function(event){
            var tabToClose = $(this).attr('id').substring(8); // 8 = "closetab".length()
            var aNew;
            var liCurrent;
            var href = '#'+tabToClose;
				
            $('#codetabs li a').each(function(){
                if($(this).attr('href') == href){
                    liCurrent = $(this).parent();
                } else if(typeof liNew === "undefined"){
                    aNew = $(this);
                }
            });
			
            //delete the li from #codetabs
            liCurrent.remove(); 

            //delete the div from #codetabcontents
            $('#'+tabToClose).remove(); 				
            if(typeof aNew === "undefined")
                $('#codetabs').hide();
            else{
                //alert(aNew.html());
                aNew.tab('show');
            }
			
            //clean up
            delete idToHighlightedLine[tabToClose];
            for(var tn in tabNameToId) {
                if(tabNameToId[tn] == tabToClose){
                    delete tabNameToId[tn];
                    break;
                }
            }
        });
	    onTabLoad(href);
    } else {
        $('#codetabs li a[href="#'+href+'"]').tab('show');
    }
    onTabDisplay(href);
};


