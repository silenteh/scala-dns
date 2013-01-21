var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.Users = function(container) {
		ScalaDNS.Users.parent.constructor.call(this, 'Users', container);
		
		this.datatable;
	}
	
	ScalaDNS.extend(ScalaDNS.Users, ScalaDNS.BaseWidget);
	
	ScalaDNS.Users.prototype.load = function(settings, callback) {
		ScalaDNS.UserService.loadUsers(callback);
	}
	
	ScalaDNS.Users.prototype.init = function() {
		var that = this;
		this._tpl = $('#userTemplate').clone().removeAttr('id').removeClass('hidden');
		this._row_tpl = $('tbody tr:first', this._tpl).clone();
		
		this._raiseSelectUser();
		
		$(this._tpl).delegate('tbody tr', 'click', function(evt) {
			if($('td.dataTables_empty', this).length == 0) {
				var selected = $(this).hasClass('row_selected');
				evt.stopPropagation();
				$('tbody tr', this._tpl).removeClass('row_selected');
				if(selected === false) {
					$(this).addClass('row_selected');
					$('[data-id="delete-us"]', this._tpl).prop('disabled', false);
					that._raiseSelectUser(that.datatable.fnGetData(this)[0]);
				} else {
					$('[data-id="delete-us"]', this._tpl).prop('disabled', true);
					that._raiseSelectUser();
				}
			}
		});
		
		$('[data-id="delete-us"]', this._tpl).bind('click', function(evt) {
			evt.stopPropagation();
			ScalaDNS.ConfirmBox.show(function() {
				var rowsToRemove = $('tbody tr.row_selected'), row, name;
				rowsToRemove.each(function() {
					row = this;
					name = that.datatable.fnGetData(this)[0];
					ScalaDNS.UserService.removeUser(name, function() {
						//row.remove();
						that.draw();
					});
				});
			});
		});
		
		$('html').bind('click', function() {
			$('tbody tr', this._tpl).removeClass('row_selected');
			$('[data-id="delete-us"]', this._tpl).prop('disabled', true);
			that._raiseSelectUser();
		});
		
		ScalaDNS.onUsersUpdate.bind(this, this._userUpdated);
	}
	
	ScalaDNS.Users.prototype.draw = function() {
		var i, user, row;
		if(this.datatable === undefined) {
			this.datatable = $('table', this._tpl).dataTable();
		}
		$('[data-id="delete-us"]', this._tpl).prop('disabled', true);
		//$('tbody', this._tpl).empty();
		this.datatable.fnClearTable();
		ScalaDNS.ConfirmBox.setMessage('Delete users', '<p>You are about to delete the selected user(s). This action is irreversible.</p><p>Do you want to proceed?</p>', 'btn-danger');
		if(ScalaDNS.users !== null) {
			ScalaDNS.users.reset();
			while(user = ScalaDNS.users.next()) {
				row = [];
				row.push(user.name);
				this.datatable.fnAddData(row);
				/*row = this._row_tpl.clone();
				row.attr('data-id', i);
				$('[data-type="user-name"]', row).text(user.name);
				if(i % 2 == 0) {
					row.addClass('even');
				} else {
					row.addClass('odd');
				}
				$('tbody', this._tpl).append(row);*/
			}
		} else {
			
		}
		this.container.append(this._tpl);
	}
	
	ScalaDNS.Users.prototype.dispose = function() {
		
	}
	
	ScalaDNS.Users.prototype.dispose = function() {
		$('html').unbind();
		ScalaDNS.onUsersUpdate.unbind(this, this._userUpdated);
	}
	
	ScalaDNS.Users.prototype._raiseSelectUser = function(name) {
		var user = null;
		if(name !== undefined) {
			user = ScalaDNS.users.get(name);
		}
		ScalaDNS.onUserSelect.raise(new ScalaDNS.SelectedEvent(this, user));
	}
	
	ScalaDNS.Users.prototype._userUpdated = function(e) {
		this.draw();
	}
	
}());