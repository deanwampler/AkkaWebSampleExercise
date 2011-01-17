// JavaScript for the AkkaWebSampleExercise

// An object that maps from the actual data fields in the returned JSON
// to fixed keys used in this code. So, if the JSON format changes, you
// only need to modify this map.
var field_map = { 
  Symbol: function(row) { return row.stock_symbol; },
  Date:   function(row) { return row.date; },
  Open:   function(row) { return row.stock_price_open; },
  Close:  function(row) { return row.stock_price_close; },
  Low:    function(row) { return row.stock_price_low; },
  High:   function(row) { return row.stock_price_high; },
  Volume: function(row) { return row.stock_volume; }
}

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
    $('#finance-graph-display').hide()
    $('#finance-table-display').hide()
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
      function(row, newDate) { row.date = newDate; })
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

function displayFinancialData(json, setDate) {
  // TODO: we more or less assume that there is really one instrument and
  // one statistic in each "row", because that's how the data is currently
  // returned, with one-element arrays for the instruments and statistics.

  $('#pong-display').hide()
  var json2 = is_array(json) ? json : [json] // hack to deal with inconsistent returned results.
  if ($('#graph:checked').length > 0)
    displayFinancialDataInAGraph(json2, setDate)
  else
    displayFinancialDataInATable(json2, setDate)
}

function displayFinancialDataInAGraph(json, setDate) {
  var all_graph_data = []
  var graph_data_bounds = undefined
  var empty_results = []
  $.each(json, function(i, row) {
    var criteria = row.criteria
    var instruments = "unknown criteria"
    var statistics  = "unknown statistics"
    if (criteria) {
      instruments = criteria.instruments
      statistics  = criteria.statistics
    }
    var graph_data = []
    var results  = row.results
    if (results.length === 0) {
      empty_results.push("No " + statistics + " results for " + instruments + "!")
    } else {
      $.each(results, function(j, result) {
        var t  = new Date(field_map.Date(result))
        setDate(result, t)
        // p is "price".
        var open_p  = field_map.Open(result)
        var close_p = field_map.Close(result)
        var low_p   = field_map.Low(result)
        var high_p  = field_map.High(result)
        var volume  = field_map.Volume(result)
        if (graph_data_bounds === undefined) {
          graph_data_bounds = {min_t: t, max_t: t, min_p: low_p, max_p: high_p, max_volume: volume}
        } else {
          if (t < graph_data_bounds.min_t) {
            graph_data_bounds.min_t = t
          }
          if (t > graph_data_bounds.max_t) {
            graph_data_bounds.max_t = t
          }
          if (low_p < graph_data_bounds.min_p) {
            graph_data_bounds.min_p = low_p
          }
          if (high_p > graph_data_bounds.max_p) {
            graph_data_bounds.max_p = high_p
          }
          if (volume > graph_data_bounds.max_volume) {
            graph_data_bounds.max_volume = volume
          }
        }
      })
    }
    all_graph_data.push({instruments: instruments, statistics: statistics, data: results})
  })
  if (empty_results.length > 0) {
    writeWarning(_.reduce(empty_results, function(s, item) { return s + "</br>" + item }, ""))
  }
  // Must delete and recreate the svg every time.
  $('span svg').remove()
  graphData(all_graph_data, graph_data_bounds)
  $('#finance-graph-display').show()
}


