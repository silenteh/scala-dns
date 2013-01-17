var ScalaDNS = ScalaDNS || {};

(function() {
	var viewManager = ScalaDNS.ViewManager;
	
	ScalaDNS.MainController = {
		domainNames: function() {
			var settings = {};
			viewManager.setView('DomainNamesView', settings);
		},
		domainRecords: function() {
			var params = ScalaDNS.Dispatcher.getActionParams('sets'),
				settings;
			
			if(params.length === 0) {
				settings = {};
			} else {
				settings = {
					DomainRecords: {domain: params[0]},
					DomainRecordForm: {domain: params[0]}
				};
			}
			viewManager.setView('DomainRecordsView', settings);
		},
		users: function() {
			var settings = {};
			viewManager.setView('UserView', settings);
		},
		//index: domainNames
	}
}());