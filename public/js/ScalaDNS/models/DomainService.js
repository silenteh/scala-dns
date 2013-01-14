var ScalaDNS = ScalaDNS || {};

ScalaDNS.DomainService = (function() {
	var i;
	
	function loadDomains(callback) {
		if(ScalaDNS.fullDomains === null) {
			$.get('http://' + ScalaDNS.config.urlBase + '/domains/', {}, function(data) {
				if(data && data.domains) {
					ScalaDNS.fullDomains = new ScalaDNS.List();
					jQuery.each(data.domains, function() {
						ScalaDNS.fullDomains.set(this.origin, this);
					});
				}
				if(callback) {
					callback();
				}
			}, 'json');
		} else {
			callback();
		}
	}
	
	function loadNames(callback) {
		$.get('http://' + ScalaDNS.config.urlBase + '/domains/?menu', {}, function(data) {
			ScalaDNS.domains = data;
			if(callback) {
				callback();
			}
		}, 'json');
	}
	
	function loadRecords(domain, callback) {
		$.get('http://' + ScalaDNS.config.urlBase + '/domains/' + domain, {}, function(data) {
			callback(data.domain[1]);
		}, 'json');
	}
	
	function updateRecords(typ, record, old_record, domain, callback) {
		var records;
		if(old_record !== null) {
			foreach(domain[old_record.typ], function(orig_record, index) {
				if(old_record.data.name === orig_record.name) {
					if(old_record.typ === typ) {
						domain[old_record.typ].splice(index, 1, record);
					} else {
						domain[old_record.typ].splice(index, 1);
					}
				}
			});
		}
		records = domain[typ] || [];
		if(old_record === null || old_record.typ !== typ) {
			records.push(record);
		}
		
		domain[typ] = records;
		ScalaDNS.fullDomains.set(domain.origin, domain);
		
		/*$.post('http://' + ScalaDNS.config.urlBase + '/domains/', {data: JSON.stringify(ScalaDNS.currentRecords)}, function(data) {
			if(data.code == 0) {
				ScalaDNS.currentRecords[typ] = data.data;
			}
			ScalaDNS.onRecordsUpdate.raise(new ScalaDNS.UpdatedEvent(this));
			if(callback) {
				callback();
			}
		}, 'json');*/
		if(callback) {
			callback();
		}
	}
	
	function saveDomain(domain, callback) {
		$.post('http://' + ScalaDNS.config.urlBase + '/domains/', {data: JSON.stringify(domain)}, function(result) {
			if(result.code == 0) {
				jQuery.each(result.data, function() {
					ScalaDNS.fullDomains.set(this.origin, this);
				});
			}
			//ScalaDNS.onRecordsUpdate.raise(new ScalaDNS.UpdatedEvent(this));
			if(callback) {
				callback(result);
			}
		}, 'json');
		/*ScalaDNS.fullDomains.set(domain.origin, domain);
		ScalaDNS.onRecordsUpdate.raise(new ScalaDNS.UpdatedEvent(this, {}));
		if(callback) {
			callback();
		}*/
	}
	
	function removeDomain(domainName, callback) {
		$.post('http://' + ScalaDNS.config.urlBase + '/domains/', {'delete': domainName}, function(result) {
			if(callback) {
				callback();
			}
		});
	}
	
	return {
		loadDomains: loadDomains,
		loadNames: loadNames,
		loadRecords: loadRecords,
		saveDomain: saveDomain,
		updateRecords: updateRecords,
		removeDomain: removeDomain
	};
	
}());