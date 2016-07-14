// Author Jason Franklin jfrankli@cs.stanford.edu


//Iterate over source sink flows and add data to DOM
function processFlows(flowXML, apkName){
    var xml = flowXML;
    xmlDoc = $.parseXML(xml),
    $xml = $(xmlDoc);
    var id = "flows" + apkName; 

    var highSeverityFlows = new Array();
    var lowSeverityFlows = new Array();

    $('#accordionFlows').find('table[id="'+id+'"]').append("<thead><tr><th>Source</th><th>Source Class</th><th>Sink</th><th>Sink Class</th></tr></thead>");


    $xml.find("tuple").each(function (highSeverityFlows, lowSeverityFlows) {
        var text = $(this).text();
        var sourceText = $(this).find("source").text().replace(/\$/g,'').toLowerCase();
        var sourceClass = $(this).find("source").attr('class');
        var sinkText = $(this).find("sink").text().replace(/\!/g,'').toLowerCase();
        var sinkClass = $(this).find("sink").attr('class');

	// Rank flows based on severity 
        if (text) {

            if (sinkClass == "offdevice" && (sourceClass == "personal data" || sourceClass == "location")) {
		//highSeverityFlows[0] = sourceText;

                $('#accordionFlows').find('table[id="'+id+'"]').prepend("<tr class=\"error\"><td>" + sourceText + "</td><td>" + sourceClass + "</td><td>" + sinkText + "</td><td>" + sinkClass + "</td></tr>"); 
            } else {
                $('#accordionFlows').find('table[id="'+id+'"]').append("<tr><td>" + sourceText +  "</td><td>" + sourceClass + "</td><td>" + sinkText + "</td><td>" + sinkClass + "</td></tr>");
            }
        }
    });

}


//Categorize and display warnings
function processWarnings(message){
    var t = message.data.split("::");
    var warnings = t[2].split("DANGER_METHOD:");
    var numOther = 0;
    var warnHash = {};

    for (var i = 0; i < warnings.length; i++) {
	if (warnings[i] && warnings[i].trim() != '')  {

            var warnTuple = warnings[i].split(";;");

            if (warnHash[warnTuple[1]]) {
		warnHash[warnTuple[1]]++;
            } else {
		warnHash[warnTuple[1]] = 1;
            }
        }
    }
    
    for (var i in warnHash) {
        $('#warnings').append("<tr><td><span class=\"label label-warning\">" 
			      + i + "</span></td><td><span class=\"badge\">" 
			      + warnHash[i] + "</span></td></tr>");		

    }
}

