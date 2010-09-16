// JavaScript for the AkkaWebSampleExercise

function padLeft(s, l, c) {
  s += '';
  while (s.length < l) {
    s = c + s;
  }
  return s;
}

function formatDate(date) {
  return date.getFullYear() + "-" +
    padLeft((date.getMonth() + 1), 2, '0') + "-" +
    padLeft(date.getDate(), 2, '0')
}

function setupDefaultDates() {
  var today = new Date()
  $('#start').val(formatDate(today))
  $('#end').val(formatDate(today))
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
  writeInfo("Response received...")
  var json = $.parseJSON(jsonString)
  if (json === null || json === undefined) {
    writeError("Invalid data returned by AJAX query: " + jsonString)
  } else if (json["message"]) {
    writeInfo(json["message"])
  } else if (json["pong"]) {
    var pongList = $('#pong-list')
    pongList.html('')
    // TODO: Fix, the returned json is a EITHER a singly- or doubly-nested array [[...]]
    $.each($(json["pong"]), function(i, arrayOrObject) {
  	  if (is_array(arrayOrObject) === false) {
        arrayOrObject = [arrayOrObject]
			}
      $.each(arrayOrObject, function(j, item) {
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
    writeError(json["error"])
  } else if (json["financial-data"]) {
		displayFinancialData(json["financial-data"],
      [["Symbol", function(row) { return row.stock_symbol; }],
       ["Date",   function(row) { return row.date; }],
       ["Price",  function(row) { return row.close; }]])
  } else if (json["instrument-list"]) {
		displayInstrumentsLists(json["instrument-list"])
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

function displayFinancialData(json, fields) {
  // Plot the financial data. 
  // TODO: we more or less assume that there is really one instrument and
  // one statistic in each "row", because that's how the data is currently
  // returned, with one-element arrays for the instruments and statistics.
  $('#pong-display').hide()
  // Create the header row:
  $("#finance-table thead").html('<tr class="finance-head"></tr>') // start with a clean row.
  $.each(fields, function(i, field) {
    if (i == 0)
      $(".finance-head").append("<th class='top-left-rounded-corners'>" + field[0] + "</th>")
    else if (i == fields.length - 1)
      $(".finance-head").append("<th class='top-right-rounded-corners'>" + field[0] + "</th>")
    else
      $(".finance-head").append("<th>" + field[0] + "</th>")    
  })
  // Create the body rows:
  $("#finance-table tbody").html('') // clear the body first.
  $.each(json, function(i, row) {
    var criteria = row.criteria
    var instruments = "unknown criteria"
    var statistics  = "unknown statistics"
    if (criteria) {
      instruments = criteria.instruments
      statistics  = criteria.statistics
    }
    var results  = row.results
    if (results.length === 0) {
      var start = $('#master-toolbar').find('#start').val()
      var end   = $('#master-toolbar').find('#end').val()
      $("#finance-table tbody").append(
        "<tr class='results-row'><tr><td colspan='"+fields.length+"'><b>No "+statistics+" data for "+instruments+". Time range: "+start+" to "+end+
          "</b></br><font class='tiny'>(Note: time range may not be relevant for all queries...).</font></td></tr>")        
    } else {
      $.each(results, function(j, result) {
        var idij = "results-row-" + i+"_"+j
        $("#finance-table tbody").append("<tr class='results-row' id='results-row-" + idij + "'></tr>")
        $.each(fields, function(k, field) {
          var idijk = "results-row-" + i+"_"+j+"_"+k
          var value = field[1](result)
          $('#results-row-' + idij).append("<td class='statistic' id='statistic-'" + idijk + "'>" + value + "</td>")
        })
      })
    }
  })
  $('#finance-display').show()
  setUpTableSorting()
}

function displayInstrumentsLists(json) {
  $('#pong-display').hide()
  // Create the header row:
  $("#finance-table thead").html(     // start with a clean row.
    "<tr class='finance-head'>" + 
    "<th class='top-left-rounded-corners'>Letter</th>" +
    "<th class='top-right-rounded-corners'>Symbols</th>" +
    "</tr>")

  // Create the body rows:
  $("#finance-table tbody").html('') // clear the body first.
	// Hack: Handle case where there was only one object, so json is that object, not an array.
	if (is_array(json) === false) {
    json = [json]
	} 
  $.each(json, function(i, row) {
    var symbols  = row.symbols
    if (symbols.length === 0) {
      $("#finance-table tbody").append(
        "<tr class='results-row'><td class='symbol-letter'>" + row.letter + "</td><td class='no-symbols'>No instruments!</td></tr>")        
    } else {
      $("#finance-table tbody").append(
			  "<tr class='results-row'><td class='symbol-letter'>" + row.letter + "</td><td class='symbols'>" + symbols.join(', ') + "</td></tr>")
		}
  })
  $('#finance-display').show()
  setUpTableSorting()
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
  writeInfo("Sending request...")
  var action2 = action;
  sendRequest(action);
}

function setUpTableSorting() {
  $('table.tablesorter').tablesorter({
					//sortList: [[0,0], [1,0]],
    // headers: { 
       // disable some columns,
       // 0: { sorter: false },
    // },
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
  Date.format = 'yyyy-mm-dd'
  var today = (new Date()).asString()
  $('.date-pick').datePicker({
    // clickInput:  true,
    startDate:   '1970-01-01',
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
  setupDatePicker()
  setupDefaultDates()
  submitOnCarriageReturn($('.submit-on-CR'), $('#master-toolbar'))
});

