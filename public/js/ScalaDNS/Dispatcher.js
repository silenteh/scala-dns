var ScalaDNS = ScalaDNS || {};

ScalaDNS.Dispatcher = (function() {
	var actions = {
		'zones': {
			title: 'Domain Names',
			fn: ScalaDNS.MainController.domainNames
		},
		'sets': {
			title: 'Record Sets',
			fn: ScalaDNS.MainController.domainRecords
		},
		'users': {
			title: 'Users',
			fn: ScalaDNS.MainController.users
		}
	}, currentAction = null;
	
	function getCurrentAction() {
		var name = $.address.path().substr(1).split('/')[0];
		if(!name) {
			name = 'zones';
		}
		return name;
	}
	
	function buildUrl(action, params) {
		var param, query = '', sep = '?';
		if (params) {
			for(param in params) {
				query += sep + param + '=' + params[param];
				sep = '&';
			}
		}
		return action + query;
	}
	
	function execute(action, params, disableEffect) {
		var url = buildUrl(action, params);
		$.address.value(url);
		return true;
	}
	
	function parseAction() {
		var name, action, query, fn, params;
		
		name = getCurrentAction();
		action = actions[name];
		if (!action) throw 'Unknown page required: ' + name;
		
		params = getQuery();
		
		currentAction = {action: name, query: params};
		
		fn = action.fn;
		fn(params);
	}
	
	function getActionParams(action) {
		var action = action || getCurrentAction(),
			path = $.address.path(),
			actionIndex = path.indexOf(action) + action.length,
			residue, params, filteredParams, i;
		
		if(actionIndex > -1) {
			residue = path.substr(actionIndex + 1);
		} else {
			residue = path.substr(1);
		}
		
		params = residue.split('/');
		filteredParams = [];
		
		for(i = 0; i < params.length; i++) {
			if(params[i] !== '') {
				filteredParams.push(params[i]);
			}
		}
		
		return filteredParams;
	}
	
	function getQuery() {
		var query = $.address.queryString(),
			params = {};
		if (query) {
			params = eval('({' + query.replace(/=/g, ':"').replace(/&/g, '",') + '"})');
		}
		return params;
	}
	
	return {
		getCurrentAction: getCurrentAction,
		buildUrl: buildUrl,
		execute: execute,
		parseAction: parseAction,
		getQuery: getQuery,
		getActionParams: getActionParams
	}
}());