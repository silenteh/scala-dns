var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.UpdatedEvent = function(sender, obj) {
		ScalaDNS.UpdatedEvent.parent.constructor.call(this, sender);
		this.obj = obj;
	}
	
	ScalaDNS.extend(ScalaDNS.UpdatedEvent, ScalaDNS.Event);
	
}());