var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.SelectedEvent = function(sender, obj) {
		ScalaDNS.SelectedEvent.parent.constructor.call(this, sender);
		this.obj = obj;
	}
	
	ScalaDNS.extend(ScalaDNS.SelectedEvent, ScalaDNS.Event);
	
}());