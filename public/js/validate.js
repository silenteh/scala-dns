function validate(json, domains, original) {
	var i, items, message, messages = [];
	message = validateOrigin(json.origin, json.origin, true);
	if(message != null) {
		messages.push(json.origin + ' origin value: ' + message);
	}
	message = checkDuplicateNames(json.origin, domains, original);
	if(message != null) {
		messages.push(message);
	}
	message = validateTimeString(json.ttl);
	if(message != null) {
		messages.push(json.origin + ' ttl value: ' + message);
	}
	if(!json.SOA) {
		messages.push('Domain must contain SOA record');
	}
	if(!json.NS || json.NS.length < 2) {
		messages.push('Domain must contain at least 2 NS records');
	}
	for(typ in json) {
		items = json[typ];
		if(typeof items == 'object') {
			for(i = 0; i < items.length; i++) {
				messages = messages.concat(validateRecord(items[i], json.origin, typ))
			}
		}
	}
	messages = messages.concat(checkDuplicateEntries(json));
	messages = messages.concat(checkMisplacedEntries(json, json.origin, domains));
	
	return messages;
}

function validateRecord(data, origin, typ) {
	var message, messages = [], i;
		prelude = data.name + '.' + origin + ' ' + typ + ' record\'s';
	switch(typ) {
	case 'SOA':
		message = validateClass(data['class']);
		if(message != null) {
			messages.push(prelude + ' class: ' + message);
		}
		message = validateDomainName(data.name);
		if(message != null) {
			messages.push(prelude + ' name: ' + message);
		}
		message = validateDomainName(data.mname, true);
		if(message != null) {
			messages.push(prelude + ' mname: ' + message);
		}
		message = validateDomainName(data.rname, true);
		if(message != null) {
			messages.push(prelude + ' rname: ' + message);
		}
		message = validateNumber(data.serial);
		if(message != null) {
			messages.push(prelude + ' serial: ' + message);
		}
		message = validateTimeString(data.at);
		if(message != null) {
			messages.push(prelude + ' ttl: ' + message);
		}
		message = validateTimeString(data.refresh);
		if(message != null) {
			messages.push(prelude + ' refresh: ' + message);
		}
		message = validateTimeString(data.expire);
		if(message != null) {
			messages.push(prelude + ' expire: ' + message);
		}
		message = validateTimeString(data.minimum);
		if(message != null) {
			messages.push(prelude + ' minimum: ' + message);
		}
		return messages;
		break;
	case 'NS':
		message = validateClass(data['class']);
		if(message != null) {
			messages.push(prelude + ' class: ' + message);
		}
		message = validateDomainName(data.name);
		if(message != null) {
			messages.push(prelude + ' name: ' + message);
		}
		break;
	case 'MX':
		message = validateClass(data['class']);
		if(message != null) {
			messages.push(prelude + ' class: ' + message);
		}
		message = validateDomainName(data.name);
		if(message != null) {
			messages.push(prelude + ' name: ' + message);
		}
		message = validateNumber(data.priority);
		if(message != null) {
			messages.push(prelude + ' priority: ' + message);
		}
		message = validateDomainName(data.value, true);
		if(message != null) {
			messages.push(prelude + ' MX server: ' + message);
		}
		break;
	case 'A':
		message = validateClass(data['class']);
		if(message != null) {
			messages.push(prelude + ' class: ' + message);
		}
		message = validateDomainName(data.name);
		if(message != null) {
			messages.push(prelude + ' name: ' + message);
		}
		if(data.value == '') {
			messages.push(prelude + ': No address specified');
		} else {
			for(i = 0; i < data.value.length; i++) {
				messages = messages.concat(validateSubRecord(data.value[i], data.name, origin, typ));
			}
		}
		break;
	case 'AAAA':
		message = validateClass(data['class']);
		if(message != null) {
			messages.push(prelude + ' class: ' + message);
		}
		message = validateDomainName(data.name);
		if(message != null) {
			messages.push(prelude + ' name: ' + message);
		}
		if(data.value == '') {
			messages.push(prelude + ': No address specified');
		} else {
			for(i = 0; i < data.value.length; i++) {
				messages = messages.concat(validateSubRecord(data.value[i], data.name, origin, typ));
			}
		}
		break;
	case 'CNAME':
		message = validateClass(data['class']);
		if(message != null) {
			messages.push(prelude + ' class: ' + message);
		}
		message = validateDomainName(data.name);
		if(message != null) {
			messages.push(prelude + ' name: ' + message);
		}
		message = validateDomainName(data.value, true);
		if(message != null) {
			messages.push(prelude + ' host name: ' + message);
		}
		break;
	case 'PTR':
		message = validateClass(data['class']);
		if(message != null) {
			messages.push(prelude + ' class: ' + message);
		}
		message = validateDomainName(data.name);
		if(message != null) {
			messages.push(prelude + ' name: ' + message);
		}
		message = validateDomainName(data.value, true);
		if(message != null) {
			messages.push(prelude + ' pointer: ' + message);
		}
		break;
	case 'TXT':
		message = validateClass(data['class']);
		if(message != null) {
			messages.push(prelude + ' class: ' + message);
		}
		message = validateDomainName(data.name);
		if(message != null) {
			messages.push(prelude + ' name: ' + message);
		}
		if(data.value == '') {
			messages.push(prelude + ': No text specified');
		} else {
			for(i = 0; i < data.value.length; i++) {
				messages = messages.concat(validateSubRecord(data.value[i], data.name, origin, typ));
			}
		}
		break;
	default:
		break;
	}
	return messages;
}

