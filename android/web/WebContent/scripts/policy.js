$(function () {

    'use strict';

    $.ajax({
        type: "GET",
        url: "/stamp/policyServlet",
        data: {
            annot: "Sources"
        },
        dataType: "xml",
        success: function (xml) {
            var seen = [];
            $(xml).find('src').each(function () {
                var d = $(this).attr("desc");
                if (jQuery.inArray(d, seen) < 0) {
                    seen.push(d);
                    $("#src-drop-down").append('<li><a href="#">' + d + '</a></li>');
                }
            });
        }
    });

    $.ajax({
        type: "GET",
        url: "/stamp/policyServlet",
        data: {
            annot: "Sinks"
        },
        dataType: "xml",
        success: function (xml) {
            var seen = [];
            $(xml).find('sink').each(function () {
                var d = $(this).attr("desc");
                if ($.inArray(d, seen) < 0) {
                    seen.push(d);
                    $("#sink-drop-down").append('<li><a href="#">' + d + '</a></li>');
                }
            });
        }
    });

    function addTableEntry(str) {
        var line = str.split(' ');
        var src = decodeURI(line[1]);
        var srcparam = decodeURI(line[2]);
        var sink = decodeURI(line[3]);
        var sinkparam = decodeURI(line[4]);
        $('#src-sink-table tr:last').after('<tr><td><input type="checkbox"' + ((line[0] === "1") ? ' checked' : '') + '><i class="checkbox"></i></td><td><i class="icon-remove"></i></td><td>' + src + '</td><td>' + srcparam + '</td><td>' + sink + '</td><td>' + sinkparam + '</td></tr>');

        $('#src-sink-table .icon-remove').click(function () {
            $(this).parent().parent().remove();
        });
    }


    function getPolicyRules(policyName) {

        $.ajax({
            type: "GET",
            url: "/stamp/policyServlet",
            data: {
                policyName: policyName
            },
            dataType: "text",
            success: function (tex) {
                $('#src-sink-table tr:not(:first)').remove();
                var lines = tex.split('\n');
                for (var i = 0; i < lines.length; i++) {
                    if (lines[i].length > 0) {
                        addTableEntry(lines[i]);
                    }
                }
                $('#policy-name-textbox').val(policyName);
            }
        });
    }

    var addListEntry = function (entry) {
        if (entry.length > 0) {
            $('#appslist tbody').append(
            ['<tr>',
               '<td>',
                 '<a class="policy-list-entry">' + decodeURI(entry) + '</a>',
               '</td>',
               '<td>',
                 '<button class="btn btn-inverse eval-policy" type="button" style="margin-left: 10px;">Eval</button>',
               '</td>',
               '<td>',
                 '<button class="btn btn-warning remove-policy" type="button" style="margin-left: 10px;">&#45</button>',
                '</td>',
              '</tr>'].join(' '));
        }
    }

    $.ajax({
        type: "GET",
        url: "/stamp/policyServlet",
        data: {
            policies: "all"
        },
        dataType: "text",
        success: function (tex) {
            var lines = tex.split('\n');
            for (var i = 0; i < lines.length; i++) {
                addListEntry(lines[i]);
            }
            if (lines.length > 0) {
                getPolicyRules(lines[0]);
            }
            $('.policy-list-entry').click(function () {
                getPolicyRules(this.text);
            });
            $(".remove-policy").click(function () {
                var outer = $(this).parent().parent();
                $.post("/stamp/policyServlet", {
                    delete: "just_delete",
                    policyName: encodeURI(outer.find('.policy-list-entry').text())
                }, function (data) {
                    $(outer).remove();
                });
            });
        }
    });


    $('#add_policy_btn').click(function () {
        var src = $('#srcSelect').select('selectedItem').text;
        var srcparam = $('#src_param').val();
        var sink = $('#sinkSelect').select('selectedItem').text;
        var sinkparam = $('#sink_param').val();
        $('#src-sink-table tr:last').after('<tr><td><input type="checkbox"><i class="checkbox" checked></i></td><td><i class="icon-remove"></i></td><td>' + src + '</td><td>' + srcparam + '</td><td>' + sink + '</td><td>' + sinkparam + '</td></tr>');
        $('#src-sink-table .icon-remove').click(function () {
            $(this).parent().parent().remove();
        });
    });
    
    $('#addNewPolicyBtn').click(function () {
        $('#src-sink-table tr:not(:first)').remove();
        $('#policy-name-textbox').val("New Policy");
    });

    $('#save_policy_btn').click(function () {
        var $table = $('#src-sink-table');
        var rules = [];
        var policyName = $('#policy-name-textbox').val();
        $table.find('tr').each(function () {
            var $tds = $(this).find('td');
            if ($tds.length > 0) {
                var rule = {};
                rule.active = ($(this).find(':checkbox')[0].checked) ? 1 : 0;
                rule.sourceName = encodeURI($tds.eq(2).text());
                rule.sourceParamRaw = encodeURI($tds.eq(3).text());
                rule.sinkName = encodeURI($tds.eq(4).text());
                rule.sinkParamRaw = encodeURI($tds.eq(5).text());
                rule.policyName = encodeURI(policyName);
                rules.push(rule);
            }
        });

        if ($('#appslist :contains(' + policyName + ')').length == 0) {
            $.ajax({
                type: "GET",
                url: "/stamp/policyServlet",
                data: {
                    policies: "all"
                },
                dataType: "text",
                success: function (tex) {
                    $('#appslist tbody').empty();
                    var lines = tex.split('\n');
                    for (var i = 0; i < lines.length; i++) {
                        addListEntry(lines[i]);
                    }
                    $('.policy-list-entry').click(function () {
                        getPolicyRules(this.text);
                    });
                    $(".remove-policy").click(function () {
                        var outer = $(this).parent().parent();
                        $.post("/stamp/policyServlet", {
                            delete: "just_delete",
                            policyName: encodeURI(outer.find('.policy-list-entry').text())
                        }, function (data) {
                            $(outer).remove();
                        });
                    });
                }
            });
        }

        if (rules.length < 1) {
            return;
        }

        rules[0].delete = "before_update";
        for (var i = 0; i < rules.length; i++) {
            $.post("/stamp/policyServlet", rules[i], function (data) {
                var date = new Date();
                var timestr = date.getHours() + ':' + date.getMinutes();
                $('#policy_save_status').html('<i>Policy Saved at ' + timestr + '</i>');
            });
        }
    });

    $('#src-sink-table .icon-remove').click(function () {
        $(this).parent().parent().remove();

    });
});