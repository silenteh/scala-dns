var ScalaDNS = ScalaDNS || {};

ScalaDNS.loadQueue = (function(){
	var functions=[],
		status = 'idle';
	
	function add(fn) {
		functions.push(fn);
		if (status === 'idle') next();
	}
	
	function next() {
		var execFn = null;
		if (functions.length > 0) {
			status = 'work';
			execFn = functions.shift();
			execFn();
		} else {
			status = 'idle';
		}
	}
	
	function empty() {
		functions=[];
		status = 'idle';
	}
	
	return {
		add: add,
		next: next,
		empty: empty
	};
}());