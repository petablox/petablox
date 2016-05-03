var ViewSource = (function ()  
{
    var module = {};

    function hashtocolor(num)
    {
        num = ((num >> 16) ^ num) * 0x45d9f3b;
        num = ((num >> 16) ^ num) * 0x45d9f3b;
        num = ((num >> 16) ^ num);
        return Math.abs(num % 16777215).toString(16);
    }
    
    function prettyprintParamType(param_type, isPopover)
    {
        if(isPopover) {
            var l = param_type.split(".");
            return l[l.length-1];
        } else {
            return param_type;
        }
    }
    
    function prettyprintParam(param, isPopover)
    {
        if(param.klass === undefined) {
            // Not an object-like param:
            if(param.type == "other" && param.value == "NULL_TYPE") {
               return "null"; 
            } else if(isPopover && param.type == "string" && 
                      param.value.length > 30) {
                return param.value.substr(0, 27) + "...";
            } else {
                return param.value;
            }
        }
        
        // Object-like param
        if(param.id === 0) return "null";
        var color = hashtocolor(param.id);
        
        if(isPopover) {
            var l = param.klass.split(".");
            return "<span style=\"color:#" + color + ";\">"+l[l.length-1]+"<span>";
        } else {
            return "<a class=\"droidrecord-obj-param-link\" title=\"Object ID: "+param.id+"\" " +
                   "style=\"color:#" + color + ";\">" + param.klass + "</a>";
        }
    }
    
    // 'Public' function
    module.droidrecordDataToTable = function(data, isPopover)
    {
        if(data.calls.length === 0) return "";
        
        var numArgs = data.calls[0].params.length;
        
        var html = "<table class=\"droidrecord-parameter-info-table ";
        if(isPopover) html += "droidrecord-parameter-info-table-popover";
        else html += "droidrecord-parameter-info-table-rightbar";
        html += "\">";
        html += "<tr>";
        var numCols = numArgs;
        var htmlClass;
        var columnCounter = 0;
        if(data.calls[0].returnValue !== undefined && 
           !(data.calls[0].returnValue.type == "other" &&
             data.calls[0].returnValue.value == "VOID_TYPE")) {
            htmlClass = "";
            numCols++;
            if(columnCounter === 0) {
                htmlClass += "table-leftmost ";
            } 
            if(columnCounter == (numCols-1)) {
                htmlClass += "table-rightmost ";
            }
            if(htmlClass !== "") {
                html += "<th class=\""+htmlClass+"\">return</th>";
            } else {
                html += "<th>return</th>";
            }
            columnCounter++;
        }
        if(numArgs == (data.parameterTypes.length + 1)) {
            htmlClass = "";
            if(columnCounter === 0) {
                htmlClass += "table-leftmost ";
            } 
            if(columnCounter == (numCols-1)) {
                htmlClass += "table-rightmost ";
            }
            if(htmlClass !== "") {
                html += "<th class=\""+htmlClass+"\">this</th>";
            } else {
                html += "<th>this</th>";
            }
            columnCounter++;
        }
        for(var ptype in data.parameterTypes) {
            htmlClass = "";
            if(columnCounter === 0) {
                htmlClass += "table-leftmost ";
            } 
            if(columnCounter == (numCols-1)) {
                htmlClass += "table-rightmost ";
            }
            if(htmlClass !== "") {
                html += "<th class=\""+htmlClass+"\">";
            } else {
                html += "<th>";
            }
            columnCounter++;
            html += prettyprintParamType(data.parameterTypes[ptype], isPopover) + "</th>";
        }
        html += "</tr>";
        for(var call in data.calls) {
            var pvals = data.calls[call].params;
            var returnV = data.calls[call].returnValue;
            html += "<tr>";
            columnCounter = 0;
            if(returnV === undefined) {
                html += "<td class=\"table-leftmost\">unknown</td>";
                columnCounter++;
            } else if(returnV.type != "other" || returnV.value != "VOID_TYPE") {
                html += "<td class=\"table-leftmost\">" + prettyprintParam(returnV, isPopover) + "</td>";
                columnCounter++;
            }
            for(var pval in pvals) {
                htmlClass = "";
                if(columnCounter === 0) {
                    htmlClass += "table-leftmost ";
                } 
                if(columnCounter == (numCols-1)) {
                    htmlClass += "table-rightmost ";
                }
                if(htmlClass !== "") {
                    html += "<td class=\""+htmlClass+"\">";
                } else {
                    html += "<td>";
                }
                columnCounter++;
                html += prettyprintParam(data.calls[call].params[pval], isPopover) + "</td>";
            }
            html += "</tr>";
        }
        html += "</table>";
        //console.log(html)
        return html;
    };
    
    
    module.formatStampSrcSinkInfo = function(data)
    {
        var html = "";
        if(data.sources.length !== 0) {
            html += "<b>Sources:</b><ul>";
            for(var s in data.sources) {
                html += "<li>" + data.sources[s] + "</li>";
            }
            html += "</ul>";
        }
        if(data.sinks.length !== 0) {
            html += "<b>Sinks:</b><ul>";
            for(var s1 in data.sinks) {
                html += "<li>" + data.sinks[s1] + "</li>";
            }
            html += "</ul>";
        }
        return html;
    };
    
    return module;
}());  
