(function($) {
	
	Validator = function(form, settings) {
		var that = this;
		this.form = form;
		if(settings) {
			this.rules = settings.rules;
			this.messages = settings.messages;
			this.callbacks = settings.callbacks,
			this.callback = settings.callback;
		} else {
			throw 'Please specify validation settings';
		}
		this.group_uniques = {};
		
		this.validateFunctions = {
			domainName : function(item, params, ignoreCallback) {
				var name = $(item).val() || '',
					labels = name.split('.'),
					absolute = true,
					relative = true,
					origin = '',
					valid = true,
					i;
				
				ignoreCallback = ignoreCallback || false;

				
				if(params && params.absolute !== undefined) {
					absolute = params.absolute;
				}
				if(params && params.relative !== undefined) {
					relative = params.relative;
				}
				if(params && params.origin) {
					origin = params.origin;
				}
				
				if(name.length < 1 || name.length > 255) {
					valid = false;
				}
				if(relative === false && absolute === true && name !== '@' && name.lastIndexOf('.') !== name.length - 1) {
					valid = false;
				}
				if(absolute === false && relative === true && name !== '@' && name !== origin && name.lastIndexOf('.') === name.length - 1) {
					valid = false;
				}
				
				if(absolute === true && name.lastIndexOf('.') === name.length - 1) {
					labels.splice(labels.length - 1, 1);
				}
				for(i = 0; i < labels.length; i++) {
					if(labels[i].length < 1 || labels[i].length > 63) {
						valid = false;
						break;
					}
				}
				
				if(ignoreCallback === false) {
					that.applyCallback(item, 'domainName', valid);
				}
				
				return valid;
			},
			hostName : function(item, params) {
				var valid, name;
				
				valid = this.domainName(item, params);
				if(valid === true) {
					name = $(item).val();
					valid = name === '@' || name.match(/([a-zA-Z0-9\*]{1}([a-zA-Z0-9\*\-]*[a-zA-Z0-9\*]{1})*\.{0,1})*/g)[0] === name;
				}
				
				that.applyCallback(item, 'hostName', valid);
				return valid;
			},
			ipv4 : function(item, params) {
				var i, valid = true, validate;
				
				validate = function(ip) {
					if(ip !== '') {
						var ipParts = ip.split('.'), ipPart, i;
						if(ipParts.length != 4) {
							valid = false;
						} else {
							for(i = 0; i < ipParts.length; i++) {
								ipPart = ipParts[i];
								if(ipPart.match(/^[0-9]+$/g) == null || parseInt(ipPart) < 0 || parseInt(ipPart) > 255) {
									valid = false;
								}
							}
						}
					}
				}
				
				if(params && params.delimiter) {
					var ips = $(item).val().split(params.delimiter), i;
					for(i = 0; i < ips.length; i++) {
						validate(ips[i]);
					}
				} else {
					validate($(item).val());
				}
				
				that.applyCallback(item, 'ipv4', valid);
				return valid;
			},
			ipv6 : function(item, params) {
				var valid = true, validate;
				
				validate = function(ip) {
					if(ip !== '') {
						var i, ipParts = ip.split(':'), ipPart, emptyPart = false;
						if(ip.match(/^\:.+\:$/g) != null || ipParts.length < 3 || ipParts.length > 8) {
							this.messages.push('Invalid IPv6 address');
						} else {
							for(i = 0; i < ipParts.length; i++) {
								ipPart = ipParts[i];
								if(i > 0 && i < ipParts.length - 1 && ipPart == '' && emptyPart) {
									valid = false;
								} else if(i > 0 && i < ipParts.length - 1 && ipPart == '') {
									emptyPart = true;
								} else if(ipPart != '' && ipPart.match(/^[0-9a-fA-F]{1,4}$/g) == null) {
									valid = false;
								}
							}
						}
					}
				}
				
				if(params && params.delimiter) {
					var ips = $(item).val().split(params.delimiter), i;
					for(i = 0; i < ips.length; i++) {
						validate(ips[i]);
					}
				} else {
					validate($(item).val());
				}
				
				that.applyCallback(item, 'ipv6', valid);
				return valid;
			},
			ttl : function(item) {
				var valid = true;
				if($(item).val().match(/^([0-9]+[hdmswHDMSW]{0,1})+$/g) == null) {
					valid = false;
				}
				that.applyCallback(item, 'ttl', valid);
				return valid;
			},
			number : function(item, params) {
				var num = $(item).val(), valid;
				
				valid = num.match(/^[0-9]+$/g) != null || (params !== undefined && params.minimum !== undefined && num < params.minimum) || (params !== undefined && params.maximum !== undefined && num > params.maximum);
				
				that.applyCallback(item, 'number', valid);
				return valid;
			},
			unique : function(item, params) {
				return false;
			},
			uniqueInGroup : function(item, index) {
				var name = $(item).attr('name'),
					value = $(item).val(),
					valid = true;
				
				if(that.group_uniques[name]) {
					if(that.group_uniques[name][value] && value !== '') {
						valid = false;
						that.applyCallback(that.group_uniques[name][value], 'uniqueInGroup', valid);
						that.applyCallback(item, 'uniqueInGroup', valid);
					} else {
						that.group_uniques[name][value] = item;
						that.applyCallback(item, 'uniqueInGroup', valid);
					}
				} else {
					that.group_uniques[name] = {};
					that.group_uniques[name][value] = item;
					that.applyCallback(item, 'uniqueInGroup', valid);
				}
				return valid;
			},
			uniqueInText : function(item, delimiter) {
				var values, uniques = {}, i = 0, valid = true;
				delimiter = delimiter || '\n';
				values = $(item).val().split(delimiter);
				
				while(valid === true && i < values.length) {
					if(uniques[values[i]]) {
						valid = false;
					} else {
						uniques[values[i]] = values[i];
					}
					i++;
				}
				that.applyCallback(item, 'uniqueInText', valid);
				return valid;
			},
			required : function(item) {
				var valid = true;
				if($(item).val() === '') {
					valid = false;
				}
				that.applyCallback(item, 'required', valid);
				return valid;
			},
			notEqualTo : function(item, params) {
				var value = null, exclude = [], valid = true, i, isInExcluded;
				if(params && params.value) {
					value = params.value;
					exclude = params.exclude;
				}
				if(jQuery.isArray(value)) {
					for(i = 0; i < value.length; i++) {
						if($(item).val() == value[i] && jQuery.inArray(value[i], exclude) < 0) {
							valid = false;
							break;
						}
					}
				} else {
					valid = $(item).val() != value;
				}
				that.applyCallback(item, 'notEqualTo', valid);
				return valid;
			},
			notEqualToElem : function(item, param) {
				var value, valid;
				if(param) {
					elem = $(':fname(' + param + ')', that.form).val();
				}
				valid = $(item).val() != value;
				that.applyCallback(item, 'notEqualToElem', valid);
				return valid;
			},
			dnsBestMatch : function(item, params) {
				var name = $(item).val(), origin = '', domainNames = [], valid = true, index = 0, i;
				if(params) {
					if(params.origin) {
						origin = params.origin;
					}
					if(name != '@' && name != origin) {
						name += '.' + origin
					} else if(name == '@') {
						name = origin;
					}
					if(params.domains && jQuery.isArray(params.domains)) {
						domainNames = params.domains;
					}
					
					while(name !== origin && index >= 0 && valid === true) {
						for(i = 0; i < domainNames.length; i++) {
							if(name === domainNames[i]) {
								valid = false;
								break;
							}
						}
						index = name.indexOf('.') + 1;
						name = name.substr(index);
					}
				}
				that.applyCallback(item, 'dnsBestMatch', valid);
				return valid;
			},
			requiredGroup : function(item, params) {
				var name = $(item).attr('name'), valid = true;;
				if($(item).val() == '' && !that.group_uniques[name]) {
					that.group_uniques[name] = item;
				}
				if(params.index == params.length - 1) {
					valid = that.group_uniques[name] === undefined;
					that.applyCallback(that.group_uniques[name], 'requiredGroup', valid);
				}
				return valid;
			},
			text : function(item, params) {
				var minlength = 1, maxlength, text = $(item).val(), valid = true;
				if(params && params.minlength && params.minlength >= 0) {
					minlength = params.minlength;
				}
				if(params && params.maxlength && params.maxlength >= 0) {
					maxlength = params.maxlength;
				}
				if(text.length < minlength) {
					valid = false;
					that.applyCallback(item, 'text', valid);
				}
				if(maxlength && text.length > maxlength) {
					valid = false;
					that.applyCallback(item, 'text', valid);
				}
				return valid;
			}
		};
	}
	
	Validator.prototype.applyCallback = function(item, rule, valid) {
		var cb, name = $(item).attr('name'), message, type = 'valid';
		if(valid === false) {
			type === 'invalid'
			message = this.findMessage(name, rule);
		}
		if(this.callbacks && this.callbacks[name] && this.callbacks[name][rule]) {
			if(this.callbacks[name][rule][type] && jQuery.isFunction(this.callbacks[name][rule][type])) {
				cb = this.callbacks[name][rule][type];
			} else if(jQuery.isFunction(this.callbacks[name][rule])) {
				cb = this.callbacks[name][rule];
			}
		} else if(this.callbacks && this.callbacks[name]) {
			if(this.callbacks[name][type] && jQuery.isFunction(this.callbacks[name][type])) {
				cb = this.callbacks[name][type];
			} else if (jQuery.isFunction(this.callbacks[name])) {
				cb = this.callbacks[name];
			}
		} else if(this.callback) {
			if(this.callback[type] && jQuery.isFunction(this.callback[type])) {
				cb = this.callback[type];
			} else if(jQuery.isFunction(this.callback)) {
				cb = this.callback;
			}
		}
		if(cb) {
			cb(item, message);
		}
	}
	
	Validator.prototype.findMessage = function(name, rule) {
		var message;
		if(this.messages && this.messages[name] && this.messages[name][rule]) {
			message = this.messages[name][rule];
		} else if(this.messages && this.messages[name] && !jQuery.isPlainObject(this.messages[name])) {
			message = this.messages[name];
		}
		return message;
	}
	
	Validator.prototype.validate = function() {
		var item, rule, errors = {}, valid, total = true, elems, validate, that = this;
		
		validate = function(elem, pos) {
			if (that.rules[item][rule] === true) {
				valid = that.validateFunctions[rule](elem, pos);
			} else if (that.rules[item][rule] !== false) {
				valid = that.validateFunctions[rule](elem, that.rules[item][rule]);
			}
			if (valid === false) {
				if (errors[item]) {
					errors[item].push(that.messages[item][rule]);
				} else {
					errors[item] = [ that.messages[item][rule] ];
				}
			}

			total = total && valid;
		}

		for (item in this.rules) {
			for (rule in this.rules[item]) {
				this.group_uniques = {};
				valid = true;
				elems = $('[name="' + item + '"]',this.form);
				if(elems.length > 1) {
					elems.each(function(index) {
						validate(this, {index: index, length: elems.length});
					});
				} else {
					validate(elems, {index: 0, length: 1});
				}
				if(valid === false) {
					break;
				}
			}
		}
		
		return {
			valid : total,
			messages : errors
		};
	}
	
	$.fn.validateDNS = function(settings) {
		return new Validator(this, settings);
	};

}(jQuery));
