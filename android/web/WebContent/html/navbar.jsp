<div class="navbar navbar-inverse navbar-fixed-top">
	<div class="navbar-inner">
		<div class="container">
			<ul class="nav pull-left">
			<li><a class="brand" href="#">STAMP&alpha;</a></li>
			</ul>
				
			<ul class="nav">
				<li class="dropdown">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown">Code<b class="caret"></b></a>
           			<ul class="dropdown-menu">
           				<li><a href="#" id="showtab-0">App</a></li>
           				<li><a href="#" id="showtab-1">Models</a></li>
           				<li><a href="#" id="showtab-2">Framework</a></li>
           				<li><a href="#" id="showtab-3">Jimple</a></li>
           			</ul>
				</li>
			</ul>
					
			<ul class="nav">				
				<!-- Results -->
				<li class="dropdown">
           			<a href="#" class="dropdown-toggle" data-toggle="dropdown">Analyze<b class="caret"></b></a>
           			<ul class="dropdown-menu">
					<%
						int i = 4;
						for(String title : titleToFileName.keySet()){
					%>
							<li><a href="#" id="showtab-<%=i%>"><%=title%></a></li>
					<%
							i++;
						}
					%>
						<li><a onClick="analyzeManifest();">Manifest</a></li>
           			</ul>
         		</li>
			</ul>
			
			<ul class="nav">
				<!-- Warning Menu -->
          		<li class="dropdown">
            		<a href="#" class="dropdown-toggle" data-toggle="dropdown">Warnings<b class="caret"></b></a>
            		<ul class="dropdown-menu">
	          			<li><a onClick="loadList('models/src', 'leftbar')">Code Loading</a></li>
	          			<li><a onClick="loadList('models/src', 'leftbar')">Code Generation</a></li>
	          			<li><a onClick="loadList('models/src', 'leftbar')">Deletions</a></li>
	          			<li><a onClick="loadList('models/src', 'leftbar')">Exposed Services</a></li>
	          			<li><a onClick="loadList('models/src', 'leftbar')">Messaging</a></li>
	          			<li><a onClick="loadList('models/src', 'leftbar')">Permissions</a></li>
	          			<li><a onClick="loadList('models/src', 'leftbar')">Randomness</a></li>
	          			<li><a onClick="loadList('models/src', 'leftbar')">Storage</a></li>
            		</ul>
          		</li>
          	</ul>

			<ul class="nav">
				<!-- Help -->
				<li class="dropdown">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown">Help <b class="caret"></b></a>
					<ul class="dropdown-menu">
						<li><a href="/doc/stamp.html" target="_blank">Docs</a></li>
						<li><a href="http://www.youtube.com/user/STAMPStanford" target="_blank">Video Tutorials</a></li>
					</ul>
				</li>

				<!-- <li><a href="#d3">Visualize</a></li> -->
				<!-- <li><a href="https://sites.google.com/site/stampwebsite">About</a></li> -->
				<!-- <li><a href="#">Settings</a></li> -->
				<!-- Modes: test for annoyances, flows from ad libs to notif bar, etc -->
			</ul>
			
			<ul>
				<div id="nav-div">
					<span id="nav-label"> Grep </span>
					<input type="text" id="grep-input">
				</div>
			</ul>

			<!-- DisplayApp Name -->
			<!--span class="pull-right" id="appname"><h4><%=session.getAttribute("appPath")%></h4></span-->

		</div>
 	</div>
</div> 
