var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.Menu = function(container) {
		ScalaDNS.Menu.parent.constructor.call(this, 'Menu', container);
	};
	
	ScalaDNS.extend(ScalaDNS.Menu, ScalaDNS.BaseWidget);
	
	ScalaDNS.Menu.prototype.load = function(settings, callback) {
		callback();
	};
	
	ScalaDNS.Menu.prototype.init = function() {
		var that = this;
		$('[data-menu]').click(function(evt) {
			evt.preventDefault();
			ScalaDNS.Dispatcher.execute($(this).attr('data-menu'))
			that.switchTab();
		});
		
		ScalaDNS.onDomainSelect.bind(this, this.domainSelected);
	};
	
	ScalaDNS.Menu.prototype.draw = function() {
		this.switchTab();
	};
	
	ScalaDNS.Menu.prototype.dispose = function() {
		
	};
	
	ScalaDNS.Menu.prototype.domainSelected = function(e) {
		var domain = e.obj,
			link = $('[data-menu^="sets"]', this.container);
		
		if(domain === null || domain.origin === '') {
			link.attr('data-menu', 'sets');
		} else {
			link.attr('data-menu', 'sets/' + domain.origin);
		}
	};
	
	ScalaDNS.Menu.prototype.switchTab = function() {
		$('[data-menu]').removeClass('active');
		$('[data-menu^="' + ScalaDNS.Dispatcher.getCurrentAction() + '"]').addClass('active');
	};
	
}());