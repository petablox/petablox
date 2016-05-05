// Author: Patrick Mutchler
// Scripts for managing the manifest analysis results

function analyzeManifest(){
	console.log("foo");
	
	$.getJSON(
		'/stamp/html/manifestData.jsp',
		function (data){
			buildManifestPanel(data); 
		}
	);	
}

function buildManifestPanel(data){
	//$("#leftbar").html(data);
	//$("#leftbar").show();
	
	//console.log("bar");
}