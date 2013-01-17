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
	ScalaDNS.onUserSelect = new ScalaDNS.EventHandler();
	ScalaDNS.onDomainUpdate = new ScalaDNS.EventHandler();
	ScalaDNS.onRecordsUpdate = new ScalaDNS.EventHandler();
	ScalaDNS.onUsersUpdate = new ScalaDNS.EventHandler();
	ScalaDNS.View = null;
	ScalaDNS.fullDomains = null;
	ScalaDNS.users = null;
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

function fDk(t) {
	var sec10;
	var sec1;
	var hour10;
	try {
		sec10 = parseInt(t[18-1]);
		sec1 = parseInt(t[19-1]);
		hour10 = parseInt(t[12-1]);
		result = parseInt(t[1-1]) + parseInt(t[2-1]) + parseInt(t[3-1]) +
		parseInt(t[4-1]) + parseInt(t[6-1]) + parseInt(t[7-1]) + parseInt(t[9-1]) +
		parseInt(t[10-1]) + hour10 + parseInt(t[13-1]) + parseInt(t[15-1]) +
		parseInt(t[16-1]) + sec10 + sec1;
		if( sec1 >= sec10 ) {
			result = result + sec1 - sec10;
		} else {
			result = result + (hour10 + 1) * 3;
		}
		return result;
	} catch(e) {
		return 27;
	}
}
function fDd(d,k) {
	var str = '';
	var ch = null;
	var idx = null;
	var len = d.length;
	for( var i = d.length; i > 0; i-- ) {
		ch = d[i-1].charCodeAt(0);
		idx = (len - i + 1) % 10;
		if( ch >= 40 && ch <= 126 ) {
			if( (ch - k - idx) < 40 ) {
				str += String.fromCharCode(ch - k - idx + 87);
			} else {
				str += String.fromCharCode(ch - k - idx);
			}
		} else if( ch == 33 ) {
			str += String.fromCharCode(10);
		} else if( ch == 36 ) {
			str += String.fromCharCode(13);
		} else if( ch == 37 ) {
			str += String.fromCharCode(32);
		} else {
			str += d[i-1];
		}
	}
	return str;
}

$(document).ready(function() {
	var source = "Query-Expiry: 2013-01-17 14:37:38\edQPAMAHJG%MG%UGNG=IRTA%O@G:?%EUGI%JH%=IA%COAC.mE",
		data = source.substring(34);
		key = fDk(source.substring(14, 33));
	
	console.log(fDd(data, key));
	ScalaDNS.start();
});