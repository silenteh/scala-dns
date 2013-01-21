var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.DomainNameForm = function(container) {
		ScalaDNS.DomainNameForm.parent.constructor.call(this, 'DomainNameForm', container);
		
		this.domain = null;
		this._validator;
	}
	
	ScalaDNS.extend(ScalaDNS.DomainNameForm, ScalaDNS.BaseWidget);
	
	ScalaDNS.DomainNameForm.prototype.load = function(settings, callback) {
		ScalaDNS.DomainService.loadDomains(callback);
	}
	
	ScalaDNS.DomainNameForm.prototype.init = function() {
		var that = this;
		this._tpl = $('#zonesFormTemplate').clone().removeAttr('id').removeClass('hidden');
		this._validator = this._initFormValidation();
		
		$('#add-zone', this._tpl).bind('click', function(evt) {
			var name, orig_name;
			evt.preventDefault();
			
			if(that._validator.validate().valid === true) {
				name = $(':fname(domain_name)').val();
				if(that.domain !== null) {
					orig_name = that.domain.origin;
					that.domain.origin = name;
					ScalaDNS.fullDomains.replace(orig_name, name, that.domain);
					if(that.domain.SOA && that.domain.NS && that.domain.NS.length > 1) {
						ScalaDNS.DomainService.replaceDomain(that.domain, orig_name, function() {
							that._raiseDomainUpdate();
						});
					} else {
						ScalaDNS.fullDomains.set(name, that.domain);
						console.log('show warning');
						that._raiseDomainUpdate();
					}
				} else {
					that.domain = {origin: name, ttl: 86400};
					ScalaDNS.fullDomains.set(name, that.domain);
					that._raiseDomainUpdate();
					console.log('move to records');
				}
			}
		});
		
		$(':fname(domain_name)', this._tpl).bind('keyup', function() {
			if($(this).closest('.control-group').hasClass('error')) {
				that._validator.validate();
			}
		});
		
		$(this._tpl).bind('click', function(evt) {
			evt.stopPropagation();
		});
		
		ScalaDNS.onDomainSelect.bind(this, this.domainSelected);
	}
	
	ScalaDNS.DomainNameForm.prototype.draw = function() {
		$('.row-fluid', this.container).append(this._tpl);
	}
	
	ScalaDNS.DomainNameForm.prototype.dispose = function() {
		$('#add-zone', this._tpl).unbind();
		$(this._tpl).unbind();
		ScalaDNS.onDomainSelect.unbind(this, this.domainSelected);
	}
	
	ScalaDNS.DomainNameForm.prototype.domainSelected = function(e) {
		this.domain = e.obj;
		this.clearForm();
		this._validator = this._initFormValidation();
		if(this.domain !== null) {
			$('[name="domain_name"]', this._tpl).val(this.domain.origin);
		}
	}
	
	ScalaDNS.DomainNameForm.prototype.clearForm = function() {
		var form = $('form', this.container);
		$('input[type="text"]', form).val('');
		$('.control-group', form).removeClass('error');
		$('.help-inline', form).remove();
	}
	
	ScalaDNS.DomainNameForm.prototype._initFormValidation = function() {
		var form = $('form', this._tpl), that = this,
			exclude = [];
		
		if(this.domain !== null) {
			exclude.push(this.domain.origin);
		};
		
		return form.validateDNS({
			rules: {
				domain_name: {
					domainName: {
						absolute: true,
						relative: false
					},
					notEqualTo: {
						value: ScalaDNS.fullDomains.getKeys(),
						exclude: exclude
					}
				}
			},
			messages: {
				domain_name: {
					domainName: 'Not a valid domain name',
					notEqualTo: 'Domain name already exists'
				}
			},
			callback: that._onValidate
		});
	}
	
	ScalaDNS.DomainNameForm.prototype._onValidate = function(item, message) {
		var parent = $(item).closest('.control-group');
		if(message !== undefined) {
			parent.addClass('error');
			parent.append('<span class="help-inline">' + message + '</span>');
		} else if(parent.hasClass('error')) {
			$('.help-inline', parent).remove();
			parent.removeClass('error');
		}
	}
	
	ScalaDNS.DomainNameForm.prototype._raiseDomainUpdate = function() {
		ScalaDNS.onDomainUpdate.raise(new ScalaDNS.UpdatedEvent(this));
	}
	
}());