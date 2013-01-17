var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.UserForm = function(container) {
		ScalaDNS.UserForm.parent.constructor.call(this, 'UserForm', container);
		
		this.user = null;
		this._validator;
	}
	
	ScalaDNS.extend(ScalaDNS.UserForm, ScalaDNS.BaseWidget);
	
	ScalaDNS.UserForm.prototype.load = function(settings, callback) {
		ScalaDNS.UserService.loadUsers(callback);
	}
	
	ScalaDNS.UserForm.prototype.init = function() {
		var that = this;
		this._tpl = $('#userFormTemplate').clone().removeAttr('id').removeClass('hidden');
		this._create_password_tpl = $(':dname(create-password)', this._tpl).detach();
		this._change_password_tpl = $(':dname(change-password)', this._tpl).detach();
		
		this._validator = this.initValidator();
		
		$('#add-user', this._tpl).bind('click', function(evt) {
			var name, orig_name, validName, validPassword, 
				passChange = $(':fname(change_password)', that._tpl);
			evt.preventDefault();
			
			validName = that._validator.username.validate().valid;
			if(passChange.legnth != 0 && passChange.prop('checked') === false) {
				validPassword = true;
			} else {
				validPassword = that._validator.password.validate().valid
			}
			
			if(validName === true && validPassword === true) {
				name = $(':fname(user_name)', that._tpl).val();
				password = $(':fname(password)', that._tpl).val();
				if(that.user !== null) {
					orig_name = that.user.name;
					that.user.name = name;
					if(passChange.legnth == 0 || passChange.prop('checked') === true) {
						that.user.digest = password;
					}
					ScalaDNS.users.replace(orig_name, name, that.user);
					ScalaDNS.UserService.replaceUser(that.user, orig_name, function() {
						that._raiseUserUpdate();
						that.clearForm();
					});
				} else {
					that.user = {name: name, digest: password};
					ScalaDNS.UserService.saveUser(that.user, function() {
						ScalaDNS.users.set(name, that.user);
						that._raiseUserUpdate();
						that.clearForm();
					});
				}
			}
		});
		
		$(this._tpl).delegate(':fname(change_password)', 'click', function() {
			var parent = $(this).closest(':dname(change-password)');
			if($(this).prop('checked') === true) {
				$('.main', parent).removeClass('hidden');
			} else {
				$('.main', parent).addClass('hidden');
			}
		});
		
		$(this._tpl).bind('click', function(evt) {
			evt.stopPropagation();
		});
		
		this._tpl.delegate('.control-group input[type="text"], .control-group input[type="password"]', 'keyup', function() {
			that._onValidate(this);
		});
		
		ScalaDNS.onUserSelect.bind(this, this.userSelected);
	}
	
	ScalaDNS.UserForm.prototype.draw = function() {
		$(':dtype(password-box)', this._tpl).append(this._create_password_tpl);
		$('.row-fluid', this.container).append(this._tpl);
	}
	
	ScalaDNS.UserForm.prototype.dispose = function() {
		$('#add-user', this._tpl).unbind();
		$(this._tpl).unbind();
		ScalaDNS.onUserSelect.unbind(this, this.userSelected);
	}
	
	ScalaDNS.UserForm.prototype.userSelected = function(e) {
		this.user = e.obj;
		this.clearForm();
		var validator = this._validator.username;
		this._validator.username = this._initUsernameValidation(validator.form, validator.callback);
		if(this.user !== null) {
			$('[data-type="form-title"]', this._tpl).text('Edit User');
			$('[data-id="add-user"]', this._tpl).text('Update User');
			$('[name="user_name"]', this._tpl).val(this.user.name);
			$(':dtype(password-box)', this._tpl).empty();
			$(':dtype(password-box)', this._tpl).append(this._change_password_tpl);
		} else {
			$(':dtype(password-box)', this._tpl).empty();
			$(':dtype(password-box)', this._tpl).append(this._create_password_tpl);
		}
	}
	
	ScalaDNS.UserForm.prototype.clearForm = function() {
		var form = $('form', this.container);
		$('[data-type="form-title"]', this._tpl).text('Add New User');
		$('[data-id="add-user"]', this._tpl).text('Add New User');
		$('input[type="text"]', form).val('');
		$('input[type="password"]', form).val('');
		$('.control-group', form).removeClass('error');
		$('.help-inline', form).remove();
		$(':dtype(password-box)', this._tpl).empty();
		$(':dtype(password-box)', this._tpl).append(this._create_password_tpl);
	}
	
	ScalaDNS.UserForm.prototype.initValidator = function() {
		var form = $('form', this._tpl), that = this;
		return {
			username: that._initUsernameValidation(form, that._onValidate),
			password: that._initPasswordValidation(form, that._onValidate)
		}
	}
	
	ScalaDNS.UserForm.prototype._initUsernameValidation = function(form, callback) {
		var exclude = [];
		if(this.user !== null) {
			exclude.push(this.user.name);
		};
		return form.validateDNS({
			rules: {
				user_name: {
					text: {
						minlength: 1,
						maxlength: 255
					},
					notEqualTo: {
						value: ScalaDNS.users.getKeys(),
						exclude: exclude
					}
				}
			},
			messages: {
				user_name: {
					text: 'User name should be 1 to 255 characters long',
					notEqualTo: 'User name already exists'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.UserForm.prototype._initPasswordValidation = function(form, callback) {
		return form.validateDNS({
			rules: {
				password: {
					text: {
						minlength: 8,
						maxlength: 50
					}
				}
			},
			messages: {
				password: {
					text: 'Password should be 8 to 50 characters long'
				}
			},
			callback: callback
		});
	}
	
	ScalaDNS.UserForm.prototype._onValidate = function(item, message) {
		var parent = $(item).closest('.control-group');
		if(message !== undefined) {
			parent.addClass('error');
			parent.append('<span class="help-inline"><small><strong>' + message + '</strong></small></span>');
		} else if(parent.hasClass('error')) {
			$('.help-inline', parent).remove();
			parent.removeClass('error');
		}
	}
	
	ScalaDNS.UserForm.prototype._raiseUserUpdate = function() {
		ScalaDNS.onUsersUpdate.raise(new ScalaDNS.UpdatedEvent(this));
	}
	
}());