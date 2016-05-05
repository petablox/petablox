<!DOCTYPE html>
<html lang="en" class="fuelux">
	<head>
	<%@ page import="java.io.*,java.util.*,stamp.reporting.QueryResults,stamp.droidrecordweb.DroidrecordProxyWeb"%>
  <%
	if(!session.isNew()){
		session.invalidate();
		session = request.getSession();
	}
  
  	String propertyFile = request.getParameter("propertyfile");
  	Properties props = new Properties();
  	try{
  		props.load(new FileInputStream(propertyFile));
  	}catch(IOException e){
  		out.println(e.getMessage());
  	}
  	
     String rootPath = props.getProperty("rootPath");
     String outPath = props.getProperty("outPath");
     String appPath = props.getProperty("appPath");
     String srcPath = props.getProperty("srcPath");
     String libPath = props.getProperty("libPath");
	 String apiLevel = props.getProperty("apiLevel");
	 
     String dr_log_template = props.getProperty("stamp.droidrecord.logfile.template");
     String dr_log_bin = props.getProperty("stamp.droidrecord.logfile.bin");
     DroidrecordProxyWeb droidrecord = new DroidrecordProxyWeb(dr_log_template, dr_log_bin);

     System.out.println("DEBUG: " + outPath);
     boolean useJimple = outPath.matches("^.*\\.apk$");
     System.out.println("DEBUG: boolean useJimple = " + useJimple);

     session.setAttribute("rootPath", rootPath);
     session.setAttribute("appPath", appPath);
     session.setAttribute("outPath", outPath);
     session.setAttribute("srcPath", srcPath);
     session.setAttribute("libPath", libPath);
     session.setAttribute("droidrecord", droidrecord);
     session.setAttribute("useJimple", useJimple);
	 session.setAttribute("apiLevel", apiLevel);

     System.out.println("srcPath = "+srcPath);
     
     session.setAttribute("msg", new LinkedList<String>());

     QueryResults qr2 = (QueryResults)session.getAttribute("qr");
     if(qr2 == null) {
          qr2 = new QueryResults();
          session.setAttribute("qr", qr2);
     }

        int numFlows = 0;
	SortedMap<String,String> titleToFileName = new TreeMap();     
	try {
		File reportTxt = new File(outPath+"/reports.txt");
		BufferedReader reader = new BufferedReader(new FileReader(reportTxt));
		String line; 
		while((line = reader.readLine()) != null){
			int i = line.lastIndexOf(" ");
			String fileName = line.substring(i+1);
			String title = line.substring(0,i);
			titleToFileName.put(title, fileName);
                        if(title.equals("Source-to-sink Flows")) {
                                String results = qr2.querySrcSinkFlows(fileName);
                                numFlows = results.split(",").length;
                        }
		}
	} catch(IOException e) {
		out.println(e.getMessage());
	}

	int tabCount = numFlows + titleToFileName.size() + 4 /* app, model, framework, jimple */;
  %>	  	
		<link href="/stamp/fuelux/css/fuelux.min.css" rel="stylesheet" />
		
		<style type="text/css">
			body {
				padding-top: 60px;
				padding-bottom: 20px;
    		}
    		
     		.source-view {height:80vh; overflow:auto; resize:auto;}
     		.right-view {overflow:auto; }
     		
     		.fuelux .nav-tabs > li > a {
     			padding-top: 0px;
     			padding-bottom: 5px;
     			line-height: 10px;
     		}
     		
     		.linenums > li {
     			white-space:pre;
     		}
     		
     		.result-container { 
 				position: relative;
				padding: 10px 15px 0 15px;
				overflow-x: hidden;
				overflow-y: auto;
				border: 1px solid #BBBBBB;
				border-radius: 4px 4px 4px 4px;
			}
     		
     		.src-ln-covered {
                background-color: rgba(255,255,0,0.2);
            }
  		</style>
  		
		<link href="/stamp/fuelux/css/fuelux-responsive.min.css" rel="stylesheet" />
		<link href="/stamp/css/prettify.css" type="text/css" rel="stylesheet"/> 
		<link href="/stamp/css/notes.css" type="text/css" rel="stylesheet"/> 
		<link href="/stamp/css/navbar.css" type="text/css" rel="stylesheet"/>     
	</head>

	<body>
		<%@include file="navbar.jsp"%>
		
		<div class="container-fluid">
			<div class="row-fluid">
 				<div class="span3" id="leftbar">
					<!--Sidebar content-->
					<%@include file="leftbar.jsp" %>
				</div>
				<div class="span6" id="centerpane">
					<ul class="nav nav-tabs" id="codetabs">
						<li style="padding-right:5px; padding-top:5px">
							<img id="back-button" src="/stamp/res/backdisabled.png" style="height:20px" onclick="back()"></img>
						</li>
						<li style="padding-right:5px; padding-top:5px">
							<img id="forward-button" src="/stamp/res/forwarddisabled.png" style="height:20px" onclick="forward()"></img>
						</li>
					<!--span class="label label-info" id="filename">
					</span>
					<div class="source-view" id="codeview">
					</div-->
					</ul>
					<div class="tab-content" id="codetabcontents">
					</div>
				</div>
				<div class="span3 right-view" id="rightside">
					<div id="rightbar">
					</div>
					<div id="notes_pane" style="padding-bottom:20px; width:100%" class="notes_hide">
						
						<div id="notes_header">
							<span id="notes_title" class="label label-info"> notes </span> 
							<!--<button type="button" class="close" onclick="toggleView()" id="notes_button" aria-hidden="true">&or;</button>-->
							</br>
						</div>
						
						<div id="notes_body" style="width:100%">
							<div id="notes_editor" style="width:100%" class="notes_hide">
								<textarea id="editor" class="input-block-level" rows="6"></textarea>
							</div>
						
							<div id="notes_viewer" class="notes_hide" style="padding-top: 10px; width:100%">
							</div>
						</div>
						
					</div>
				</div>
			</div>
		</div>
			
		<script src="/stamp/jquery/1.8.2/jquery.min.js" type="text/javascript"></script>
		<script src="/stamp/fuelux/loader.js" type="text/javascript"></script>
		<script src="/stamp/scripts/prettify.js" type="text/javascript"></script>
		<script src="/stamp/scripts/viewSource.js" type="text/javascript"></script>
		<script src="/stamp/scripts/notes.js" type="text/javascript"></script>
		<script src="/stamp/scripts/nav.js" type="text/javascript"></script>
		<script src="/stamp/scripts/manifest.js" type="text/javascript"></script>
		<script src="/stamp/scripts/grep.js" type="text/javascript"></script>
		
		<script>
		$('#codetabs').hide();
		</script>
		
		<script>		
			var numFlows = <%=numFlows%>;
			function showTab(index){
				for(j = 0; j < <%=tabCount%>; j++){
					if(index == j){
						$("#leftbartab-"+j).show();
					} else {
						$("#leftbartab-"+j).hide();
					}
				}
			}
			showTab(-1);
			
			<%
			for(i = 0; i < tabCount; i++){
			%>
				$("#showtab-<%=i%>").click(function(e){
					e.preventDefault();
					showTab(<%=i%>);
				});
			<%
			}
			%>
		</script>
		
		<script>
                    /* Trick for on show event from 
                       http://stackoverflow.com/questions/1225102/jquery-event-to-trigger-action-when-a-div-is-made-visible */
                      var _oldShow = $.fn.show;

                      $.fn.show = function(speed, oldCallback) {
                        return $(this).each(function() {
                          var obj         = $(this),
                              newCallback = function() {
                                if ($.isFunction(oldCallback)) {
                                  oldCallback.apply(obj);
                                }
                              };

                          // you can trigger a before show if you want
                          obj.trigger('beforeShow');

                          // now use the old function to show the element passing the new callback
                          _oldShow.apply(obj, [speed, newCallback]);
                          obj.trigger('afterShow');
                        });
                      };

		  function contract(b,id) {
		    document.getElementById(id).style.display = "none";
		    b.innerHTML = "Expand";
		    b.setAttribute("onclick", "expand(this,'"+id+"')");
		  }

		  function expand(b,id) {
		    document.getElementById(id).style.display = "";
		    b.innerHTML = "Contract";
		    b.setAttribute("onclick", "contract(this,'"+id+"');");
		  }
		  
		  function switchHTMLObject(b,html1,html2,obj1,obj2) {
		    document.getElementById(obj1).style.display = "none";
		    document.getElementById(obj2).style.display = "";
		    b.setAttribute("onclick", "switchHTMLObject(this,'"+html2+"','"+html1+"','"+obj2+"','"+obj1+"')");
		    b.innerHTML = html1;
		  }
		</script>


		
		<script>
			var ClassHierarchyDataSource = function(type){
					this.srcType = type;
			};

			ClassHierarchyDataSource.prototype = {
					data : function(options, callback) {
						$.ajax({
							type: "POST",
							url: "/stamp/html/classIndex.jsp",
							data: {type: this.srcType, pkgName: options.name}
						}).done(function (response) {
							callback({data: $.parseJSON(response)});
						});
					}
			};
			
		    $('#AppHierarchy').tree({dataSource: new ClassHierarchyDataSource("app")});
		    $('#ModelsHierarchy').tree({dataSource: new ClassHierarchyDataSource("model")});
		    $('#FrameworkHierarchy').tree({dataSource: new ClassHierarchyDataSource("framework")});
		    $('#JimpleHierarchy').tree({dataSource: new ClassHierarchyDataSource("jimple")});
		</script>
		
		<script>
			var filePathToId = new Object();
			var tabNameToId = new Object();
			var idToHighlightedLine = new Object();
			var totalFilesOpened = 0;
			var flowSwitches = [];
                        var srcSinkWhitelist = [];
			for (var ii = 0; ii < numFlows; ++ii) {
				flowSwitches.push(true);
			}
			
			function rightBarAddDynamicData(drDataParams)
			{
			    var data = jQuery.parseJSON(atob(drDataParams));
			    if(data == null) return;
			    var html = ViewSource.droidrecordDataToTable(data, false);
			    $("#rightbar div.droidrecord-runtime-parameters").html(html);
			}

			function anyTaintedFlowShowing(taintedFlows) {
				for (var i = 0; i < taintedFlows.length; ++i) {
					if (flowSwitches[taintedFlows[i]-1]) {
						return true;
					}
				}
				return false;
			}

                        function ssTainted(ssdata) {
                            var whitelisted = false;
                            $.each(ssdata, function (index, value) {
                                $.each(value, function (i, v) {
                                    if ($.inArray(v,srcSinkWhitelist)>-1) {
                                        whitelisted = true;
                                    }
                                });
                            });

                            return whitelisted;
                        }
                        

			function colorTaint(href) {
				href = href.replace('#','');
				var taintedVariables = $('#'+href).find("[name=taintedVariable]");
			    for(var i=0; i<taintedVariables.length; ++i) {
			    	var flowString = taintedVariables[i].getAttribute("flows");
                                var ssspan = $(taintedVariables[i]).find(".srcSinkSpan")[0];
                                var ssdata = jQuery.parseJSON(atob($(ssspan).attr("data-stamp-srcsink")));
			    	var taintedFlows = flowString.split(':');
                    if (ssTainted(ssdata)) {
						if (flowString === 'null' || anyTaintedFlowShowing(taintedFlows)) {
							taintedVariables[i].setAttribute("style", "background-color:#FFB2B2");
						} else if (taintedVariables[i].hasAttribute('style')) {
							taintedVariables[i].removeAttribute('style');
						}
                    } else if (taintedVariables[i].hasAttribute('style')) {
                    	taintedVariables[i].removeAttribute('style');
                    }
			    }
			}

			function compactFlowCtxtTable($table) {
				var $tds = $table.find('td');

				$tds.each(function (index) {
						var tex = $(this).text();
						var a_regex = /.* (.+)\(.*\)$/;
						var match = tex.match(a_regex);
                                                if (match!=null) {
                                                    $(this).html(match[1]);
                                                }
					});
			}

			function showCode(response, href)
			{
				var $flowtable = $('#centerpane #flowctxttable');
				if ($flowtable.length > 0) {
					compactFlowCtxtTable($flowtable);
					$('#rightside').append($flowtable[0].outerHTML);
					$flowtable.remove();
					registerCellback();
				}

				var ppStr = prettyPrintOne(response, 'java', true);
				$('#codetabcontents').append('<div class="tab-pane source-view" id="'+href+'">'+ppStr+'</div>');

				colorTaint(href);
				
				$('#codetabs a:last').tab('show');
				
			    var methodNames = $('#'+href).find("span[name=MethodName]");
			    for(var i=0; i < methodNames.length; ++i) {
					
					// Added by Patrick
					lineNumber = $(methodNames[i]).parent().attr("name");
					
			        $(methodNames[i]).after('<img src="/stamp/res/down.png" height="12" width="12" style="display:inline"></img>');
			        var reachable = $(methodNames[i]).attr("data-reachable");
					var reached = $(methodNames[i]).attr("reached");
			        if(reachable == "true")
			        	$(methodNames[i]).css('background','#BCF5A9' );
			        $(methodNames[i]).next().on("click",  function(event){
			  		  var chordSig = $(this).prev().attr("data-chordsig");
					  var file = $(this).prev().attr("data-filepath");
					  
			  		  $('#rightbar').load('/stamp/html/imList.jsp',
			  		            {chordSig: chordSig, type: 'method'},
				    		    function () { 
									;//editNotes(chordSig);
								}
						);
								
						// Added by Patrick
						// display notes for the method when it is clicked
						//editNotes(chordSig);
						
						// Added by Patrick
						// records that we were at this location for the back button
						// recordNav(file, lineNumber);
			  		});
			    }

			    var invkSites = $('#'+href).find("span[name=PreInvocation]");
			    for(var i=0; i < invkSites.length; ++i) {
			        $(invkSites[i]).append('<img src="/stamp/res/down.png" height="12" width="12" style="display:inline"></img>');
			        
			        $(invkSites[i]).on("click",  function(event){ // invoke click
			    		  var chordSig = $(this).attr("data-chordsig");
			    		  var filePath = $(this).attr("data-filePath");
			    		  var lineNum = $(this).attr("data-lineNum");
			    		  var drDataParams = ""
			    		  var invocationExpression = $(this).find(".invocationExpression")[0];
			    		  if(invocationExpression != null) {
			    		    drDataParams = invocationExpression.attr("data-droidrecord-params");
			    		  }
			    		  $('#rightbar').load('/stamp/html/imList.jsp',
			    		    {chordSig: chordSig, type: 'invk', filePath: filePath, lineNum: lineNum}, 
			    		    function () { 
								rightBarAddDynamicData(drDataParams);
								//hideElement("#notes_pane")
								//showNotes(); 
							})
							
							// Added by Patrick
							// display notes for a call site's targets when it is clicked
							//showNotes(chordSig, filePath, lineNum)
							
			    		});
			    }
			    
			    function popoverAutoPlacement() {
			        var numVisiblePopOvers = $(".popover").size() % 4;
                    if(numVisiblePopOvers == 0) {
                        return 'bottom';
                    } else if(numVisiblePopOvers == 1) {
                        return 'top';
                    } else if(numVisiblePopOvers == 2) {
                        return 'left';
                    } else {
                        return 'right';
                    } 
			    }
			    
			    $(".invocationExpression").popover({
                        placement : popoverAutoPlacement, 
                        html : true,
                        title : function () {
                            var data = jQuery.parseJSON(atob($(this).attr("data-droidrecord-params")));
                            if(data == null) return "";
                            return data.methodName;
                        }, 
                        content : function() {
                            var data = jQuery.parseJSON(atob($(this).attr("data-droidrecord-params")));
			                if(data == null) return;
                            return ViewSource.droidrecordDataToTable(data, true);
                        }
                    });
			    
			    $(".srcSinkSpan").popover({
                        placement : popoverAutoPlacement, 
                        html : true,
                        title : function () {
                            return ""/*$(this).find("span[name=taintedVariable]").text()*/;
                        }, 
                        content : function() {
                            var data = jQuery.parseJSON(atob($(this).attr("data-stamp-srcsink")));
                            return ViewSource.formatStampSrcSinkInfo(data);
                        }
                    });
    
                $(".invocationExpression[data-droidrecord-params!=\"\"]").on("mouseenter",  function(event){
                    $(this).popover('show');
                    });
    
                $(".invocationExpression[data-droidrecord-params!=\"\"]").on("mouseleave",  function(event){
                    $(this).popover('hide');
                    });
    
                $(".srcSinkSpan").on("mouseenter",  function(event){
                    var srcSinkPopoverCount = $(".srcSinkSpan .popover").size();
                    if(srcSinkPopoverCount > 0) return;
                    $(this).popover('show');
                    });
    
                $(".srcSinkSpan").on("mouseleave",  function(event){
                    $(this).popover('hide');
                    });
    
                $(".invocationExpression[data-droidrecord-params!=\"\"]").on("click",  function(event){
                    var preinvk = $(this).find("span[name=PreInvocation]");
                    var chordSig = $(preinvk).attr("data-chordsig");
		            var filePath = $(preinvk).attr("data-filePath");
		            var lineNum = $(preinvk).attr("data-lineNum");
			    	var drDataParams = $(this).attr("data-droidrecord-params");
		            $('#rightbar').load('/stamp/html/imList.jsp',
		                {chordSig: chordSig, type: 'invk', filePath: filePath, lineNum: lineNum}, 
			    		function () { rightBarAddDynamicData(drDataParams); })
		            });

				var typeRefs = $('#'+href).find("[name=TypeRef]");
			    for(var i=0; i<typeRefs.length; ++i) {
					$(typeRefs[i]).on("click", function(event){
			    	   var chordSig = $(this).attr("data-chordsig");
			    	   $.ajax({
							type: "POST",
							url: "/stamp/html/getClassInfo.jsp",
							data: {chordsig: chordSig}
						}).done(function (response) {
							var tokens = $.trim(response).split(",");
							var filePath = tokens[0];
							var lineNum = tokens[1];
							showSource(filePath, 'false', lineNum, <%=useJimple%>);
						});	
                    });
			    }
			}		
			
			function highlightLine(ln, href)
			{
				if(typeof ln !== "undefined"){
					idToHighlightedLine[href] = ln;
					var scrollTo = $('#'+href+' ol li:nth-child('+ln+')');			
					var container = $('#'+href);
					container.scrollTop(
				    		scrollTo.offset().top - container.offset().top + container.scrollTop()
						);
					scrollTo.css('backgroundColor','#CEECF5' );
				}
			}
		</script>
			  
		<script>
			$('#AppHierarchy').on('selected', function(event,selection){
				showSource(selection.info[0].file, 'false', undefined, 'false');
			});

			$('#ModelsHierarchy').on('selected', function(event,selection){
				showSource(selection.info[0].file, 'true', undefined, 'false');
			});

			$('#FrameworkHierarchy').on('selected', function(event,selection){
				showSource(selection.info[0].file, 'false', undefined, 'false');
			});

			$('#JimpleHierarchy').on('selected', function(event,selection){
				showSource(selection.info[0].file, 'false', undefined, 'true');
			});
		</script>
		
		<script>
			var ResultDataSource = function(fname){
				this.resultFileName = fname;
			};

			ResultDataSource.prototype = {
					data : function(options, callback) {
						//for(var prop in options) alert(prop);
						//alert(this.resultFileName);
						$.ajax({
							type: "POST",
							url: "/stamp/html/viewResult.jsp",
							data: {resultFileName: this.resultFileName, nodeId: options.nodeId}
						}).done(function (response) {
							response = response.replace(/-&gt;/g,"&#10148;");
							callback({data: $.parseJSON(response)});
							});
						}
			};

			var showReport = function(reportscript, reportfile, nodeid, shortname) 
			{
			    var tabUniqueName = reportscript + ":" + nodeid;
			    var onTabLoad = function (href) {
                    $.ajax({
                        type: "POST",
                        url: "/stamp/html/reportviews/" + reportscript + ".jsp",
                        data: {filepath: reportfile, 
                               id: nodeid}
	                }).done(function (response) {
                        $('#codetabcontents').append('<div class="tab-pane source-view" id="'+href+'">'+response+'</div>');
				        $('#codetabs a:last').tab('show');
                    })
			    };
			    
			    var onTabDisplay = function (href) {
			        // Do nothing
			    };
			    
			    showContentTab(tabUniqueName, shortname, onTabLoad, onTabDisplay);
			};

            var datasources = {};
			
                       function setupResultTree(resultTreeId, resultFileName) {
                          useJimple = 'false';
                          //if(resultFileName.indexOf('jimple') != -1) {
                          if(<%=useJimple%>) {
                              useJimple = 'true';
                          }

                var datasource = new ResultDataSource(resultFileName);

                          $('#' + resultTreeId).tree({dataSource: datasource});
                datasources[resultTreeId] = datasource;
                      if(useJimple == 'true') {
                      //alert('true: ' + resultFileName);

                $('#' + resultTreeId).on('selected', function(event,selection){



                    var reportScript = selection.info[0].showReport;
                    if(typeof reportScript === "undefined")
                    {
                        var file = selection.info[0].file;
                        var lineNum = selection.info[0].lineNum;
                        if(typeof file === "undefined")
                            return;
                        if(typeof lineNum === "undefined")
                            return;
                        showSource(file, 'false', lineNum, 'true');
                    }
				    else 
				    {
                        var nodeID = selection.info[0].nodeId;
                        var shortName = selection.info[0].reportNodeShortName;
                        if(typeof nodeID === "undefined")
                            return;
                        if(typeof shortName === "undefined")
                            return;
                        showReport(reportScript, resultFileName, nodeID, shortName);
                    }
                });
		} else {
			//alert('false: ' + resultFileName);
                $('#' + resultTreeId).on('selected', function(event,selection){
                    var reportScript = selection.info[0].showReport;
                    if(typeof reportScript === "undefined")
                    {
                        var file = selection.info[0].file;
                        var lineNum = selection.info[0].lineNum;
                        if(typeof file === "undefined")
                            return;
                        if(typeof lineNum === "undefined")
                            return;
                        showSource(file, 'false', lineNum, 'false');
                    }
				    else 
				    {
                        var nodeID = selection.info[0].nodeId;
                        var shortName = selection.info[0].reportNodeShortName;
                        if(typeof nodeID === "undefined")
                            return;
                        if(typeof shortName === "undefined")
                            return;
                        showReport(reportScript, resultFileName, nodeID, shortName);
                    }
                });
		}	}

            /*
             * Hack for unescaping HTML strings. From CMS on stackoverflow
             * at http://stackoverflow.com/questions/1912501/unescape-html-entities-in-javascript
             */
            function htmlDecode(input){
              var e = document.createElement('div');
              e.innerHTML = input;
              return e.childNodes.length === 0 ? "" : e.childNodes[0].nodeValue;
            }

            function addSrcSinkToggles(id) {

                $('#'+id).parent().bind('afterShow', function () {
	                    var $selected = $(this).find('.tree-folder-name').filter ( function () {
		                        return true;
	                    });
                            if ($selected.find('i.icon-eye-open, i.icon-eye-close').length == 0) {
                                $selected.append('<i class="icon-eye-close" style="position:relative; float:right;"></i>');
                            }
                });

                $('#'+id).on('click','i.icon-eye-open, i.icon-eye-close', function() {

	                	var $selected = $(this).parent().parent().find('.tree-folder-name');
	                	var name = $selected.text();

	                    if ($(this)[0].className === 'icon-eye-close') {
	                        $(this).parent().append('<i class="icon-eye-open" style="position:relative; float:right;"></i>');
                                $(this).remove();
                                srcSinkWhitelist.push(name);
	                        $('#'+id+'help').empty();
	                        $('#'+id+'help').append('Taint from '+name+' now highlighted.');
	                    } else {
	                        $(this).parent().append('<i class="icon-eye-close" style="position:relative; float:right;"></i>');
                                $(this).remove();
                                if ($.inArray(name, srcSinkWhitelist) >= 0) {
                                    srcSinkWhitelist.splice($.inArray(name, srcSinkWhitelist), 1);
                                } else {
                                    console.log(name+' expected but not present in srcSinkWhitelist');
                                }
	                        $('#'+id+'help').empty();
	                        $('#'+id+'help').append('Not highlighting Taint from '+name);
	                    }

		                var $activeCodeTabs = $('li.active a');
		                for (var i = 0; i < $activeCodeTabs.length; ++i) {
		                	var attr = $activeCodeTabs[i].getAttribute('href');
		                	colorTaint(attr);
		                }
	                 });

				$('#'+id).parent().append('<p class="muted"><em id="'+id+'help">Click a Src/Sink name to show / hide </em></p>');
            }

            function addSrcSinkFlowBehavior(id) {

                function escTags(str) {
                    var tagsToReplace = {
                        '&': '&amp;',
                        '<': '&lt;',
                        '>': '&gt;'
                    };

                    

                    function replaceTag(tag) {
                        return tagsToReplace[tag] || tag;
                    }

                    return str.replace(/[&<>]/g, replaceTag);
                }

                /* Inner function to create each new table row */
                function newTableEntries(clines) {

                        if (clines.length < 2) {
                            console.log("Error: incomplete context");
                            return;
                        }
                        var ctxtSplit = /(.+)~~~(.+)/;
                        //var reSplit = /(.+)>,(.+)/;
                        //var reEntry = /.*<.* (\S+ .*)>.*<.* (\S+ .*)>.*/;
                        var sourceline = htmlDecode(clines[0].name);
                        var sinkline = htmlDecode(clines[1].name);

                        if (!ctxtSplit.test(sourceline)) {
                            console.log("Error: Regex failure on context parse");
                            return;
                        }

                        var source = sourceline.split('~~~');
                        var sink = sinkline.split('~~~');

                        //if (!reSplit.test(source[1]) || !reSplit.test(sink[1])) {
                        //    console.log("Error: Regex failure on context parse");
                        //    return;
                        //}

                        var source_ctxts = source[0].split('~~');//console.log('0? '+source_ctxts[0] + ' 1? '+source_ctxts[1]+' 2? '+source_ctxts[2]);
                        var sink_ctxts = sink[0].split('~~');//console.log('0? '+sink_ctxts[0] + ' 1? '+sink_ctxts[1]+' 2? '+sink_ctxts[2]);
                        console.log("lengths source "+source_ctxts.length + " sink "+sink_ctxts.length);
                        var entries = [];

                        //console.log("source: "+source[1]); console.log("sink: "+sink[1]);
                        var source_files = source[1].split('~~');//console.log('0? '+source_files[0] + ' 1? '+source_files[1]+' 2? '+source_files[2]);
                        var sink_files = sink[1].split('~~');//console.log('0? '+sink_files[0] + ' 1? '+sink_files[1]+' 2? '+sink_files[2]);
            
                        var source_entry = [];
                        for (var i = 0; i < 2; ++i) {
                            var sourcem = source_ctxts[i];//.match(reEntry);
                            source_entry.push('<tr><td'+((source_files[i]!=null)?' source="'+source_files[i]+'"':'')+'>'+escTags((sourcem!=null)?sourcem:'')+'</td></tr>');
                        }
                        entries.push(source_entry.join('\n'));

                        var sink_entry = [];
                        for (var i = 0; i < 2; ++i) {
                            var sinkm = sink_ctxts[i];//.match(reEntry);
                            sink_entry.push('<tr><td'+((sink_files[i]!=null)?' source="'+sink_files[i]+'"':'')+'>'+escTags((sinkm!=null)?sinkm:'')+'</td></tr>');
                        }
						entries.push(sink_entry.join('\n'));

                        return entries;                
                }

                $('#'+id).on('opened', function () {
	                    var $selected = $(this).find('.tree-folder-name').filter ( function () {
	                    	var flow_regex = /Flow (\d+)/;
		                    if ($(this).text() == '') {
		                        return true;
		                    }
	                    });
	                    $selected.parent().find('.icon-plus-sign').parent().html('<i class="icon-eye-open"></i>');
                });

                // on selected callback. Fuel UX provides selection
				$('#'+id).on('selected', function (ev, selection) {
					var $selected = $(this).find('.tree-folder-name').filter ( function () {
                        if ($(this).text() === selection.info[0].name) {
                            return true;
                        }
                    });

	                var datasource = datasources[id];

	                datasource.data($selected.parent().data(), function (items) {
	                                var dataarr = items.data;
	                                var contexts = [];
	                                for (var i = 0; i < dataarr.length; ++i) {
	                                    contexts.push(dataarr[i]);
	                                }

	                                var id = 'centerpane';
	                                if ($('li.active').length > 0) {
	                                	id = 'rightside';
	                                }

	                                if ($('#flowctxttable').length > 0) {
	                                    $('#flowctxttable').remove();
	                                }

                                    var entries = newTableEntries(contexts);
                                    var table = [];
                                    table.push('<table class="table" id="flowctxttable" border="0">');
                                    table.push('<tbody>');

                                    //source
								    table.push('<tr><td><span class="label label-info">Source Context</span></td></tr>');
                                    table.push('<tr><td><table class="table-condensed table-striped"  style="font-size: small; word-break: break-all; word-wrap: break-word"><tbody>'+entries[0]+'</tbody></table></td></tr>');

                                    //sink
								    table.push('<tr><td><span class="label label-info">Sink Context</span></td></tr>');
                                    table.push('<tr><td><table class="table-condensed table-striped"  style="font-size: small; word-break: break-all; word-wrap: break-word"><tbody>'+entries[1]+'</tbody></table></td></tr>');

	                                table.push('</tbody></table>');
                                    console.log(table.join('\n'));
	                                $('#'+id).append(table.join('\n'));
	                                if (id === 'rightside') {
	                                	compactFlowCtxtTable($('#'+id+' #flowctxttable'));
	                                }
	                                registerCellback();

	                            });

	            	});

                $('#'+id).on('click','i.icon-eye-open, i.icon-eye-close', function() {

	                	var $selected = $(this).parent().parent().find('.tree-folder-name');
	                	var name = $selected.text();
	                    var flow_regex = /Flow (\d+)/;
	                    if (!flow_regex.test(name)) {
	                        return;
	                    }
	                    var num = name.match(flow_regex)[1];

	                    if ($(this)[0].className === 'icon-eye-close') {
	                        $(this).parent().html('<i class="icon-eye-open"></i>');
	                        flowSwitches[num-1] = true;
	                        $('#srcsinkflowhelp').empty();
	                        $('#srcsinkflowhelp').append('Taint from Flow '+num+' now hightlighted.')
	                    } else {
	                        $(this).parent().html('<i class="icon-eye-close"></i>');
	                        flowSwitches[num-1] = false;
	                        $('#srcsinkflowhelp').empty();
	                        $('#srcsinkflowhelp').append('Not hightlighting taint from Flow '+num);
	                    }

		                var $activeCodeTabs = $('li.active a');
		                for (var i = 0; i < $activeCodeTabs.length; ++i) {
		                	var attr = $activeCodeTabs[i].getAttribute('href');
		                	colorTaint(attr);
		                }

                	
	                 });

				$('#'+id).parent().append('<p class="muted"><em id="srcsinkflowhelp">Click a Flow name to show / hide </em></p>');
            }

            function registerCellback() {
	            var $cells = $('#flowctxttable td');
	            $cells.click( function() {
		            	var source_str = $(this).attr('source');
		            	if (source_str !== '') {
			            	var source_splits = source_str.split(' ');
			            	showSource(source_splits[0],'false',source_splits[1]);
		            	}
	            	});
            }

			<%
			j = 0;
			for(Map.Entry<String,String> entry : titleToFileName.entrySet()){
				String title = entry.getKey();
				String resultFileName = entry.getValue();
				//if(!title.equals("Source-to-sink Flows")){
		    %>
			        setupResultTree('ResultTree<%=j%>', '<%=resultFileName%>');
			<%
                if (title.equals("Source-to-sink Flows")) {
            %>
                    addSrcSinkFlowBehavior('ResultTree<%=j%>');
			<%
                } else if (title.equals("Sinks") || title.equals("Sources")) {
            %>
                    addSrcSinkToggles('ResultTree<%=j%>');
			<%

                }
				//}
				j++;
			}
			%>		
		</script>	
	</body>
</html>