function graphData(data, data_bounds) {
  if (data_bounds === undefined) {
    console.log("graphData: data_bounds is undefined!!")
    return
  }
  
  /* Parse dates. */
  // var dateFormat = pv.Format.date("%y-%b-%s");
  // data.forEach(function(d) { d.date = dateFormat.parse(d.date) });
  
  time_delta = data_bounds.max_t.getTime() - data_bounds.min_t.getTime()
  padding_on_the_right = time_delta * .05
  max_t = new Date(data_bounds.max_t.getTime() + padding_on_the_right)
  
  /* Sizing and scales. */
  var w = $(window).width() * .9,
      h = $(window).height() * .8,
      x = pv.Scale.linear(data_bounds.min_t, max_t).range(0, w),
      y = pv.Scale.linear(data_bounds.min_p, data_bounds.max_p).range(0, h);

  /* The root panel. */
  var vis = new pv.Panel()
        .width(w)
        .height(h)
        .bottom(20)
        .left(20)
        .right(10)
        .right(5)
  
  /* X-axis ticks. */
  vis.add(pv.Rule)
      .data(x.ticks(20))
      .visible(function(d) {return d})
      .left(x)
      .strokeStyle("#eee")
      .add(pv.Rule)
      .bottom(-5)
      .height(5)
      .strokeStyle("#000")
      .anchor("bottom").add(pv.Label)
      .text(x.tickFormat);

  /* Y-axis ticks. */
  vis.add(pv.Rule)
      .data(y.ticks(20))
      .bottom(y)
      // .strokeStyle(function(d){return d ? "#eee" : "#000"})
      .strokeStyle(function(d) {return d ? "rgba(128,128,128,.2)" : "#CCC"})
      .anchor("left").add(pv.Label)
      .text(y.tickFormat);
  
  /* Lines and candlesticks. */
  $.each(data, function(i, data_for_stock) {
    var color = 'rgba(' + ((32 * i) % 256) + ',' + ((64 * i) % 256) + ',' + ((128 * i) % 256) + ', .5)' //pv.Colors.category20().by(i)

    /* A line for each stock, colored differently.
       However, the lines don't work well with candelsticks, so they are commented out.
    vis.add(pv.Line)
        .data(data_for_stock.data)
        .interpolate("step-after")
        .left(function(d) {return x(new Date (d.date))})
        .bottom(function(d) {return y(d.close)})
        .strokeStyle(color)
        .lineWidth(3)
    */
    /* Candlestick. */
    console.log(data_for_stock)
    vis.add(pv.Rule)
        .data(data_for_stock.data)
        .left(function(d) { return x(new Date(field_map.Date(d)))})
        .bottom(function(d) { return y(Math.min(field_map.High(d), field_map.Low(d))) })
        .height(function(d) { return Math.abs(y(field_map.High(d)) - y(field_map.Low(d))) })
        .strokeStyle(function(d) { return field_map.Open(d) <= field_map.Close(d) ? "#06982d" : "#ae1325" })
      .add(pv.Rule)
        .bottom(function(d) { return y(Math.min(field_map.Open(d), field_map.Close(d))) })
        .height(function(d) { return Math.abs(y(field_map.Open(d)) - y(field_map.Close(d))) })
        .lineWidth(4);
    
    /* Add labels for each line end */
    vis.add(pv.Mark)
        .data(data_for_stock.data.slice(-1))
        .add(pv.Label)
        .textStyle(color)
        .font("16px sans-serif")
        .left(function(d) { return x(field_map.Date(d)) })
        .bottom(function(d) { return y(field_map.Close(d)) })
        .text(data_for_stock.instruments)
  })

  vis.render();  
}

function displayFinancialDataInATable(json, setDate) {
  // Create the header row:
  $("#finance-table thead").html('<tr class="finance-head"></tr>') // start with a clean row.
  $.each(field_map, function(key, value) {
    if (i == 0)
      $(".finance-head").append("<th class='top-left-rounded-corners'>" + key + "</th>")
    else if (i == field_map.length - 1)
      $(".finance-head").append("<th class='top-right-rounded-corners'>" + key + "</th>")
    else
      $(".finance-head").append("<th>" + key + "</th>")    
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
        "<tr class='results-row'><tr><td colspan='"+field_map.length+"'><b>No "+statistics+" data for "+instruments+". Time range: "+start+" to "+end+
          "</b></br><font class='tiny'>(Note: time range may not be relevant for all queries...).</font></td></tr>")        
    } else {
      $.each(results, function(j, result) {
        var idij = "results-row-" + i+"_"+j
        $("#finance-table tbody").append("<tr class='results-row' id='results-row-" + idij + "'></tr>")
        $.each(field_map, function(k, field) {
          var idijk = "results-row-" + i+"_"+j+"_"+k
          var value = field(result)
          $('#results-row-' + idij).append("<td class='statistic' id='statistic-'" + idijk + "'>" + value + "</td>")
        })
      })
    }
  })
  $('#finance-table-display').show()
  setUpTableSorting()
}

