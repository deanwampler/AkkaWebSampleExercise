// JavaScript for the AkkaWebSampleExercise

$(document).ready(function () {
  $('.icon').click(function(){
    $('.banner').fadeIn('slow');
    $('.banner').fadeOut('slow');
  });
  $('.date-pick').datePicker({
    clickInput: true,
    startDate: '01/01/2010',
    endDate: (new Date()).asString()
  });
  $('.date-pick').dpSetOffset(25,0);
});

function writeDebug(string) {
  $("#debug").html(string);
}

function appendDebug(string) {
  var dbg = $("#debug");
  dbg.html(dbg.html() + "<br/>" + string);
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

function getNext() {
  sendRequest("results");
}

function onSuccess(jsonString) {
  var json = $.parseJSON(jsonString); 
  if (json === null) {
    writeError("Invalid data returned by AJAX query: " + jsonString);
  } else if (json["message"]) {
    writeInfo(json["message"]);
  } else if (json["ping replies"]) {
    var replies ="["
    for (var i=0; i<json["ping replies"].length; i++) {
      replies += json["ping replies"][i]["pong"] + ", "
    }
    replies += "]"
    writeInfo("Ping replies received from: " + replies);
  } else if (json["info"]) {
    writeInfo(json["info"]);
  } else if (json["warn"]) {
    writeWarning(json["warn"]);
  } else if (json["warning"]) {
    writeWarning(json["warning"]);
  } else if (json["error"]) {
    writeInfo(json["error"]);
  } else {
    // Plot the results data. 
    // TODO: Note that we currently retrieve all data calculated, including data already retrieved.
    $(".results-table").css("display", "table");
    for (var i = 0; i < json.length; i++) {
      var array = json[i];
      for (var j = 0; j < array.length; j++) {
        var row = array[j];
        $(".results-table").append(
          "<tr class='results-row'><tr><td>"+row["from"]+"</td><td>"+row["to"]+"</td><td>"+row["number-of-results"]+"</td></tr>");
      }
    }
  }
  setTimeout(getNext, 3000);
}

function onError(request, textStatus, errorThrown) {
  writeError("Ajax request failed: XMLHttpRequest="+request.responseText+", textStatus: "+textStatus+", errorThrown: "+errorThrown);
}

function sendRequest(action) {
  if (! keepPolling) return;
  $.ajax({
    url: "ajax/"+action,
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

var keepPolling = true

function serverControl(action) {
  var action2 = action;
  sendRequest(action);
}

function intVal(textId) {
  var text = $("#"+textId);
  return parseInt(text.val(), 10);
}

function integerOnly(textId) {
  if(intVal(textId) === NaN) {
    alert("You must enter an integer.");
  }
}

function fixMin(minId, maxId) {
  var min = intVal(minId);
  var max = intVal(maxId);
  if (min > max) {
    $("#"+minId).val(max-1);
    alert("Resetting min value to "+(max-1));
  }
}

function fixMax(minId, maxId) {
  var min = intVal(minId);
  var max = intVal(maxId);
  if (min > max) {
    $("#"+maxId).val(min+1);
    alert("Resetting max value to "+(min+1));
  }
}
