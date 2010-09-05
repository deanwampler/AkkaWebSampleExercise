// JavaScript for the AkkaWebSampleExercise

function padLeft(s, l, c) {
  s += '';
  while (s.length < l) {
    s = c + s;
  }
  return s;
}

function formatDate(date) {
  return padLeft((date.getMonth() + 1), 2, '0') + "/" +
    padLeft(date.getDate(), 2, '0') + "/" +
    date.getFullYear()
}

function setupDefaultDates() {
  var today = new Date()
  $('.start').val(formatDate(today))
  $('.end').val(formatDate(today))
}

function submitOnCarriageReturn(elements, controlsParent) {
  $(elements).keydown(function (e) {
    var keyCode = e.keyCode || e.which;

    if (keyCode == 13) {
  		// If used a regular HTML form, we would "submit" it here:
      //   $(this).parents('form').submit();
      // But we use a programmatic approach...
      serverControl('stats');
      return false;
    }
  });
}

function writeDebug(string) {
  $("#debug").html(string);
}

function appendDebug(string) {
  var dbg = $("#debug");
  dbg.html(dbg.html() + "<br/>" + string);
}

function clearMessage() {
  writeMessage("", "info")
}

function writeMessage(string, fontClass, whichMessageSpan) {
  var span = whichMessageSpan;
  if (! whichMessageSpan) {
    span = "";
  }
  var messageTag = $("#message"+span);
  messageTag.html("<font class='"+fontClass+"'>"+string+"</font>");
}

function writeError(string, whichMessageSpan) {
  writeMessage(string, "error", whichMessageSpan);
}

function writeWarning(string, whichMessageSpan) {
  writeMessage(string, "warning", whichMessageSpan);
}

function writeInfo(string, whichMessageSpan) {
  writeMessage(string, "info", whichMessageSpan);
}

function onSuccess(jsonString) {
  $('.banner').fadeOut(2000);
  writeInfo("Response received")
  var json = $.parseJSON(jsonString)
  if (json === null || json === undefined) {
    writeError("Invalid data returned by AJAX query: " + jsonString)
  } else if (json["message"]) {
    writeInfo(json["message"])
  } else if (json["pong"]) {
    var pongList = $('#pong-list')
    pongList.html('')
    // TODO: Fix, the returned json is a doubly-nested array [[...]]
    $.each($(json["pong"]), function(i, array) {
      $.each(array, function(j, item) {
        pongList.append("<li>" + item + "</li>")
      })
    })
    $('#finance-display').hide()
    $('#pong-display').show()
  } else if (json["info"]) {
    writeInfo(json["info"]);
  } else if (json["warn"]) {
    writeWarning(json["warn"])
  } else if (json["warning"]) {
    writeWarning(json["warning"])
  } else if (json["error"]) {
    writeInfo(json["error"])
  } else if (json["financial-data"]) {
    // Plot the financial data. 
    // TODO: we more or less assume that there is really one instrument and
    // one statistic in each "row", because that's how the data is currently
    // returned, with one-element arrays for the instruments and statistics.
    $('#pong-display').hide()
    $("#finance-table tbody").html('')
    $.each(json["financial-data"], function(i, row) {
      var criteria = row.criteria
      var instruments = row.criteria.instruments
      var statistics  = row.criteria.statistics
      var results     = row.results
      if (instruments.length > 1)
        appendDebug("More than one instrument in the row! Assuming all data is for the first instrument...")
      if (statistics.length > 1)
        appendDebug("More than one statistic in the row! Assuming all data is for the first statistic...")
      if (results.length === 0) {
        $("#finance-table tbody").append(
          "<tr class='results-row'><tr><td>"+instruments+"</td><td>"+statistics+"</td><td><b>No Data!</b></td></tr>")        
      } else {
        $.each(results, function(j, result) {
          $("#finance-table tbody").append(
            "<tr class='results-row'><td class='instruments'>" + instruments +
            "</td><td class='statistics'>" + statistics + 
            "</td><td class='results'>" + result + "</td></tr>")        
        })
      }
    })
    setUpTableSorting()
    $('#finance-display').show()
  } else {
    writeError("Unexpected JSON returned. Listed below and also written to the JavaScript console")
    writeDebug("Unexpected JSON returned: "+json)
    console.log(json)
  }
  // Use for continuous polling. Adjust the anon. function as appropriate.
  //  setTimeout(function() {
  //   sendRequest("???");
  // }, 3000);
}

function onError(request, textStatus, errorThrown) {
  writeDebug("Ajax request failed: XMLHttpRequest="+request.responseText+", textStatus: "+textStatus+", errorThrown: "+errorThrown);
}

function sendRequest(action) {
  var toolbar = $.find('#master-toolbar')
  var symbols = $(toolbar).find('#symbols').val()
  var stats   = $(toolbar).find('.stats-option:selected').val()
  var start   = $(toolbar).find('#start').val()
  var end     = $(toolbar).find('#end').val()
  $('#pong-display').hide()
  $('#finance-display').hide()
  
  $.ajax({
    url: "ajax/" + action + 
         "/?symbols=" + encodeURI(symbols) +
         "&stats=" + encodeURI(stats) +
         "&start=" + encodeURI(start) +
         "&end=" + encodeURI(end),
    method: 'GET',
    contentType: 'text/plain',
    dataType: 'text/plain',
    success : onSuccess,
    error : onError,
    beforeSend: function(request) {
      if (request && request.overrideMimeType) {
        request.overrideMimeType('text/plain');
      }
    }
  });
}

function serverControl(action) {
  var action2 = action;
  sendRequest(action);
}

function setUpTableSorting() {
  return // disabled until I can figure out a JS error that occurs.
  $('table.tablesorter').tablesorter({
    sortList:[[0,0], [1,0], [2,0]],
    headers: { 
       // disable some columns,
       // 0: { sorter: false },
    },
    widgets:['zebra']
  });  
  
  $("table.tablesorter").bind("sortStart", function() { 
  }).bind("sortEnd",function(doc) {
    // make sure the correct td's have the rounded corners.
    $("table.tablesorter").find(".results-row > td").removeClass('bottom-left-rounded-corners').removeClass('bottom-right-rounded-corners'); 
    $.each($("table.tablesorter"), function(index, table) {
      var rows = $(table).find(".results-row")
      var lastRow = rows[rows.size()-1]
      $(lastRow).find('.instruments').addClass('bottom-left-rounded-corners')
      $(lastRow).find('.results').addClass('bottom-right-rounded-corners')        
    })
  })  
}

function setupDatePicker(){
  Date.format = 'mm/dd/yyyy'
  var today = (new Date()).asString()
  $('.date-pick').datePicker({
    clickInput:  true,
    startDate:   '01/01/1970',
    endDate:     today,
    defaultDate: today
  });
  $('.date-pick').dpSetOffset(25,0);
}

$(document).ready(function () {
  $('.icon').click(function(){
    $('.banner').fadeIn('slow');
    $('.banner').fadeOut(2000);
  })
  // Call the following each time the finance table is set up!
  // setUpTableSorting()
  setupDatePicker()
  setupDefaultDates()
  submitOnCarriageReturn($('.date-pick'), $('#master-toolbar'))
});

