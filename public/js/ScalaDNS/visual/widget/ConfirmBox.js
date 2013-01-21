var ScalaDNS = ScalaDNS || {};

(function() {
	ScalaDNS.ConfirmBox = function(container) {
		ScalaDNS.ConfirmBox.parent.constructor.call(this, 'ConfirmBox', container);
		
		this.onConfirm;
		this.onCancel;
		this.messageType;
	}
	
	ScalaDNS.extend(ScalaDNS.ConfirmBox, ScalaDNS.BaseWidget);
	
	/*ScalaDNS.ConfirmBox.prototype.load = function(settings, callback) {
		callback();
	}*/
	
	ScalaDNS.ConfirmBox.prototype.init = function() {
		var that = this;
		this._tpl = $('#modal-from-dom');
		$(':dtype(confirm)', this._tpl).bind('click', function(evt) {
			evt.preventDefault();
			if(jQuery.isFunction(that.onConfirm)) {
				that.onConfirm();
			}
			that.onConfirm = null;
			that.onCancel = null;
			that._tpl.modal('hide');
		});
		$(':dtype(cancel)', this._tpl).bind('click', function(evt) {
			evt.preventDefault();
			if(jQuery.isFunction(that.onCancel)) {
				that.onCancel();
			}
			that.onConfirm = null;
			that.onCancel = null;
			that._tpl.modal('hide');
		});
		$('.close', this._tpl).bind('click', function(evt) {
			evt.preventDefault();
			if(jQuery.isFunction(that.onCancel)) {
				that.onCancel();
			}
			that.onConfirm = null;
			that.onCancel = null;
			that._tpl.modal('hide');
		});
		$('html').delegate('.modal-backdrop', 'click', function(evt) {
			evt.stopPropagation();
		});
		$(this._tpl).click(function(evt) {
			evt.stopPropagation();
		});
	}
	
	ScalaDNS.ConfirmBox.prototype.draw = function() {
		
	}
	
	ScalaDNS.ConfirmBox.prototype.dispose = function() {
		
	}
	
	ScalaDNS.ConfirmBox.prototype.setMessage = function(title, message, msgType) {
		$('h3', this._tpl).text(title);
		$('.modal-body', this._tpl).html(message);
		if(this.messageType !== undefined && this.messageType !== null) {
			$(':dtype(confirm)', this._tpl).removeClass(this.messageType);
		}
		if(msgType) {
			this.messageType = msgType;
			$(':dtype(confirm)', this._tpl).addClass(this.messageType);
		}
	}
	
	ScalaDNS.ConfirmBox.prototype.show = function(onConfirm, onCancel) {
		this.onConfirm = onConfirm;
		this.onCancel = onCancel;
		this._tpl.modal('show');
	}
}());