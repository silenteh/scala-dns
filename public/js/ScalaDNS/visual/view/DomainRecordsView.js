var ScalaDNS = ScalaDNS || {};

(function() {
	ScalaDNS.DomainRecordsView = function(name) {
		ScalaDNS.DomainRecordsView.parent.constructor.call(this, name);
		
		this.containers.set('sets', $('#sets'));
		
		this.widgets.set('sets', new ScalaDNS.DomainRecords(this.containers.get('sets')));
		this.widgets.set('setform', new ScalaDNS.DomainRecordForm(this.containers.get('sets')));
	}
	
	ScalaDNS.extend(ScalaDNS.DomainRecordsView, ScalaDNS.BaseView);
	
	ScalaDNS.DomainRecordsView.prototype.getTitle = function() {
		return 'Record Sets';
	}
}());