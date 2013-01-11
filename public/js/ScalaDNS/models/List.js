var ScalaDNS = ScalaDNS || {};

ScalaDNS.List = function() {
	this._data = {};
	this._array = [];
	this._index = 0;
}

ScalaDNS.List.prototype = {
	get: function(id) {
		return this._data[id] || null;
	},
	getByIndex: function(index) {
		return this._data[this._array[index]];
	},
	getIndex: function(id) {
		var i, len = this._array.length;
		for (i=0; i<len; i++) {
			if (this._array[i] == id) return i;
		}
		return null;
	},
	getIndexByObj: function(obj) {
		var i, len = this._array.length;
		for (i=0; i<len; i++) {
			if (this._data[this._array[i]] === obj) return i;
		}
		return null;
	},
	getKeys: function() {
		return this._array;
	},
	set: function(id, item, prepend) {
		var isNew = false;
		
		prepend = prepend || false;
		if (!id || item === undefined || item === null) {
			throw('Invalid params on List::set');
		}
		if (!this._data.hasOwnProperty(id)) {
			if (prepend === false) {
				this._array.push(id);
			} else {
				this._array.unshift(id);
			}
			isNew = true;
		}
		this._data[id] = item;
		return isNew;
	},
	replace: function(old_id, new_id, item) {
		var isNew = false;
		if (!old_id || !new_id || !item || !this._data.hasOwnProperty(old_id)) {
			throw('Invalid params on List::replace');
		}
		this._array[this.getIndex(old_id)] = new_id;
		delete this._data[old_id];
		this._data[new_id] = item;
	},
	load: function(id, buildFn, prepend) {
		var item = this.get(id);
		if (item === null) {
			item = buildFn();
			if (item !== null) {
				prepend = prepend || false;
				this.set(id, item, prepend);
			}
		}
		return item;
	},
	remove: function(id) {
		var i, len = this._array.length;
		
		delete this._data[id];
		
		for (i = 0; i<len; i++) {
			if (this._array[i]==id) break;
		}
		
		this._array.splice(i, 1);
		
		if(i > 0 && this._index >= i) this._index--;
	},
	getLength: function() {
		return this._array.length;
	},
	reset: function() {
		this._index = 0;
	},
	next: function() {
		var id, item;
		
		if (this._array.length > this._index) {
			id = this._array[this._index];
			item = this._data[id];
			this._index++;
			return item;
		} else {
			return null;
		}
	},
	sort: function(callback) {
		var that = this, item, i = 0;
		function innerSortCallback(a, b){
			return callback(
				{
					id: a,
					item: that._data[a]
				},
				{
					id: b,
					item: that._data[b]
				}
			)
		}
		this._array.sort(innerSortCallback);
	},
	empty: function() {
		this._data = {};
		this._array = [];
		this._index = 0;
	},
	toArray: function() {
		var array = [], id;
		for(id in this._data) {
			array.push(this._data[id]);
		}
		return array;
	}
}