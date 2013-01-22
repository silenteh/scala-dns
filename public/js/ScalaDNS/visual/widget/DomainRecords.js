var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.DomainRecords = function(container) {
		ScalaDNS.DomainRecords.parent.constructor.call(this, 'DomainRecords', container);
		this.domain = null;
		this.records = null;
		this.selectedRecord = null;
		this.datatable;
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
		this._ctrlKeyDown = false;
		
		$(this._tpl).delegate('tbody tr', 'click', function(evt) {
			if($('td.dataTables_empty', this).length == 0) {
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
			evt.stopPropagation();
			ScalaDNS.ConfirmBox.show(function() {
				var data, typ, name, updated, id;
				$('tbody tr.row_selected', that._tpl).each(function() {
					id = $(this).data('id');
					data = that.datatable.fnGetData(this);
					name = data[0];
					typ = data[1];
					that.domain[typ].splice(id, 1);
					if(that.domain[typ].length === 0) {
						delete that.domain.typ;
					}
				});
				if(that.domain.SOA && that.domain.NS && that.domain.NS.length > 1) {
					ScalaDNS.DomainService.saveDomain(that.domain, function(result) {
						if(result.code === 0) {
							that.domain = result.data[0];
							if(result.data.length > 1) {
								ScalaDNS.AlertBox.showClear('<strong>Warning!</strong> The domains have been reorganized.');
							}
						} else {
							ScalaDNS.AlertBox.showClear(result.messages.join('<br/>'), 'error');
						}
					});
				} else {
					ScalaDNS.fullDomains.set(that.domain.origin, that.domain);
					ScalaDNS.AlertBox.showClear('<strong>Warning!</strong> The domain could not be updated at this point because it does not contain all the required records. Make sure you add a SOA record and at least 2 NS records.');
				}
				$(this).attr('disabled', 'disabled');
				ScalaDNS.onRecordsUpdate.raise(new ScalaDNS.UpdatedEvent(this, {}));
				that._raiseDomainUpdate();
			});
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
		var i, recordSet, record, data, row, that = this, addedData, addedRow;
		if(this.datatable === undefined) {
			this.datatable = $('table', this._tpl).dataTable({
				'fnDrawCallback': function(item) {
					$('tr', item.nTBody).addClass('gradeU');
				}
			});
		}
		this.datatable.fnClearTable();
		//$('tbody', this._tpl).empty();
		
		ScalaDNS.ConfirmBox.setMessage('Delete records', '<p>You are about to delete the selected record(s). This action is irreversible.</p><p>Do you want to proceed?</p>', 'btn-danger');
		if(this.domain !== null) {
			foreach(ScalaDNS.config.types, function(typ) {
				if(that.domain[typ]) {
					recordSet = that.domain[typ];
					for(i = 0; i < recordSet.length; i++) {
						//row = that._row_tpl.clone();
						row = [];
						record = recordSet[i];
						data = that.buildValue(typ, record);
						
						row.push(record.name);
						row.push(typ);
						
						if(data.value) {
							row.push(data.value);
						} else {
							row.push(data);
						}
						if(data.ttl) {
							row.push(data.ttl);
						} else {
							row.push('');
						}
						if(data.weight) {
							row.push(data.weight);
						} else {
							row.push('');
						}
						row.push('');
						addedData = that.datatable.fnAddData(row);
						addedRow = that.datatable.fnSettings().aoData[addedData[0]].nTr;
						$(addedRow).attr('data-id', i);
						$(addedRow).attr('data-typ', typ);
						//$('table tr:last', that._tpl).attr('data-id', i);
						/*row.attr('data-id', i);
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
						
						$('tbody', that._tpl).append(row);*/
					}
				}
			});
			
			this.container.append(this._tpl);
		}
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
				value = {weight: '', value: ''};
				foreach(data.value, function(item, index) {
					if(index > 0) {
						value.value += '<br/>';
						value.weight += '<br/>';
					}
					value.weight += item.weight;
					value.value += item.ns;
				});
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
				id: id,
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