// JavaScript for the AkkaWebSampleExercise

$(document).ready(function () {
  $('.start').fadeOut('slow');
  $('.icon').click(function(){
    $('.start').fadeIn('slow');
    $('.start').fadeOut('slow');
  });
});

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

function onSuccess(json) {
  $("#primes").append("<div class='datum'>"+json+"</div>");
  setTimeout(getNext, 2000);
}

function onError(request, textStatus, errorThrown) {
  writeError("Ajax request failed: XMLHttpRequest="+request.responseText+", textStatus: "+textStatus+", errorThrown: "+errorThrown);
}

function sendRequest(action) {
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

function serverControl(action) {
  if (action != "ping") {
    $(".control").attr("disabled", "disabled");
    if (action == "start" || action == "restart") {
      $("#stop").attr("disabled", "");
      $("#restart").attr("disabled", "");
    } else if (action == "stop") {
      $("#start").attr("disabled", "");    
    }
  }
  sendRequest(action);
}