function validateSubRecord(data, name, origin, typ) {
	var message, messages = [], 
		prelude = name + '.' + origin + ' ' + typ + ' record\'s';
	switch(typ) {
	case 'A':
		message = validateWeight(data.weight);
		if(message != null) {
			messages.push(prelude + ' weight: ' + message);
		}
		message = validateIPAddress(data.ip);
		if(message != null) {
			messages.push(prelude + ' address: ' + message);
		}
		break;
	case 'AAAA':
		message = validateWeight(data.weight);
		if(message != null) {
			messages.push(prelude + ' weight: ' + message);
		}
		message = validateIPv6Address(data.ip);
		if(message != null) {
			messages.push(prelude + ' address: ' + message);
		}
		break;
	case 'TXT':
		message = validateString(data);
		if(message != null) {
			messages.push(prelude + ' text: ' + message);
		}
		break;
	default:
		break;
	}
	return messages;
}

function validateOrigin(name) {
	if(name == '') {
		return 'Name is empty';
	} else if(name.match(/^([a-zA-Z0-9\*]+\.)+$/g) == null) {
		return 'Invalid name';
	}
	return null;
}

function validateDomainName(name, domain, allowAbsolute) {
	var allowAbsolute = allowAbsolute || false, nameMatch;
	
	if(name == '') {
		return 'Name is empty';
	}
	nameMatch = name.match(/^[a-zA-Z0-9\*]+(\.[a-zA-Z0-9\*]+)*$/g)
	if(nameMatch != null && nameMatch[0] != name && 
			(!allowAbsolute && name != domain && name.indexOf(domain) > -1) && 
			(!allowAbsolute || name.match(/^([a-zA-Z0-9\*]+\.)+$/g) == null)) {
		return 'Invalid name';
	}
	
	return null;
}

function checkDuplicateNames(origin, domains, original) {
	var count = 0;
	for(i = 0; i < domains.length; i++) {
		if(domains[i] + '.' == origin && (original == null || origin != original || count >= 1)) {
			return 'Duplicate domain name "' + origin + '"'
		} else if(domains[i] + '.' == origin) {
			count++;
		}
	}
	return null;
}

function checkDuplicateEntries(json) {
	var items, item, other, i, j, messages = [], checkedEntries;
	for(key in json) {
		checkedEntries = [];
		items = json[key];
		if(typeof items == 'object' && key != 'CNAME' && key != 'NS') {
			for(i = 0; i < items.length; i++) {
				item = items[i];
				for(j = 0; j < checkedEntries.length; j++) {
					other = checkedEntries[j];
					if(item.name == other.name && item['class'] == other['class']) {
						messages.push('Duplicate ' + key + ' record "' + item.name + '"');
					} 
				}
				checkedEntries.push(item);
			}
		}
	}
	return messages;
}

function checkMisplacedEntries(json, origin, domains) {
	var messages = [], items, i, item, j, domain, index;
	for(key in json) {
		items = json[key];
		if(typeof items == 'object') {
			for(i = 0; i < items.length; i++) {
				item = items[i];
				for(j = 0; j < domains.length; j++) {
					domain = domains[j];
					index = (item.name + '.' + origin).indexOf(domain)
					if(index > -1 && index < item.name.length) {
						messages.push('Misplaced domain "' + item.name + '". Move it to "' + domain + '"');
					}
				}
			}
		}
	}
	return messages;
}

function validateIPAddress(ip) {
	var i, ipPart, ipParts = ip.split(".");
	
	if(ipParts.length != 4) {
		return 'Invalid IP address';
	} else {
		for(i = 0; i < ipParts.length; i++) {
			ipPart = ipParts[i];
			if(ipPart.match(/^[0-9]+$/g) == null || parseInt(ipPart) < 0 || parseInt(ipPart) > 255) {
				return 'Invalid IP address';
			}
		}
	}
	return null;
}

function validateIPv6Address(ip) {
	var i, ipPart, emptyPart = false, 
		ipParts = ip.split(":");
	
	if(ip.match(/^\:.+\:$/g) != null || ipParts.length < 3 || ipParts.length > 8) {
		messages.push('Invalid IPv6 address');
	} else {
		for(i = 0; i < ipParts.length; i++) {
			ipPart = ipParts[i];
			if(i > 0 && i < ipParts.length - 1 && ipPart == '' && emptyPart) {
				return 'Invalid IPv6 address';
			} else if(i > 0 && i < ipParts.length - 1 && ipPart == '') {
				emptyPart = true;
			} else if(ipPart != '' && ipPart.match(/^[0-9a-fA-F]{1,4}$/g) == null) {
				return 'Invalid IPv6 address';
			}
		}
	}
	return null;
}

function validateTimeString(str) {
	if(str.match(/^([0-9]+[hdmswHDMSW]{0,1})+$/g) == null) {
		return 'Invalid time string';
	}
	return null;
}

function validateNumber(num) {
	if(num.match(/^[0-9]+$/g) == null) {
		return 'Invalid number';
	}
	return null;
}

function validateWeight(num) {
	message = validateNumber(num);
	if(message !== null) {
		return message;
	} else if(num < 1 || num > 10) {
		return 'Invalid weight - must be a number from 1 to 10';
	}
	return null;
}

function validateClass(str) {
	if(str != 'in') {
		return 'Invalid class';
	}
	return null;
}

function validateString(str) {
	if(str == '') {
		return 'Invalid class';
	}
	return null;
}