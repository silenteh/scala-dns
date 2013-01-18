var ScalaDNS = ScalaDNS || {};

(function() {
	ScalaDNS.BaseWidget = function(name, container) {
		this.name = name;
		this.container = container;
	}
	
	ScalaDNS.BaseWidget.prototype = {
		load: function(settings, callback) {
			callback();
		},
		init: function() {
			
		},
		draw: function() {
			
		},
		dispose: function() {
			
		}
	}
}());