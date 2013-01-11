var ScalaDNS = ScalaDNS || {};

(function() {
	ScalaDNS.DomainNamesView = function(name) {
		ScalaDNS.DomainNamesView.parent.constructor.call(this, name);
		
		this.containers.set('zones', $('#zones'));
		
		this.widgets.set('zones', new ScalaDNS.DomainNames(this.containers.get('zones')));
		this.widgets.set('zoneform', new ScalaDNS.DomainNameForm(this.containers.get('zones')));
	}
	
	ScalaDNS.extend(ScalaDNS.DomainNamesView, ScalaDNS.BaseView);
	
	ScalaDNS.DomainNamesView.prototype.getTitle = function() {
		return 'Hosted Zones';
	}
}());