// Process JSON flows
function processFlowJSON(flow) {

    function newPrivTableEntry(entry) {
        return "<tr><td>"+entry.sourceLabel+"</td><td><i class=\"icon-arrow-right\"></i></td><td>"+entry.sinkLabel+"</td> <td><i  onClick=\"window.alert('Accepted')\" class=\"icon-ok\"></i></td> \ <td><i  onClick=\"window.alert('Reject')\" class=\"icon-ban-circle\"></i></td> \ </tr> ";
    }

    function newTableEntry(entry) {
        return "<tr><td>"+entry.sourceLabel+"</td><td><i class=\"icon-arrow-right\"></i></td><td>"+entry.sinkLabel+"</td><td><span class=\"label label-success\">"+entry.modifier+"</span></td> \ <td><i  onClick=\"\" class=\"icon-ok\"></i></td> \ <td><i  onClick=\"\" class=\"icon-ban-circle\"></i></td> \ </tr> ";
    }

    function newTableEntryUnencrypted(entry) {
        return "<tr><td>"+entry.sourceLabel+"</td><td><i class=\"icon-arrow-right\"></i></td><td>"+entry.sinkLabel+"</td><td><span class=\"label label-important\">"+entry.modifier+"</span></td> \ <td><i  onClick=\"\" class=\"icon-ok\"></i></td> \ <td><i  onClick=\"\" class=\"icon-ban-circle\"></i></td> \ </tr> ";
    }

    var maxC = -1;
    var apkName = "";
    var privacyCount = -1;
    var lowRiskCount = -1;
    var confCount = -1;
    $.each(flow, function(i, item) {
	if ('lowRiskCount' in item) {
	    lowRiskCount = item.lowRiskCount;
	}

	if ('privacyCount' in item) {
	    privacyCount = item.privacyCount;
	}

	if (parseInt(item.analysisCounter) > maxC) {
	    maxC = item.analysisCounter;
	    apkName = item.appName;
	}
    });

    // Report header
    var headerRow = "<th><h3>" + apkName +  " Risk Report &nbsp</h3></th>";
    $("#reportheader").append(headerRow);

    // Incident counts
    $("#incident-summary").append("<th><h4>Risk Type</h4></th><th><h4>Incidents</h4></th>");
    $("#incident-summary").append("<tr><td><h5>Privacy</h5></td></td><td>" + privacyCount + "</td></tr>");
    $("#incident-summary").append("<tr><td><h5>Confidentiality</h5></td></td><td>" + privacyCount + "</td></tr>");
    $("#incident-summary").append("<tr><td><h5>Integrity</h5></td></td><td>" + lowRiskCount + "</td></tr>");
    //$("#incident-summary").append("<tr><td>Warnings</td></td><td>" + "X" + "</td></tr>");

    // Section headers
    $("#privacy-rpt").append("<th colspan=\"5\"><h4>Privacy Report - Data sent off device</h4></th>");
    $("#privacy-rpt").append("<tr><td><h4>Data</h4></td><td></td><td><h4>Destination</h4></td><td><h4>Approve</h4></td><td><h4>Reject</h4></td></tr>");
    $("#conf-rpt").append("<th colspan=\"7\"><h4>Confidentiality Report - Data encryption status</h4></th>");
    $("#conf-rpt").append("<tr><td><h4>Data</h4></td><td></td><td><h4>Destination</h4></td><td></td><td><h4>Approve</h4></td><td><h4>Reject</h4></td></tr>");



    if (privacyCount == 0) {
	$("#privacy-rpt").append("<tr colspan=\"7\"><h3>No Privacy Risks Detected!</h3></tr>");
	$("#conf-rpt").append("<tr colspan=\"7\"><h3>No Confidentiality Risks Detected!</h3></tr>");
    }

    if (lowRiskCount != 0) {
	$("#lowrisk-rpt").append("<th colspan=\"7\"><h4>Integrity Report - Data accessed by application</h4></th>")
	$("#lowrisk-rpt").append("<tr><td><h4>Data</h4></td><td></td><td><h4>Destination</h4></td><td></td><td><h4>Approve</h4></td><td><h4>Reject</h4></td></tr>");
    }
			  
    $.each(flow, function(i, item) {
        if (item.analysisCounter === maxC) {

	    var newentry;
	    if (item.modifier === "encrypted") {
		newentry = newTableEntry(item); 
	    } else {
		newentry = newTableEntryUnencrypted(item); 
	    }

            var flowC = item.flowClass;

	    if (flowC === "privacy") {
                $("#privacy-rpt").append(newPrivTableEntry(item));
                $("#conf-rpt").append(newentry);
            } else if (flowC === "integrity") {
                $("integrity-rpt").append(newentry);
            } else if (flowC === "other") {
                $("#lowrisk-rpt").append(newentry);
            } else if (flowC === "NoClass" || flowC === "") {
                // explicit no class. Treat as low-risk.
                $("#lowrisk-rpt").append(newentry);

            } else {
                // unknown flowClass. Treat as low-risk.
                $("#lowrisk-rpt").append(newentry);
                console.log("unknown flow class" + flowC);
            }

        }
    });
}   


// Process messages from server
function processMessage(message){

    function newAccordionGroup(apkName) {
        return "<div class=\"accordion-group\"><div class=\"accordion-heading\"><a class=\"accordion-toggle\" data-toggle=\"collapse\" data-parent=\"#accordionFlows\" href=\"#collapse" + apkName + "\">" + apkName + " Flows</a></div><div id=\"collapse" + apkName + "\" class=\"accordion-body collapse in\"><div class=\"accordion-inner\"><table class=\"table table-condensed table-hover \" id=\"flows" + apkName + "\"></table></div></div></div>";
    }


    if(message.data == "Hello!")
    	return;

    console.log(message.data);

    // Parse message 
    var tokens = message.data.split("::");
    var action = tokens[0];
    var tkns = tokens[1].split('.apk');
    var apkName = tkns[0] + ".apk";
    var apkId = tokens[1];
    var rowElem, labElem, flowJSON;

    // get JSON data
    if(action == "Flow") {
        flowJSON = tokens[2];

    } else {

        // Create new apk status box
        if (action == "BEGIN") {

            $('#warnings').append("<tr><td colspan=\"2\"><h5>" + apkName + " Warnings</h5></td></tr>");
        }

        rowElem = apkIdToRow[apkId];
        labElem = rowElem.find('.label');
    }

    // Process based on message type. The first three simply 
    // adjust the analysis status box
    // WARN and Flow are handled more specifically 
    if(action === "BEGIN")
        labElem.addClass('label-info').text("Analyzing");
    else if(action === "END") {
        labElem.removeClass('label-info').addClass('label-success').text("Finished");
    } else if(action === "ERROR")
        labElem.removeClass('label-info').addClass('label-important').text("Error");

    else if(action === "WARN") 
        processWarnings(message);
    else if (action === "Flow") {
        // is it JSON? probably need action flag for this; we lost the flow one
        flow = $.parseJSON(flowJSON);
        processFlowJSON(flow);
    }
}

function closeConnect(){
    ws.close();
}

