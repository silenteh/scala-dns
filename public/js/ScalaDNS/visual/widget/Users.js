var ScalaDNS = ScalaDNS || {};

(function() {
	
	ScalaDNS.Users = function(container) {
		ScalaDNS.Users.parent.constructor.call(this, 'Users', container);
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
			var selected = $(this).hasClass('row_selected');
			evt.stopPropagation();
			$('tbody tr', this._tpl).removeClass('row_selected');
			if(selected === false) {
				$(this).addClass('row_selected');
				that._raiseSelectUser($('[data-type="user-name"]', this).text());
			} else {
				that._raiseSelectUser();
			}
		});
		
		$('[data-id="delete-us"]', this._tpl).bind('click', function(evt) {
			var rowsToRemove = $('tbody tr.row_selected'), row, name;
			evt.stopPropagation();
			rowsToRemove.each(function() {
				row = this;
				name = $(':dtype(user-name)', row).text();
				ScalaDNS.UserService.removeUser(name, function() {
					row.remove();
				});
			});
		});
		
		$('html').bind('click', function() {
			$('tbody tr', this._tpl).removeClass('row_selected');
			that._raiseSelectUser();
		});
		
		ScalaDNS.onUsersUpdate.bind(this, this._userUpdated);
	}
	
	ScalaDNS.Users.prototype.draw = function() {
		var i, user, row;
		$('tbody', this._tpl).empty();
		if(ScalaDNS.users !== null) {
			ScalaDNS.users.reset();
			while(user = ScalaDNS.users.next()) {
				row = this._row_tpl.clone();
				row.attr('data-id', i);
				$('[data-type="user-name"]', row).text(user.name);
				if(i % 2 == 0) {
					row.addClass('even');
				} else {
					row.addClass('odd');
				}
				$('tbody', this._tpl).append(row);
			}
		} else {
			
		}
		this.container.append(this._tpl);
	}
	
	ScalaDNS.Users.prototype.dispose = function() {
		
	}
	
	ScalaDNS.Users.prototype.dispose = function() {
		$('html').unbind();
		ScalaDNS.onDomainUpdate.unbind(this, this._domainUpdated);
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