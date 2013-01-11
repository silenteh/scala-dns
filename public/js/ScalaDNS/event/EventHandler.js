var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.EventHandler = function() {
		this.callbacks = [];
	}
	
	ScalaDNS.EventHandler.prototype.bind = function(obj, callback) {
		this.callbacks.push({
			obj: obj,
			fn: callback
		});
	} 
	
	ScalaDNS.EventHandler.prototype.unbind = function(obj, fn) {
		var i, clb;
		
		i = this.callbacks.length;
		while(--i >= 0) {
			clb = this.callbacks[i];
			if(clb.obj === obj && clb.fn === fn) {
				this.callbacks.splice(i, 1);
				break;
			}
		}
	}
	
	ScalaDNS.EventHandler.prototype.raise = function(e) {
		var i, clb;
		
		i = this.callbacks.length;
		while (--i>=0) {
			clb = this.callbacks[i];
			if (clb.obj) {
				clb.fn.call(clb.obj, e);
			} else {
				clb.fn(e);
			}
		}
	}
	
}());