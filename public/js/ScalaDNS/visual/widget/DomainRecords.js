var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.DomainRecords = function(container) {
		ScalaDNS.DomainRecords.parent.constructor.call(this, 'DomainRecords', container);
		this.domain = null;
		this.records = null;
		this.selectedRecord = null;
	}
	
	ScalaDNS.extend(ScalaDNS.DomainRecords, ScalaDNS.BaseWidget);
	
	ScalaDNS.DomainRecords.prototype.load = function(settings, callback) {
		var that = this;
		ScalaDNS.DomainService.loadDomains(function() {
			that.domain = ScalaDNS.fullDomains.get(settings.domain);
			callback();
		});
	};
	
	ScalaDNS.DomainRecords.prototype.init = function() {
		var that = this;
		this._tpl = $('#setsTemplate').clone().removeAttr('id').removeClass('hidden');
		this._row_tpl = $('tbody tr:first', this._tpl).clone();
		this._alert_tpl = $('#alertTemplate').clone().removeAttr('id').removeClass('hidden');
		this._ctrlKeyDown = false;
		
		$(this._tpl).delegate('tbody tr', 'click', function(evt) {
			var selected = $(this).hasClass('row_selected'),
				count_selected = $('tr.row_selected', $(this).parent()).length;
			
			evt.stopPropagation();
			if(that._ctrlKeyDown === true) {
				if($(this).hasClass('row_selected')) {
					$(this).removeClass('row_selected');
					count_selected--;
				} else {
					$(this).addClass('row_selected');
					count_selected++;
				}
				
				if(count_selected === 1) {
					$('[data-type="button-bar"] button').removeAttr('disabled');
				} else if(count_selected === 0) {
					$('[data-type="button-bar"] button', that._tpl).attr('disabled', 'disabled');
				} else {
					$('[data-id="edit-rs"]').attr('disabled', 'disabled');
				}
			} else {
				$('tbody tr', that.container).removeClass('row_selected');
				if(selected === false || count_selected > 1) {
					$(this).addClass('row_selected');
					that.selectedRecord = this;
					
					$('[data-type="button-bar"] button', that._tpl).removeAttr('disabled');
				} else {
					$('[data-type="button-bar"] button', that._tpl).attr('disabled', 'disabled');
					that.selectedRecord = null;
				}
			}
		});
		
		$('[data-id="edit-rs"]', this._tpl).bind('click', function(evt) {
			evt.stopPropagation();
			that._raiseSelectRecord(
				$(that.selectedRecord).attr('data-id'),
				$(that.selectedRecord).attr('data-typ')
			);
		});
		
		$('[data-id="delete-rs"]', this._tpl).bind('click', function(evt) {
			var typ, name, updated, alert = that._alert_tpl.clone();;
			evt.stopPropagation();
			$('.alert', that.container).remove();
			$('tbody tr.row_selected', this._tpl).each(function() {
				name = $(':dtype(record-name)', this).text();
				typ = $(':dtype(record-type)', this).text();
				$(this).remove();
				updated = [];
				$.each(that.domain[typ], function() {
					if(this.name !== name) {
						updated.push(this);
					}
				});
				if(updated.length === 0) {
					delete that.domain[typ];
				} else {
					that.domain[typ] = updated;
				}
			});
			if(that.domain.SOA && that.domain.NS && that.domain.NS.length > 1) {
				ScalaDNS.DomainService.saveDomain(that.domain);
			} else {
				alert.append('<strong>Warning!</strong> The domain could not be updated at this point because it does not contain all the required records. Make sure you add a SOA record and at least 2 NS records.');
				$('button', alert).click(function() {
					$(this).closest('div').remove();
				});
				that.container.prepend(alert);
			}
			$(this).attr('disabled', 'disabled');
			that._raiseDomainUpdate();
		});
		
		$('html').bind('click', function(evt) {
			$('tbody tr', that._tpl).removeClass('row_selected');
			$('[data-type="button-bar"] button', that._tpl).attr('disabled', 'disabled');
		});
		
		$('html').bind('keydown', function(evt) {
			if(evt.keyCode === 17) {
				that._ctrlKeyDown = true;
			}
		});
		
		$('html').bind('keyup', function(evt) {
			if(evt.keyCode === 17) {
				that._ctrlKeyDown = false;
			}
		});
		
		ScalaDNS.onRecordsUpdate.bind(this, this._recordsUpdated);
	};
	
	ScalaDNS.DomainRecords.prototype.draw = function() {
		var i, recordSet, record, data, row, that = this;
		$('tbody', this._tpl).empty();
		
		foreach(ScalaDNS.config.types, function(typ) {
			if(that.domain[typ]) {
				recordSet = that.domain[typ];
				for(i = 0; i < recordSet.length; i++) {
					row = that._row_tpl.clone();
					record = recordSet[i];
					data = that.buildValue(typ, record);
					
					row.attr('data-id', i);
					row.attr('data-typ', typ);
					row.addClass('even');
					$('[data-type="record-name"]', row).html(record.name);
					$('[data-type="record-type"]', row).html(typ);
					
					if(data.value) {
						$('[data-type="record-value"]', row).html(data.value);
					} else {
						$('[data-type="record-value"]', row).html(data);
					}
					if(data.ttl) {
						$('[data-type="record-ttl"]', row).html(data.ttl);
					}
					if(data.weight) {
						$('[data-type="record-weight"]', row).html(data.weight);
					}
					
					$('tbody', that._tpl).append(row);
				}
			}
		});
		
		this.container.append(this._tpl);
	};
	
	ScalaDNS.DomainRecords.prototype.dispose = function() {
		$(this.container).undelegate();
		$('html').unbind();
		ScalaDNS.onRecordsUpdate.unbind(this, this._recordsUpdated);
	};
	
	ScalaDNS.DomainRecords.prototype.buildValue = function(typ, data) {
		var value;
		switch(typ) {
			case 'A':
			case 'AAAA':
				value = {weight: '', value: ''};
				foreach(data.value, function(item, index) {
					if(index > 0) {
						value.value += '<br/>';
						value.weight += '<br/>';
					}
					value.weight += item.weight;
					value.value += item.ip;
				});
				break;
			case 'NS':
				value = {
					weight: data.weight, 
					value: data.value
				};
				break;
			case 'CNAME':
			case 'PTR':
				value = data.value;
				break;
			case 'MX':
				value = data.priority + ' ' + data.value;
				break;
			case 'SOA':
				value = {
					ttl: data.at,
					value: data.mname + ' ' + data.rname + ' ' + data.serial + ' ' + data.refresh + ' ' + data.retry + ' ' + data.expire + ' ' + data.minimum
				};
				break;
			case 'TXT':
				value = '';
				foreach(data.value, function(item, index) {
					if(index > 0) {
						value += '<br/>';
					}
					value += item;
				});
				break;
		}
		return value;
	};
	
	ScalaDNS.DomainRecords.prototype._raiseSelectRecord = function(id, typ) {
		var recordNames;
		if(!id || !typ || this.domain === null) {
			return;
		}
		recordNames = this._getRecordNames(typ);
		ScalaDNS.onRecordSelect.raise(
			new ScalaDNS.SelectedEvent(this, {
				typ: typ, 
				data: this.domain[typ][id], 
				names: recordNames, 
				origin: this.domain.origin
			})
		);
	}
	
	ScalaDNS.DomainRecords.prototype._raiseDomainUpdate = function() {
		ScalaDNS.onDomainUpdate.raise(new ScalaDNS.UpdatedEvent(this, this.domain));
	}
	
	ScalaDNS.DomainRecords.prototype._getRecordNames = function(typ) {
		var array = [], i;
		if(jQuery.isArray(this.domain[typ])) {
			for(i = 0; i < this.domain[typ].length; i++) {
				array.push(this.domain[typ][i].name);
			}
		}
		return array;
	}
	
	ScalaDNS.DomainRecords.prototype._recordsUpdated = function() {
		this.domain = ScalaDNS.fullDomains.get(this.domain.origin);
		this.draw();
	}
	
}());