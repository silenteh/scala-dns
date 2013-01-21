var ScalaDNS = ScalaDNS || {};

ScalaDNS.UserService = (function() {
	var i;
	
	function loadUsers(callback) {
		if(ScalaDNS.users === null) {
			$.ajax('http://' + ScalaDNS.config.urlBase + '/users/', {
				cache: false, 
				success: function(data) {
					if(data && data.users) {
						ScalaDNS.users = new ScalaDNS.List();
						jQuery.each(data.users, function() {
							ScalaDNS.users.set(this.name, this);
						});
					}
				},
				complete: function() {
					if(callback) {
						callback();
					}
				}
			}, 'json');
		} else {
			callback();
		}
	}
	
	function saveUser(user, callback) {
		$.post('http://' + ScalaDNS.config.urlBase + '/users/', {name: user.name, digest: user.digest}, function(result) {
			if(result.code == 0) {
				ScalaDNS.users.set(result.data.name, result.data);
			}
			if(callback) {
				callback(result);
			}
		}, 'json');
	}
	
	function replaceUser(user, replaceFilename, callback) {
		$.post('http://' + ScalaDNS.config.urlBase + '/users/', {
			name: user.name, 
			digest: user.digest,
			replace_filename: replaceFilename
		}, function(result) {
			if(result.code == 0) {
				ScalaDNS.users.set(result.data.name, result.data);
			}
			if(callback) {
				callback(result);
			}
		}, 'json');
	}
	
	function removeUser(userName, callback) {
		$.post('http://' + ScalaDNS.config.urlBase + '/users/', {'delete': userName}, function(result) {
			ScalaDNS.users.remove(userName);
			if(callback) {
				callback();
			}
		});
	}
	
	return {
		loadUsers: loadUsers,
		saveUser: saveUser,
		replaceUser: replaceUser,
		removeUser: removeUser
	};
	
}());