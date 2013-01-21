var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.DomainRecordForm = function(container) {
		ScalaDNS.DomainRecordForm.parent.constructor.call(this, 'DomainRecordForm', container);
		
		this.domain = null;
		this.record = null;
		this._validator = null;
		
		this._status = {
			blocked: false,
			type: null,
			ipv4_policy: null,
			ipv6_policy: null,
			selected_ips: {}
		}
	}
	
	ScalaDNS.extend(ScalaDNS.DomainRecordForm, ScalaDNS.BaseWidget);
	
	ScalaDNS.DomainRecordForm.prototype.load = function(settings, callback) {
		var that = this;
		ScalaDNS.DomainService.loadDomains(function() {
			that.domain = ScalaDNS.fullDomains.get(settings.domain);
			callback();
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype.init = function() {
		if(this.domain === null) return;
		var that = this, ipWeightSwitch;
		this.form = this.initFormFunctions();
		this._tpl = $('#setsFormTemplate').clone().removeAttr('id').removeClass('hidden');
		this._weighted_ipv4_tpl = $('[data-name="a"] [data-type="rout-weight"] .control-group', this._tpl).clone();
		this._weighted_ipv6_tpl = $('[data-name="aaaa"] [data-type="rout-weight"] .control-group', this._tpl).clone();
		this._validator = this.initValidator();
		
		this.updateFormStatus();
		
		$(':fname(name)', this._tpl).bind('keydown', function(evt) {
			if(that._status.blocked === true) {
				evt.preventDefault();
			}
		});
		
		$('[name="typ"]', this._tpl).bind('change', function(evt) {
			if(that._status.blocked === true) {
				$('option', this).removeAttr('selected');
				$('option[value="' + that._status.type + '"]', this).attr('selected', true);
			} else {
				that._status.type = $(this).val();
				that.switchFormPart($(this).val().toLowerCase());
			}
		});
		
		$('[data-id="add-record"]', this._tpl).bind('click', function(evt) {
			ScalaDNS.AlertBox.clear();
			if(that._status.blocked === false) {
				var typ = $('[name="typ"]', this._tpl).val(),
					formPart = $('[data-type="rr-content"]:visible'),
					domain = jQuery.extend(true, {},  that.domain),
					validateHead, validateBody, record;
				evt.preventDefault();
				
				validateHead = that._validator.name.validate();
				
				switch(typ) {
					case 'A':
						if($('[data-id="ipv4_routing_simple"]').prop('checked')) {
							validateBody = that._validator[typ + 'Simple'].validate();
						} else {
							validateBody = that._validator[typ + 'Weighted'].validate();
						}
						break;
					case 'AAAA':
						if($('[data-id="ipv6_routing_simple"]').prop('checked')) {
							validateBody = that._validator[typ + 'Simple'].validate();
						} else {
							validateBody = that._validator[typ + 'Weighted'].validate();
						}
						break;
					default:
						validateBody = that._validator[typ].validate();
						break;
				}
				
				if(validateHead.valid && validateBody.valid) {
					newRecord = that.form[typ].parse(formPart);
					if(that.record !== null && that.record.typ === typ) {
						domain[that.record.typ].splice(that.record.id, 1, newRecord);
					} else {
						if(that.record !== null) {
							domain[that.record.typ].splice(that.record.id, 1);
						}
						domain[typ] = domain[typ] || [];
						domain[typ].push(newRecord);
					}
					
					if(domain.SOA && domain.NS && domain.NS.length > 1) {
						ScalaDNS.DomainService.saveDomain(domain, function(result) {
							if(result.code === 0) {
								ScalaDNS.onRecordsUpdate.raise(new ScalaDNS.UpdatedEvent(this, {}));
								that.record = null;
								that.domain = result.data[0];
								that.clearForm();
								if(result.data.length > 1) {
									ScalaDNS.AlertBox.showClear('<strong>Warning!</strong> The domains have been reorganized.');
								}
							} else {
								ScalaDNS.AlertBox.showClear(result.messages.join('<br/>'), 'error');
							}
						});
					} else {
						that.domain = domain;
						ScalaDNS.fullDomains.set(that.domain.origin, that.domain);
						that.clearForm();
						ScalaDNS.AlertBox.showClear('<strong>Warning!</strong> The domain could not be saved at this point because it does not contain all the required records. Make sure you add a SOA record and at least 2 NS records.');
						ScalaDNS.onRecordsUpdate.raise(new ScalaDNS.UpdatedEvent(this, {}));
					}
				}
			}
		});
		
		$('[data-id="cancel"]', this._tpl).bind('click', function(evt) {
			that.clearForm();
			that.updateFormStatus();
			that.record = null;
		});
		
		this._tpl.delegate('[data-name="a"] [data-type="add"]', 'click', function() {
			if(that._status.blocked === false) {
				that.ipAddRow(this, that._weighted_ipv4_tpl.clone());
			}
		});
		
		this._tpl.delegate('[data-name="aaaa"] [data-type="add"]', 'click', function() {
			if(that._status.blocked === false) {
				that.ipAddRow(this, that._weighted_ipv6_tpl.clone());
			}
		});
		
		this._tpl.delegate('[data-type="remove"]', 'click', function() {
			var typ = $(':fname(typ)', that._tpl).val();
			$(this).closest('.control-group').remove();
			that._status.blocked = false;
			that._validator[typ + 'WeightedDuplicity'].validate();
		});
		
		this._tpl.delegate('[name="ipv4_routing"]', 'click', function(evt) {
			if(that._status.blocked === false) {
				that._status.ipv4_policy = $(this).attr('data-id');
				that.ipWeightSwitch(this, 'a', 'ipv4', that._weighted_ipv4_tpl.clone());
			} else {
				evt.preventDefault();
				$('[data-id="' + that._status.ipv4_policy + '"]').prop('checked', true);
			}
		});
		
		this._tpl.delegate('[name="ipv6_routing"]', 'click', function(evt) {
			if(that._status.blocked === false) {
				that._status.ipv6_policy = $(this).attr('data-id');
				that.ipWeightSwitch(this, 'aaaa', 'ipv6', that._weighted_ipv6_tpl.clone());
			} else {
				evt.preventDefault();
				$('[data-id="' + that._status.ipv6_policy + '"]').prop('checked', true);
			}
		});
		
		this._tpl.delegate('.control-group input[type="text"], .control-group textarea', 'keyup', function() {
			that._onValidate(this);
		});
		
		this._tpl.delegate('[name="ipv4_address[]"]', 'keyup', function() {
			that._status.blocked = false;
			that._validator.AWeightedDuplicity.validate();
		});
		
		this._tpl.delegate('[name="ipv4_value"]', 'keyup', function() {
			that._status.blocked = false;
			that._validator.ASimpleDuplicity.validate();
		});
		
		this._tpl.delegate('[name="ipv6_address[]"]', 'keyup', function() {
			that._status.blocked = false;
			that._validator.AAAAWeightedDuplicity.validate();
		});
		
		this._tpl.delegate('[name="ipv6_value"]', 'keyup', function() {
			that._status.blocked = false;
			that._validator.AAAASimpleDuplicity.validate();
		});
		
		this._tpl.bind('click', function(evt) {
			evt.stopPropagation();
		});
		
		this._tpl.delegate('[data-type="error-msg"] button', 'click', function() {
			$(this).closest('[data-type="error-msg"]').remove();
		});
		
		this._tpl.delegate('[data-toggle="buttons-radio"] button', 'click', function(evt) {
			var buttonGroup = $(this).closest('[data-toggle="buttons-radio"]'),
				parent = $(this).closest('.control-group');
			
			$('button', buttonGroup).removeClass('active');
			$(this).addClass('active');
			$('input[type="text"]', parent).val($(this).text());
		});

		$('input[type="text"]', $('[data-toggle="buttons-radio"]', this._tpl).closest('.control-group')).bind('keyup', function() {
			var parent = $(this).closest('.control-group'),
				value = $(this).val();
			$('[data-toggle="buttons-radio"] button').removeClass('active');
			if(value !== '') {
				$('[data-toggle="buttons-radio"] button', parent).filter(function() { return $(this).text() === value; }).addClass('active');
			}
		});
		
		ScalaDNS.onRecordSelect.bind(this, this.recordSelected);
		ScalaDNS.onDomainUpdate.bind(this, this.domainUpdated);
	}
	
	ScalaDNS.DomainRecordForm.prototype.draw = function() {
		if(this.domain !== null) {
			this.drawForm();
			$('.row-fluid', this.container).append(this._tpl);
		}
	}
	
	ScalaDNS.DomainRecordForm.prototype.dispose = function() {
		$(':fname(name)', this._tpl).unbind();
		$('[name="typ"]', this._tpl).unbind();
		$('[data-id="add-record"]', this._tpl).unbind();
		$('[data-id="cancel"]', this._tpl).unbind();
		if(this._tpl) {
			this._tpl.unbind();
			this._tpl.undelegate();
		}
		ScalaDNS.onRecordSelect.unbind(this, this.recordSelected);
		ScalaDNS.onDomainUpdate.unbind(this, this.domainUpdated);
	}
	
	ScalaDNS.DomainRecordForm.prototype.drawForm = function() {
		this.clearForm();
		$('[data-type="rr-content"]', this._tpl).hide();
		$('[data-type="rout-weight"]', this._tpl).hide();
		
		if(this.record === null) {
			$('[data-name="a"]', this._tpl).show();
		} else {
			$('[data-type="form-title"]', this._tpl).text('Edit Record Set');
			$('[data-id="add-record"]', this._tpl).text('Update Record Set');
			this.drawFormPart(this.record.typ, this.record.data);
		}
		this.updateFormStatus();
	}
	
	ScalaDNS.DomainRecordForm.prototype.clearForm = function(typ) {
		var form;
		this._status.blocked = false;
		if(!typ) {
			form = this._tpl;
			$('[data-type="form-title"]', this._tpl).text('Create Record Set');
			$('[data-id="add-record"]', this._tpl).text('Create Record Set');
		} else {
			form = $('[data-name="' + typ.toLowerCase() + '"]', this._tpl);
		}
		$('input[type="text"]', form).val('');
		$('input[type="hidden"]', form).val('');
		$('textarea', form).val('');
		
		$('[name="ipv4_routing"]', form).prop('checked', false);
		$('[name="ipv4_routing"]:first', form).prop('checked', true);
		$('[name="ipv6_routing"]', form).prop('checked', false);
		$('[name="ipv6_routing"]:first', form).prop('checked', true);
		
		$('[data-name="a"] [data-type="rout-weighted .control-group"]', form).remove();
		$('[data-name="a"] [data-type="rout-weighted"]', form).append(this._weighted_ipv4_tpl);
		$('[data-name="aaaa"] [data-type="rout-weighted .control-group"]', form).remove();
		$('[data-name="aaaa"] [data-type="rout-weighted"]', form).append(this._weighted_ipv6_tpl);
		
		$('[data-type="rout-simple"]', form).show();
		$('[data-type="rout-weight"]', form).hide();
		
		$('.control-group', form).removeClass('error');
		$('.help-inline', form).remove();
		
		$('[data-toggle="buttons-radio"] button', form).removeClass('active');
		
		ScalaDNS.AlertBox.clear();
		
		this.refreshNameValidator();
	}
	
	ScalaDNS.DomainRecordForm.prototype.drawFormPart = function(typ, data) {
		var formPart = $('[data-name="' + typ.toLowerCase() + '"]', this._tpl);
		$('[name="name"]', this._tpl).val(data.name);
		$('[name="typ"] option', this._tpl).removeAttr('selected');
		$('[name="typ"] option[value="' + typ + '"]', this._tpl).attr('selected', 'selected');
		this.form[typ].populate(data, formPart);
		formPart.show();
	}
	
	ScalaDNS.DomainRecordForm.prototype.switchFormPart = function(typ) {
		$('[data-type="rr-content"]', this._tpl).hide();
		$('[data-name="' + typ + '"]', this._tpl).show();
		this.refreshNameValidator();
	}
	
	ScalaDNS.DomainRecordForm.prototype.isWeighted = function(data) {
		if(jQuery.isArray(data)) {
			var i = 0, weighted = false;
			while(i < data.length && weighted === false) {
				weighted = this.isWeighted(data[i]);
				i++;
			}
			return weighted;
		} else if(jQuery.isPlainObject(data)) {
			return data.weight && data.weight > 1;
		} else {
			return false;
		}
	}
	
	ScalaDNS.DomainRecordForm.prototype.initFormFunctions = function() {
		var that = this,
		
		populateAddress = function(data, container) {
			var row_tpl;
			if(container.attr('data-name') === 'aaaa') {
				row_tpl = that._weighted_ipv6_tpl;
			} else {
				row_tpl = that._weighted_ipv4_tpl;
			}
			$(':dtype(rout-weight) .control-group', container).remove();
			$(':dtype(ip-value)', container).empty();
			$(':dtype(routing-policy)', container).removeAttr('checked');
			foreach(data.value, function(item, index) {
				row = row_tpl.clone();
				if(index === 0) {
					$('button', row).attr('data-type', 'add');
				} else {
					$('button', row).attr('data-type', 'remove');
					$('button i', row).removeClass('icon-plus');
					$('button i', row).addClass('icon-minus');
					$(':dtype(ip-value)', container).val($(':dtype(ip-value)', container).val() + '\n');
				}
				$(':dtype(ip-weight)', row).val(item.weight);
				$(':dtype(ip-address)', row).val(item.ip);
				$(':dtype(rout-weight)', container).append(row);
				$(':dtype(ip-value)', container).val($(':dtype(ip-value)', container).val() + item.ip);
			});
			if(that.isWeighted(data.value)) {
				$(':dname(routing-weighted)', container).prop('checked', true);
				$(':dtype(rout-simple)', container).hide();
				$(':dtype(rout-weight)', container).show();
			} else {
				$(':dname(routing-simple)', container).prop('checked', true);
				$(':dtype(rout-weight)', container).hide();
				$(':dtype(rout-simple)', container).show();
			}
		},
		
		populateText = function(data, container) {
			$(':dtype(text-value)', container).val(data.value);
		},
		
		populateMx = function(data, container) {
			$(':dtype(text-param)', container).val(data.priority);
			$(':dtype(text-value)', container).val(data.value);
		},
		
		populateNs = function(data, container) {
			$(':dtype(text-param)', container).val(data.weight);
			$(':dtype(text-value)', container).val(data.value);
		},
		
		populateSoa = function(data, container) {
			$(':fname(soa_serial)', container).val(data.serial);
			$(':fname(soa_mname)', container).val(data.mname);
			$(':fname(soa_rname)', container).val(data.rname);
			$(':fname(soa_ttl)', container).val(data.at);
			$(':fname(soa_refresh)', container).val(data.refresh);
			$(':fname(soa_retry)', container).val(data.retry);
			$(':fname(soa_expire)', container).val(data.expire);
			$(':fname(soa_minimum)', container).val(data.minimum);
			
			$('input[type="text"]', $('[data-toggle="buttons-radio"]', that._tpl).closest('.control-group')).each(function() {
				var parent = $(this).closest('.control-group'),
					value = $(this).val().toLowerCase();
				$('[data-toggle="buttons-radio"] button', parent).removeClass('active');
				$('[data-toggle="buttons-radio"] button', parent).filter(function() { return $(this).text() === value }).addClass('active');
			});
		},
		
		populateMText = function(data, container) {
			foreach(data.value, function(item, index) {
				if(index > 0) {
					$(':dtype(text-values)', container).append('\n');
				}
				$(':dtype(text-values)', container).append(item);
			});
		},
		
		parseAddress = function(container) {
			var json, value = [], weight = 1;
			json = {
				name: $(':fname(name)', that._tpl).val(),
				'class': 'in'
			};
			if($(':dname(routing-simple)', container).is(':checked')) {
				foreach($(':dtype(ip-value)', container).val().split('\n'), function(item) {
					if(item !== '') {
						value.push({weight: weight, ip: item});
					}
				});
			} else {
				$(':dtype(rout-weight) .control-group', container).each(function() {
					if($(':dtype(ip-address)', this).val() !== '') {
						if($(':dtype(ip-weight)', this).val() != '') {
							weight = $(':dtype(ip-weight)', this).val();
						}
						value.push({
							weight: weight,
							ip: $(':dtype(ip-address)', this).val()
						});
					}
				});
			}
			json.value = value;
			return json;
		},
		
		parseText = function(container) {
			return {
				name: $(':fname(name)', that._tpl).val(),
				'class': 'in',
				value: $(':dtype(text-value)', container).val()
			}
		},
		
		parseMx = function(container) {
			return {
				name: $(':fname(name)', that._tpl).val(),
				'class': 'in',
				priority: $(':dtype(text-param)', container).val(),
				value: $(':dtype(text-value)', container).val()	
			}
		},
		
		parseNs = function(container) {
			return {
				name: $(':fname(name)', that._tpl).val(),
				'class': 'in',
				weight: $(':dtype(text-param)', container).val(),
				value: $(':dtype(text-value)', container).val()	
			}
		},
		
		parseSoa = function(container) {
			return {
				name: $(':fname(name)', that._tpl).val(),
				at: $(':fname(soa_ttl)', that._tpl).val(),
				'class': 'in',
				mname: $(':fname(soa_mname)', container).val(),
				rname: $(':fname(soa_rname)', container).val(),
				serial: $(':fname(soa_serial)', container).val(),
				refresh: $(':fname(soa_refresh)', container).val(),
				retry: $(':fname(soa_retry)', container).val(),
				expire: $(':fname(soa_expire)', container).val(),
				minimum: $(':fname(soa_minimum)', container).val()
			}
		},
		
		parseMText = function(container) {
			var json, value = [];
			json = {
				name: $(':fname(name)', that._tpl).val(),
				'class': 'in'
			};
			foreach($(':dtype(text-values)', container).val().split('\n'), function(item) {
				value.push(item);
			});
			json.value = value;
			return json;
		};
		
		return {
			'A': {
				populate: populateAddress,
				parse: parseAddress
			},
			'AAAA': {
				populate: populateAddress,
				parse: parseAddress
			},
			'CNAME': {
				populate: populateText,
				parse: parseText
			},
			'MX': {
				populate: populateMx,
				parse: parseMx
			},
			'NS': {
				populate: populateNs,
				parse: parseNs
			},
			'PTR': {
				populate: populateText,
				parse: parseText
			},
			'SOA': {
				populate: populateSoa,
				parse: parseSoa
			},
			'TXT': {
				populate: populateMText,
				parse: parseMText
			}
		}
	}
	
	ScalaDNS.DomainRecordForm.prototype.ipAddRow = function(target, template) {
		var parent = $(target).closest('.control-group');
		
		$('input[type="text"]', template).val('');
		$('button', template).attr('data-type', 'add');
		
		$(target).attr('data-type', 'remove');
		$('i', target).removeClass('icon-plus');
		$('i', target).addClass('icon-minus');
		
		parent.before(template);
	}
	
	ScalaDNS.DomainRecordForm.prototype.ipWeightSwitch = function(target, typ, prefix, template) {
		var parent = $(target).parent().closest('[data-name]'),
			cont_simple = $('[data-type="rout-simple"]', parent),
			cont_weight = $('[data-type="rout-weight"]', parent),
			textarea = $('[name="' + prefix + '_value"]', parent),
			that = this,
			tpl;
		
		if($(target).val() === 'weighted') {
			if(!$('[data-type="rout-weight"]', parent).is(':visible')) {
				var data = textarea.val().split('\n');
				$('.control-group', cont_weight).remove();
				if(data.length == 0) {
					cont_weight.append(template);
				} else {
					var unique_ips = {};
					foreach(data, function(item, index) {
						if(unique_ips[item] === undefined) {
							unique_ips[item] = item;
							tpl = template.clone();
							if(that._status.selected_ips[item]) {
								$('[name="' + prefix + '_weight[]"]', tpl).val(that._status.selected_ips[item]);
							} else {
								$('[name="' + prefix + '_weight[]"]', tpl).val('');
							}
							$('[name="' + prefix + '_address[]"]', tpl).val(item);
							if(index == 0) {
								$('button', tpl).attr('data-type', 'add');
							} else {
								$('button', tpl).attr('data-type', 'remove');
								$('button i', tpl).removeClass('icon-plus');
								$('button i', tpl).addClass('icon-minus');
							}
							cont_weight.append(tpl);
						}
					});
				}
				cont_simple.hide();
				cont_weight.show();
			}
		} else {
			if(!$('[data-type="rout-simple"]', parent).is(':visible')) {
				var value;
				this._status.selected_ips = {};
				textarea.val('');
				$('[data-type="rout-weight"] .control-group', parent).each(function() {
					value = $('[name="' + prefix + '_address[]"]', this).val();
					if(value != '' && that._status.selected_ips[value] === undefined) {
						that._status.selected_ips[value] = $('[name="' + prefix + '_weight[]"]', this).val();
						if(textarea.val().indexOf(value) < 0) {
							if(textarea.val() != '') {
								textarea.val(textarea.val() + '\n');
							}
							textarea.val(textarea.val() + value);
						}
					}
				});
				cont_weight.hide();
				cont_simple.show();
			}
		}
	}
	
	ScalaDNS.DomainRecordForm.prototype.updateFormStatus = function() {
		this._status = {
			blocked: this._status.blocked,
			type: $(':fname(typ)', this._tpl).val(),
			ipv4_policy: $(':fname(ipv4_routing):checked').attr('data-id'),
			ipv6_policy: $(':fname(ipv6_routing):checked').attr('data-id'),
			selected_ips: {}
		}
	}
	
	ScalaDNS.DomainRecordForm.prototype.recordSelected = function(e) {
		this.record = e.obj;
		this.drawForm();
	}
	
	ScalaDNS.DomainRecordForm.prototype.initValidator = function() {
		var form = $('form', this._tpl), that = this, callback, duplicityCallback;
		
		callback = this._onValidate;
		
		duplicityCallback = function(item, message) {
			that._onValidate(item, message);
			that._status.blocked = that._status.blocked || message !== undefined;
		}
		
		return {
			name: that._initNameValidation(form, callback),
			ASimpleDuplicity: that._initASimpleDuplicityValidation(form, duplicityCallback),
			AWeightedDuplicity: that._initAWeightedDuplicityValidation(form, duplicityCallback),
			ASimple: that._initASimpleValidation(form, callback),
			AWeighted: that._initAWeightedValidation(form, callback),
			AAAASimpleDuplicity: that._initAAAASimpleDuplicityValidation(form, duplicityCallback),
			AAAAWeightedDuplicity: that._initAAAAWeightedDuplicityValidation(form, duplicityCallback),
			AAAASimple: that._initAAAASimpleValidation(form, callback),
			AAAAWeighted: that._initAAAAWeightedValidation(form, callback),
			CNAME: that._initCNAMEValidation(form, callback),
			MX: that._initMXValidation(form, callback),
			NS: that._initNSValidation(form, callback),
			PTR: that._initPTRValidation(form, callback),
			SOA: that._initSOAValidation(form, callback),
			TXT: that._initTXTValidation(form, callback)
		}
	}
	
	ScalaDNS.DomainRecordForm.prototype.refreshNameValidator = function() {
		var validator = this._validator.name;
		this._validator.name = this._initNameValidation(validator.form, validator.callback);
	}
	
	ScalaDNS.DomainRecordForm.prototype._onValidate = function(item, message) {
		var parent = $(item).closest('.control-group'), hlptext;
		if(message) {
			parent.addClass('error');
			if($('span.help-inline', parent).length === 0) {
				hlptext = $('small', parent);
				if(hlptext.length === 0) {
					parent.append('<span class="help-inline"><small><strong>' + message + '</strong></small></span>');
				} else {
					hlptext.prev().before('<span class="help-inline"><small><strong>' + message + '</strong></small></span>');
				}
			}
		} else {
			if(parent.hasClass('error')) {
				$('span.help-inline', parent).remove();
				parent.removeClass('error');
			}
		}
	}
	
	ScalaDNS.DomainRecordForm.prototype._initNameValidation = function(form, callback) {
		var exclude = [];
		if(this.record !== null) {
			exclude.push(this.record.data.name);
		}
		
		return form.validateDNS({
			rules: {
				name: {
					hostName: {
						relative: true,
						absolute: false
					},
					notEqualTo: {
						value: this._getRecordNames(),
						exclude: exclude
					}
				}
			},
			messages: {
				name: {
					hostName: 'Not a valid host name',
					notEqualTo: 'The host name already exists'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initASimpleDuplicityValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				'ipv4_value': {
					uniqueInText: '\n'
				}
			},
			messages: {
				'ipv4_value': {
					uniqueInText: 'Duplicate IP address'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initAWeightedDuplicityValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				'ipv4_address[]': {
					uniqueInGroup: true
				}
			},
			messages: {
				'ipv4_address[]': {
					uniqueInGroup: 'Duplicate IP address'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initASimpleValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				'ipv4_value': {
					required: true,
					ipv4: {
						delimiter: '\n'
					}
				}
			},
			messages: {
				'ipv4_value': {
					required: 'No IP address specified',
					ipv4: 'Not a valid IP address',
					uniqueInGroup: 'Duplicate IP address'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initAWeightedValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				'ipv4_address[]': {
					requiredGroup: true,
					ipv4: true
				}
			},
			messages: {
				'ipv4_address[]': {
					requiredGroup: 'No IP address specified',
					ipv4: 'Not a valid IP address'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initAAAASimpleDuplicityValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				'ipv6_value': {
					uniqueInText: '\n'
				}
			},
			messages: {
				'ipv6_value': {
					uniqueInText: 'Duplicate IPv6 address'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initAAAAWeightedDuplicityValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				'ipv6_address[]': {
					requiredInGroup: true
				}
			},
			messages: {
				'ipv6_address[]': {
					uniqueInGroup: 'Duplicate IPv6 address'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initAAAASimpleValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				'ipv6_value': {
					required: true,
					ipv6: {
						delimiter: '\n',
						uniqueInGroup: true
					}
				}
			},
			messages: {
				'ipv6_value': {
					required: 'No IP address specified',
					ipv6: 'Not a valid IPv6 address',
					uniqueInGroup: 'Duplicate IPv6 address'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initAAAAWeightedValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				'ipv6_address[]': {
					requiredGroup: true,
					ipv6: true
				}
			},
			messages: {
				'ipv6_address[]': {
					requiredGroup: 'No IP address specified',
					ipv6: 'Not a valid IPv6 address'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initCNAMEValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				cname_value: {
					domainName: {
						absolute: true,
						relative: true
					},
					notEqualToElem: 'name'
				}
			},
			messages: {
				cname_value: {
					domainName: 'Not a valid domain name',
					notEqualToElem: 'Cname points to itself'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initMXValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				mx_priority: {
					number: true
				},
				mx_value: {
					domainName: {
						absolute: true,
						relative: true
					}
				}
			},
			messages: {
				mx_priority: {
					number: 'Not a valid number'
				},
				mx_value: {
					domainName: 'Not a valid domain name'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initNSValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				ns_weight: {
					number: true
				},
				ns_value: {
					domainName: {
						absolute: true,
						relative: true
					}
				}
			},
			messages: {
				ns_value: {
					number: 'Not a valid number'
				},
				ns_value: {
					domainName: 'Not a valid domain name'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initPTRValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				ptr_value: {
					domainName: {
						absolute: true,
						relative: true
					}
				}
			},
			messages: {
				ptr_value: {
					domainName: 'Not a valid domain name'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initSOAValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				soa_mname: {
					domainName: {
						absolute: true,
						relative: true
					}
				},
				soa_rname: {
					domainName: {
						absolute: true,
						relative: true
					}
				},
				soa_ttl: {
					ttl: true
				},
				soa_refresh: {
					ttl: true
				},
				soa_retry: {
					ttl: true
				},
				soa_expire: {
					ttl: true
				},
				soa_minimum: {
					ttl: true
				}
			},
			messages: {
				soa_mname: {
					domainName: 'Not a valid domain name'
				},
				soa_rname: {
					domainName: 'Not a valid domain name'
				},
				soa_ttl: {
					ttl: 'Not a valid time record'
				},
				soa_refresh: {
					ttl: 'Not a valid time record'
				},
				soa_retry: {
					ttl: 'Not a valid time record'
				},
				soa_expire: {
					ttl: 'Not a valid time record'
				},
				soa_minimum: {
					ttl: 'Not a valid time record'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.DomainRecordForm.prototype._initTXTValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				txt_value: {
					required: true
				}
			},
			messages: {
				txt_value: {
					required: 'Text must not be empty'
				}
			},
			callback: callback
		})
	}
	
	ScalaDNS.DomainRecordForm.prototype._getRecordNames = function() {
		var typ, records, array = [], i;
		if(this.record !== null) {
			typ = this.record.typ;
		} else {
			typ = $(':fname(typ)', this._tpl).val();
		}
		records = this.domain[typ]
		if(records) {
			for(i = 0; i < records.length; i++) {
				array.push(records[i].name);
			}
		}
		return array;
	}

	ScalaDNS.DomainRecordForm.prototype.domainUpdated = function(e) {
		this.domain = e.obj;
		this.refreshNameValidator();
	}
}());