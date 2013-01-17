var ScalaDNS = ScalaDNS || {};

(function() {
	ScalaDNS.UserView = function(name) {
		ScalaDNS.UserView.parent.constructor.call(this, name);
		
		this.containers.set('users', $('#users'));
		
		this.widgets.set('users', new ScalaDNS.Users(this.containers.get('users')));
		this.widgets.set('userform', new ScalaDNS.UserForm(this.containers.get('users')));
	}
	
	ScalaDNS.extend(ScalaDNS.UserView, ScalaDNS.BaseView);
	
	ScalaDNS.UserView.prototype.getTitle = function() {
		return 'Users';
	}
}());