var ScalaDNS = ScalaDNS || {};

(function() {
	
	var baseWidgets = new ScalaDNS.List(),
		baseContainers = new ScalaDNS.List(),
		baseWidgetsInitialized = false,
		baseWidgetsLoaded = false;
	
	ScalaDNS.BaseView = function(name) {
		this.name = name;
		this.containers = new ScalaDNS.List();
		this.widgets = new ScalaDNS.List();
		this._stopLoad = false;
		
		if(baseWidgetsInitialized === false) {
			baseContainers.set('confirm_box', $('#confirmBox'));
			ScalaDNS.ConfirmBox = new ScalaDNS.ConfirmBox(baseContainers.get('confirm_box'))
			baseWidgets.set('ConfirmBox', ScalaDNS.ConfirmBox);
		}
	}
	
	ScalaDNS.BaseView.prototype = {
		load: function(settings, callback) {
			var widget, counter = 0, that = this, total = this.widgets.getLength() + baseWidgets.getLength();
			
			if(baseWidgetsLoaded === false) {
				baseWidgetsLoaded = true;
				baseWidgets.reset();
				while(widget = baseWidgets.next()) {
					widget.load(settings[widget.name], loaded);
				}
			} else {
				counter = baseWidgets.getLength();
			}
			this.widgets.reset();
			while(widget = this.widgets.next()) {
				widget.load(settings[widget.name], loaded);
			}
			
			function loaded() {
				if(that._stopLoad) return;
				if(++counter == total) {
					callback();
				}
			}
		},
		init: function() {
			var widget;
			if(baseWidgetsInitialized === false) {
				baseWidgetsInitialized = true;
				baseWidgets.reset();
				while(widget = baseWidgets.next()) {
					widget.init();
				}
			}
			this.widgets.reset();
			while(widget = this.widgets.next()) {
				widget.init();
			}
		},
		draw: function(fullRedraw) {
			var widget;
			if(fullRedraw === true) {
				baseWidgets.reset();
				while(widget = baseWidgets.next()) {
					widget.draw();
				}
			}
			this.widgets.reset();
			while(widget = this.widgets.next()) {
				widget.draw();
			}
		},
		dispose: function() {
			var widget;
			this.widgets.reset();
			while(widget = this.widgets.next()) {
				widget.dispose();
			}
		},
		stopLoading: function() {
			this._stopLoad = true;
		},
		show: function(callback) {
			var container, counter = 0, total = this.containers.getLength();
			
			this.containers.reset();
			while((container = this.containers.next())) {
				container.hide();
			}
			this.draw(false)
			this.containers.reset();
			while((container = this.containers.next())) {
				container.show();
				shown();
			}
			
			function shown() {
				if(++counter == total) {
					if(callback) callback();
				}
			}
		},
		hide: function(callback) {
			var container, counter = 0, total = this.containers.getLength();
			
			this.containers.reset();
			while(container = this.containers.next()) {
				container.hide();
				container.empty();
				hidden();
			}
			
			function hidden() {
				if(++counter == total) {
					if(callback) callback();
				}
			}
		},
		getTitle: function() {
			return 'ScalaDNS';
		}
	}
	
}());