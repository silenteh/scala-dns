var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.DomainNames = function(container) {
		ScalaDNS.DomainNames.parent.constructor.call(this, 'DomainNames', container);
	}
	
	ScalaDNS.extend(ScalaDNS.DomainNames, ScalaDNS.BaseWidget);
	
	ScalaDNS.DomainNames.prototype.load = function(settings, callback) {
		ScalaDNS.DomainService.loadDomains(callback);
	}
	
	ScalaDNS.DomainNames.prototype.init = function() {
		var that = this;
		this._tpl = $('#zonesTemplate').clone().removeAttr('id').removeClass('hidden');
		this._row_tpl = $('tbody tr:first', this._tpl).clone();
		
		this._raiseSelectDomain();
		
		$(this._tpl).delegate('tbody tr', 'click', function(evt) {
			var selected = $(this).hasClass('row_selected');
			evt.stopPropagation();
			$('tbody tr', this._tpl).removeClass('row_selected');
			if(selected === false) {
				$(this).addClass('row_selected');
				that._raiseSelectDomain($('[data-type="domain-name"]', this).text());
			} else {
				that._raiseSelectDomain();
			}
		});
		
		$('[data-id="delete-hz"]', this._tpl).bind('click', function(evt) {
			var rowsToRemove = $('tbody tr.row_selected'), row, name;
			evt.stopPropagation();
			rowsToRemove.each(function() {
				row = this;
				name = $(':dtype(domain-name)', row).text();
				ScalaDNS.DomainService.removeDomain(name, function() {
					row.remove();
				});
			});
		});
		
		$('html').bind('click', function() {
			$('tbody tr', this._tpl).removeClass('row_selected');
			that._raiseSelectDomain();
		});
		
		ScalaDNS.onDomainUpdate.bind(this, this._domainUpdated);
	}
	
	ScalaDNS.DomainNames.prototype.draw = function() {
		var i, domain, row, count;
		$('tbody', this._tpl).empty();
		if(ScalaDNS.fullDomains !== null) {
			ScalaDNS.fullDomains.reset();
			while(domain = ScalaDNS.fullDomains.next()) {
				row = this._row_tpl.clone();
				row.attr('data-id', i);
				$('[data-type="domain-name"]', row).text(domain.origin);
				if(i % 2 == 0) {
					row.addClass('even');
				} else {
					row.addClass('odd');
				}
				count = 0;
				jQuery.each(domain, function() {
					if(jQuery.isArray(this)) {
						count += this.length;
					}
				});
				$(':dtype(domain-record-count)', row).text(count);
				$('tbody', this._tpl).append(row);
			}
		} else {
			
		}
		this.container.append(this._tpl);
	}
	
	ScalaDNS.DomainNames.prototype.dispose = function() {
		$('html').unbind();
		ScalaDNS.onDomainUpdate.unbind(this, this._domainUpdated);
	}
	
	ScalaDNS.DomainNames.prototype._raiseSelectDomain = function(name) {
		var domain = null;
		if(name !== undefined) {
			domain = ScalaDNS.fullDomains.get(name);
		}
		ScalaDNS.onDomainSelect.raise(new ScalaDNS.SelectedEvent(this, domain));
	}
	
	ScalaDNS.DomainNames.prototype._domainUpdated = function(e) {
		this.draw();
	}
	
}());