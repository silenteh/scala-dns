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
	
	ScalaDNS.Menu.prototype.draw = function() {
		this.container.append(this._tpl);
		this.switchTab();
	};
	
	ScalaDNS.Menu.prototype.dispose = function() {
		
	};
	
	ScalaDNS.Menu.prototype.domainSelected = function(e) {
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
	
	ScalaDNS.Menu.prototype.switchTab = function() {
		$('[data-menu]').removeClass('active');
		$('[data-menu^="' + ScalaDNS.Dispatcher.getCurrentAction() + '"]').addClass('active');
	};
	
}());