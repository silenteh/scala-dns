var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.Tabs = function(container) {
		ScalaDNS.Tabs.parent.constructor.call(this, 'Menu', container);
	};
	
	ScalaDNS.extend(ScalaDNS.Tabs, ScalaDNS.BaseWidget);
	
	ScalaDNS.Tabs.prototype.load = function(settings, callback) {
		callback();
	};
	
	ScalaDNS.Tabs.prototype.init = function() {
		var that = this;
		this._tpl = $('#tabsTemplate').clone().removeAttr('id').removeClass('hidden');
		$('[data-menu]', this._tpl).click(function(evt) {
			evt.preventDefault();
			if($(this).hasClass('disabled') === false) {
				ScalaDNS.Dispatcher.execute($(this).attr('data-menu'))
				that.switchTab();
			}
		});
		
		ScalaDNS.onDomainSelect.bind(this, this.domainSelected);
	};
	
	ScalaDNS.Tabs.prototype.draw = function() {
		this.container.append(this._tpl);
		this.switchTab();
	};
	
	ScalaDNS.Tabs.prototype.dispose = function() {
		
	};
	
	ScalaDNS.Tabs.prototype.domainSelected = function(e) {
		var domain = e.obj,
			link = $('[data-menu^="sets"]', this._tpl);
		
		if(domain === null || domain.origin === '') {
			link.attr('data-menu', 'sets');
			link.addClass('disabled');
		} else {
			link.attr('data-menu', 'sets/' + domain.origin);
			link.removeClass('disabled');
		}
	};
	
	ScalaDNS.Tabs.prototype.switchTab = function() {
		$('[data-menu]', this._tpl).removeClass('active');
		$('[data-menu^="' + ScalaDNS.Dispatcher.getCurrentAction() + '"]', this._tpl).addClass('active');
	};
	
}());