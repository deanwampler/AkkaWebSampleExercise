// JavaScript for the AkkaWebSampleExercise

$(document).ready(function () {
  $('.icon').click(function(){
    $('.start').fadeIn('slow');
    $('.start').fadeOut('slow');
  });
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
  sendRequest("primes");
}

function onSuccess(jsonString) {
  $('.start').fadeOut('slow');
  appendDebug(jsonString);
  var json = $.parseJSON(jsonString); 
  if (json === null) {
    writeError("Invalid data returned by AJAX query: " + jsonString);
  } else if (json["message"]) {
    writeInfo(json["message"]);
  } else if (json["ping replies"]) {
    writeInfo("Ping replies" + json["ping replies"]);
  } else if (json["info"]) {
    writeInfo(json["info"]);
  } else if (json["warn"]) {
    writeWarning(json["warn"]);
  } else if (json["warning"]) {
    writeWarning(json["warning"]);
  } else if (json["error"]) {
    writeInfo(json["error"]);
  } else {
    $(".primes-table").css("display", "table");
    for (var i = 0; i < json.length; i++) {
      var row = json[i]
      $(".primes-table").append(
        "<tr class='primes-row'><tr><td>"+row["from"]+"</td><td>"+row["to"]+"</td><td>"+row["number-of-primes"]+"</td></tr>");
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
  if (action != "ping") {
    $(".control").attr("disabled", "disabled");
    if (action == "start" || action == "restart") {
      keepPolling = true;
      $("#stop").attr("disabled", "");
      $("#restart").attr("disabled", "");
    } else if (action == "stop") {
      keepPolling = false;
      $("#start").attr("disabled", "");    
    }
  }
  sendRequest(action);
}
