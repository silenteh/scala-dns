var ScalaDNS = ScalaDNS || {};

(function() {
	var _viewsClasses = {
		'DomainNamesView': ScalaDNS.DomainNamesView,
		'DomainRecordsView': ScalaDNS.DomainRecordsView,
		'UserView': ScalaDNS.UserView
	},
	_currentView = null,
	_currentQueryString = null,
	_currentSettings = null,
	_loadingView = null,
	_loadingQueryString = null,
	_loadingSettings = null,
	_effectEnabled = true,
	_changingView = false;
	
	ScalaDNS.ViewManager = {
		isChangingView: function() {
			return _changingView;
		},
		setView: function(name, settings) {
			var oldView = _currentView,
				oldQuery = _currentQueryString,
				that = this,
				queryString = $.address.queryString();
			
			if(oldView !== null && oldView.name === name && oldQuery === queryString) {
				return;
			}
			
			if(_changingView === true && oldView !== null) {
				_loadingView.stopLoading();
				_loadingView = null;
				_currentView.hide(function() {
					_changingView = false;
					that.setView(name, settings);
				});
				return;
			}
			if(_viewsClasses[name]) {
				_changingView = true;
				_loadingView = new _viewsClasses[name](name, settings);
				_loadingQueryString = queryString;
				_loadingSettings = settings;
				_loadingView.load(settings, function() {
					that._switchView(oldView, _loadingView);
				});
			} else {
				throw 'Unknown view: ' + name;
			}
		},
		refreshView: function() {
			var that = this,
				oldView = _currentView, 
				name = _currentView.name;
				
			_loadingView = new _viewsClasses[name](name, _currentSettings);
			_loadingView.load(_currentSettings, function() {
				that._switchView(oldView, _loadingView, false);
			});
		},
		_switchView: function(oldView, newView) {
			var that = this;
			if (oldView !== null) {
				oldView.dispose();
				oldView.hide(function(){
					ScalaDNS.View = _currentView = newView;
					_currentQueryString = _loadingQueryString;
					_currentSettings = _loadingSettings;
					newView.init();
					newView.show(null);
					_changingView = false;
					$.address.title(newView.getTitle());
				});
			} else {
				ScalaDNS.View = _currentView = newView;
				_currentQueryString = _loadingQueryString;
				_currentSettings = _loadingSettings;
				newView.init();
				newView.draw(true);
				_changingView = false;
				$.address.title(newView.getTitle());
			}
		}
	}
}());