function displayInstrumentsLists(json) {
  $('#pong-display').hide()
  // Create the header row:
  $("#finance-table thead").html(     // start with a clean row.
    "<tr class='finance-head'>" + 
    "<th class='top-left-rounded-corners'>&nbsp;</th>" +
    "<th class='top-right-rounded-corners'>Symbols</th>" +
    "</tr>")

  // Create the body rows:
  $("#finance-table tbody").html('') // clear the body first.
	// Hack: Handle case where there was only one object, so json is that object, not an array.
	if (is_array(json) === false) {
    json = [json]
	} 
	console.log(json)
  $.each(json, function(i, row) {
    var symbols  = row.stock_symbol
    if (symbols.length === 0) {
      $("#finance-table tbody").append(
        "<tr class='results-row'><td class='symbol-letter'>" + row.key + "</td><td class='no-symbols'>No instruments!</td></tr>")        
    } else {
      $("#finance-table tbody").append(
			  "<tr class='results-row'><td class='symbol-letter'>" + row.key + "</td><td class='symbols'>" + symbols.join(', ') + "</td></tr>")
		}
  })
  $('#finance-table-display').show()
  setUpTableSorting()
}

function onError(request, textStatus, errorThrown) {
  writeDebug("Ajax request failed: XMLHttpRequest="+request.responseText+", textStatus: "+textStatus+", errorThrown: "+errorThrown);
}

function saveCookies(symbols, stats, start, end, graph) {
  $.cookie('symbols', symbols)
  $.cookie('stats-option', stats)
  $.cookie('start', start)
  $.cookie('end', end)
  $.cookie('graph', graph)
}

function sendRequest(action) {
  var toolbar = $.find('#master-toolbar')
  var symbols = $(toolbar).find('#symbols').val()
  var stats   = $(toolbar).find('.stats-option:selected').val()
  var start   = $(toolbar).find('#start').val()
  var end     = $(toolbar).find('#end').val()
  var graph   = $(toolbar).find('#graph:checked').length > 0
  saveCookies(symbols, stats, start, end, graph)
  
  $('#pong-display').hide()
  $('#finance-table-display').hide()
  
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

function cookied_defined(cookie) {
  return cookie !== undefined && cookie !== ""
}

$(document).ready(function () {
  $('.icon').click(function(){
    $('.banner').fadeIn('slow');
    $('.banner').fadeOut(2000);
  })
  setupDatePicker()
  setupDefaultDates()
  submitOnCarriageReturn($('.submit-on-CR'), $('#master-toolbar'))
  $('#symbols').focus()

  var symbols_val = $.cookie('symbols')
  if (cookied_defined(symbols_val)) {
    $('#symbols').val(symbols_val)
  }
  var stat_option_val = $.cookie('stats-option')
  if (cookied_defined(stat_option_val)) {
    $('#stats option[value='+stat_option_val+']').attr("selected", "selected");
  }
  var start_val = $.cookie('start')
  if (cookied_defined(start_val)) {
    $('#start').val(start_val)
  }
  var end_val = $.cookie('end')
  if (cookied_defined(end_val)) {
    $('#end').val(end_val)
  }
  var graph_val = $.cookie('graph')
  if (cookied_defined(graph_val)) {
    if (graph_val = "true")
      $('#graph').attr('checked', true)
    else
      $('#graph').attr('checked', false)
  }
});

