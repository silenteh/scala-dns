var ScalaDNS = ScalaDNS || {};

function foreach(data, callback) {
	var i;
	for(i = 0; i < data.length; i++) {
		callback(data[i], i);
	}
}

ScalaDNS.config = {
	urlBase: location.host,
	types: ['SOA', 'NS', 'MX', 'A', 'AAAA', 'CNAME', 'PTR', 'TXT']
}

ScalaDNS.extend = (function() {
	var F = function() {};
	return function(subClass, superClass) {
		F.prototype = superClass.prototype;
		subClass.prototype = new F();
		subClass.prototype.constructor = subClass;
		subClass.parent = superClass.prototype;
		
		if(superClass.prototype.constructor == Object.prototype.constructor) {
			superClass.prototype.constructor = superClass;
		}
	}
}())

ScalaDNS.start = function() {
	ScalaDNS.onDomainSelect = new ScalaDNS.EventHandler();
	ScalaDNS.onRecordSelect = new ScalaDNS.EventHandler();
	ScalaDNS.onDomainUpdate = new ScalaDNS.EventHandler();
	ScalaDNS.onRecordsUpdate = new ScalaDNS.EventHandler();
	ScalaDNS.View = null;
	ScalaDNS.domains = [];
	ScalaDNS.currentRecords = {};
	ScalaDNS.fullDomains = null;
	$.address.change(function() {
		try {
			ScalaDNS.Dispatcher.parseAction();
		} catch(e) {
			console.log(e);
			console.log(e.message);
			if(ScalaDNS.View === null) {
				$.address.value('/zones');
			}
		}
	});
}

$(document).ready(function() {
	ScalaDNS.start();
});