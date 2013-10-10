$(window).load(function() {
	w = new widget("update", 1000, function(data) {
		$("#loading").detach();
		$("p").show();
		$(".error").hide();
		$("#earliestStartTime").text(data.earliestStartTime);
		$("#latestStartTime").text(data.latestStartTime);
		$("#programName").text(data.programName);
	});
});