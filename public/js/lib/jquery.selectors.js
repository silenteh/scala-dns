(function($) {
	$.expr[':'].dt = function(obj, index, meta, stack) {
		var data = meta[3], selector, value;
		
		if(data.indexOf(',') > -1) {
			selector = data.substr(0, data.indexOf(','));
			value = data.substr(data.indexOf(',') + 1).replace(/^\s+|\s+$/g, '');
		} else {
			selector = data;
			value = null;
		}
		
		if(value === null) {
			return $(obj).attr('data-' + selector) !== undefined;
		} else {
			return $(obj).attr('data-' + selector) === value;
		}
	}
	
	$.expr[':'].dname = function(obj, index, meta, stack) {
		var data = meta[3];
		return $(obj).attr('data-name') === data;
	}
	
	$.expr[':'].dtype = function(obj, index, meta, stack) {
		var data = meta[3];
		return $(obj).attr('data-type') === data;
	}
	
	$.expr[':'].fname = function(obj, index, meta, stack) {
		var data = meta[3];
		return $(obj).attr('name') === data;
	}
}(jQuery));