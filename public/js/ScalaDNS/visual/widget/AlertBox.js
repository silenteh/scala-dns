var ScalaDNS = ScalaDNS || {};

(function() {
	ScalaDNS.AlertBox = function(container) {
		ScalaDNS.AlertBox.parent.constructor.call(this, 'AlertBox', container);
	}
	
	ScalaDNS.extend(ScalaDNS.AlertBox, ScalaDNS.BaseWidget);
	
	ScalaDNS.AlertBox.prototype.init = function() {
		this._tpl = $('#alertTemplate').clone().removeAttr('id').removeClass('hidden');
		$(this.container).delegate('button', 'click', function() {
			$(this).closest('div').remove();
		});
	}
	
	ScalaDNS.AlertBox.prototype.dispose = function() {
		this.container.undelegate();
	}
	
	ScalaDNS.AlertBox.prototype.show = function(message, typ) {
		var alert = this._tpl.clone();
		if(typ) {
			alert.addClass('alert-' + typ);
		}
		alert.append(message);
		this.container.append(alert);
	}
	
	ScalaDNS.AlertBox.prototype.showClear = function(message, typ) {
		this.container.empty();
		this.show(message, typ);
	}
	
	ScalaDNS.AlertBox.prototype.clear = function(message, typ) {
		this.container.empty();
	}
}());