var urlBase = location.host,
	types = ['SOA', 'NS', 'MX', 'A', 'AAAA', 'CNAME', 'PTR', 'TXT'],
	domains, original = null;

function loadMenu(callback) {
	$.get('http://' + urlBase + '/domains/?menu', {}, function(data) {
		domains = data;
		callback(data);
	}, 'json');
}
function loadDomain(domain, callback) {
	$.get('http://' + urlBase + '/domains/' + domain, {}, function(data) {
		callback(data);
	}, 'json');
}
function drawMenu(data) {
	var menu = $('[data-type="nav"]'), permanentLinks = $('[data-type="perm"]',
			menu).clone(), length = data.length, i;
	menu.empty();
	permanentLinks.each(function(index) {
		menu.append(permanentLinks[index]);
		menu.append(' | ');
	});

	for (i = 0; i < length; i++) {
		if (i < length - 1) {
			menu.append('<a href="#">' + data[i] + '</a> | ');
		} else {
			menu.append('<a href="#">' + data[i] + '</a>');
		}
	}

	$('[data-type="nav"] a').unbind();

	$('[data-type="nav"] a').click(function(evt) {
		evt.preventDefault();
		if ($(this).attr('data-type')) {
			drawDomainForm();
		} else {
			loadDomain($(this).text(), drawDomainForm);
		}
	});
}

function drawDomainForm(data) {
	var form_tpl = $('[data-type="domain-form-tpl"]'), 
		form = form_tpl.clone().removeClass('hidden').removeAttr('data-type'), 
		container = $('[data-type="content"]');

	container.empty();
	original = null;
	if (data) {
		var domain = data.domain[1], i, head, items, j, typ, count,
			form_data = $('form', form), 
			submit = $('[data-type="domain-form-submit"]:first', form_tpl).clone();
		
		original = domain.origin;
		form_data.empty();
		form_data.append(submit);
		dataToForm(domain, form_data, form_tpl);
		
		for(i = 0; i < types.length; i++) {
			typ = types[i];
			count = 0;
			if(domain[typ]) {
				count = domain[typ].length;
			}
			head = $('[data-type="domain-form-' + typ.toLowerCase() + '-head-tpl"]', form_tpl).clone();
			$('[data-type="count"]', head).text(count);
			form_data.append(head);
			
			if(count > 0) {
				items = domain[typ];
				for(j = 0; j < items.length; j++) {
					dataToForm(items[j], form_data, form_tpl, typ);
				}
			}
		}
		
		form_data.append(submit.clone());
	}

	$('[data-type="domain-form-remove"]', form).click(function(evt) {
		evt.preventDefault();
		onRemoveClick(this);
	});

	$('[data-type="domain-form-add"]', form).click(function(evt) {
		evt.preventDefault();
		onAddClick($(this), form_tpl);
	});
	
	$('[data-type="submit"]', form).click(function(evt) {
		evt.preventDefault();
		onFormSubmit(form);
	});

	$('h3', form).click(function(evt) {
		evt.preventDefault();
		$('table', form).hide();
		$('table table', form).show();
		$(this).nextUntil('h3').show();
	});
	
	container.append(form);
}

function onAddClick(target, template) {
	var count, head, children,
		parent = target.parent(), dataType = parent.closest('[data-type]')
			.attr('data-type'), headIndex = dataType.indexOf('head'), refpoint, item;

	console.log(parent);
	
	if (headIndex < 0) {
		dataType = dataType.substring(0, dataType.indexOf('tpl')) + 'subtpl';
		refpoint = $('td:first', parent.closest('tr').next());
	} else {
		dataType = dataType.substring(0, headIndex) + 'tpl';
		refpoint = parent;
	}

	item = $('[data-type="' + dataType + '"]', template).clone();

	if (headIndex < 0) {
		refpoint.prepend(item);
	} else {
		refpoint.after(item);
	}
	
	head = $(target).closest('h3');
	count = $('[data-type="count"]', head).text();
	
	children = $('td', parent.parent().next()).children();
	if(children.length == 0) {
		$('[data-type="domain-form-remove"]', $('table', item)).hide();
	} else if(children.length == 1) {
		$('[data-type="domain-form-remove"]', children).hide();
	} else {
		$('[data-type="domain-form-remove"]', children).show();
	}
	
	
	$('[data-type="count"]', head).text(parseInt(count) + 1);
	
	$('[data-type="domain-form-add"]', item).click(function(evt) {
		evt.preventDefault();
		onAddClick($(this), template);
	});

	$('[data-type="domain-form-remove"]', item).click(function(evt) {
		evt.preventDefault();
		onRemoveClick(this);
	});
}

function onRemoveClick(target) {
	var count, head, parent, children;
	
	head = $(target).closest('table').prev('h3');
	parent = $(target).closest('table').parent();
	$(target).closest('table').remove();
	count = $('[data-type="count"]', head).text();
	$('[data-type="count"]', head).text(count - 1);
	
	if(parent.closest('table').length > 0) {
		children = parent.children();
		if(children.length == 1) {
			$('[data-type="domain-form-remove"]', children).hide();
		}
	}
	console.log($(parent));
}

function onFormSubmit(form) {
	var json = formDataToJson(form), 
		content = $('[data-type="content"]'),
		message_tpl, i,
		messages = validate(json, domains, original);
	
	$('[data-type="messages"]').remove();
	
	if(messages.length == 0) {
		$.post('http://' + urlBase + '/domains/', {data: JSON.stringify(json)}, function(data) {
			if(data.code == 0) {
				drawDomainForm({domain: [0, data.data]});
			} else {
				drawDomainForm({domain: [0, json]});
			}
		}, 'json');
	} else {
		message_tpl = $('[data-type="message-tpl"]').clone().attr('data-type', 'messages');
		for(i = 0; i < messages.length; i++) {
			message_tpl.append('<p>' + messages[i] + '</p>');
		}
		content.prepend(message_tpl);
	}
}

function formDataToJson(form) {
	var data, item, i, typ, itemjson,
		json = formToData($('[data-type="domain-form-main"]', form));
	
	for(i = 0; i < types.length; i++) {
		typ = types[i];
		item =  $('[data-type="domain-form-' + typ.toLowerCase() + '-tpl"]', form);
		if(item.length > 0) {
			data = [];
			item.each(function() {
				itemjson = formToData(this, typ);
				if(itemjson != null) {
					data.push(itemjson);
				}
			});
			if(data.length > 0) {
				json[typ] = data;
			}
		}
	}
	
	return json;
}

function dataToForm(item, form, template, typ) {
	var body, subdata, subform;
	switch(typ) {
	case 'SOA':
		body = $('[data-type="domain-form-soa-tpl"]', template).clone();
		$('[name="soaclass[]"]', body).val(item['class']);
		$('[name="soaname[]"]', body).val(item.name);
		$('[name="soattl[]"]', body).val(item.at);
		$('[name="soamname[]"]', body).val(item.mname);
		$('[name="soarname[]"]', body).val(item.rname);
		$('[name="soaserial[]"]', body).val(item.serial);
		$('[name="soarefresh[]"]', body).val(item.refresh);
		$('[name="soaretry[]"]', body).val(item.retry);
		$('[name="soaexpire[]"]', body).val(item.expire);
		$('[name="soaminimum[]"]', body).val(item.minimum);
		break;
	case 'NS':
		body = $('[data-type="domain-form-ns-tpl"]', template).clone();
		$('[name="nsclass[]"]', body).val(item['class']);
		$('[name="nsname[]"]', body).val(item.name);
		$('[name="nsvalue[]"]', body).val(item.value);
		break;
	case 'MX':
		body = $('[data-type="domain-form-mx-tpl"]', template).clone();
		$('[name="mxclass[]"]', body).val(item['class']);
		$('[name="mxname[]"]', body).val(item.name);
		$('[name="mxpriority[]"]', body).val(item.priority);
		$('[name="mxvalue[]"]', body).val(item.value);
		break;
	case 'A':
		body = $('[data-type="domain-form-a-tpl"]', template).clone();
		$('[name="aclass[]"]', body).val(item['class']);
		$('[name="aname[]"]', body).val(item.name);
		subdata = item.value;
		break;
	case 'AAAA':
		body = $('[data-type="domain-form-aaaa-tpl"]', template).clone();
		$('[name="aaaaclass[]"]', body).val(item['class']);
		$('[name="aaaaname[]"]', body).val(item.name);
		subdata = item.value;
		break;
	case 'CNAME':
		body = $('[data-type="domain-form-cname-tpl"]', template).clone();
		$('[name="cnameclass[]"]', body).val(item['class']);
		$('[name="cnamename[]"]', body).val(item.name);
		$('[name="cnamevalue[]"]', body).val(item.value);
		break;
	case 'PTR':
		body = $('[data-type="domain-form-ptr-tpl"]', template).clone();
		$('[name="ptrclass[]"]', body).val(item['class']);
		$('[name="ptrname[]"]', body).val(item.name);
		$('[name="ptrvalue[]"]', body).val(item.value);
		break;
	case 'TXT':
		body = $('[data-type="domain-form-txt-tpl"]', template).clone();
		$('[name="txtclass[]"]', body).val(item['class']);
		$('[name="txtname[]"]', body).val(item.name);
		subdata = item.value;
		break;
	default:
		body = $('[data-type="domain-form-main"]',template).clone()
		$('[name="origin"]', body).val(item.origin);
		$('[name="ttl"]', body).val(item.ttl);
		break;
	}
	
	form.append(body);
	
	if(subdata) {
		subform = $('[data-type="domain-form-' + typ.toLowerCase() + '-subtpl"]', form).closest('td');
		subform.empty();
		for (i = 0; i < subdata.length; i++) {
			subdataToForm(subdata[i], subform, template, typ)
		}
		if(subdata.length <= 1) {
			$('[data-type="domain-form-remove"]', subform).hide();
		}
	}
	
	body.hide();
}

function subdataToForm(item, container, template, typ) {
	var body;
	switch(typ) {
	case 'A':
		body = $('[data-type="domain-form-a-subtpl"]', template).clone().removeAttr('data-type');
		$('[name="aweight[][]"]', body).val(item.weight);
		$('[name="avalue[][]"]', body).val(item.ip);
		container.append(body);
		break;
	case 'AAAA':
		body = $('[data-type="domain-form-aaaa-subtpl"]',template).clone().removeAttr('data-type');
		$('[name="aaaaweight[][]"]', body).val(item.weight);
		$('[name="aaaavalue[][]"]', body).val(item.ip);
		container.append(body);
		break;
	case 'TXT':
		body = $('[data-type="domain-form-txt-subtpl"]', template).clone().removeAttr('data-type');
		$('[name="txtvalue[][]"]', body).val(item);
		container.append(body);
		break;
	default:
		break;
	}
}

function formToData(item, typ) {
	var json, subitem, subdata, subitemjson, empty = true;
	switch(typ) {
	case 'SOA':
		json = {
			name: $('[name="soaname[]"]', item).val(),
			at: $('[name="soattl[]"]', item).val(),
			'class': $('[name="soaclass[]"]', item).val(),
			mname: $('[name="soamname[]"]', item).val(),
			rname: $('[name="soarname[]"]', item).val(),
			serial: $('[name="soaserial[]"]', item).val(),
			refresh: $('[name="soarefresh[]"]', item).val(),
			expire: $('[name="soaexpire[]"]', item).val(),
			minimum: $('[name="soaminimum[]"]', item).val()
		};
		break;
	case 'NS':
		json = {
			name: $('[name="nsname[]"]', item).val(),
			'class': $('[name="nsclass[]"]', item).val(),
			value: $('[name="nsvalue[]"]', item).val()
		};
		break;
	case 'MX':
		json = {
			name: $('[name="mxname[]"]', item).val(),
			'class': $('[name="mxclass[]"]', item).val(),
			priority: $('[name="mxpriority[]"]', item).val(),
			value: $('[name="mxvalue[]"]', item).val()
		};
		break;
	case 'A':
		json = {
			name: $('[name="aname[]"]', item).val(),
			'class': $('[name="aclass[]"]', item).val()
		};
		subitem = $('table', item);
		break;
	case 'AAAA':
		json = {
			name: $('[name="aaaaname[]"]', item).val(),
			'class': $('[name="aaaaclass[]"]', item).val()
		};
		subitem = $('table', item);
		break;
	case 'CNAME':
		json = {
			name: $('[name="cnamename[]"]', item).val(),
			'class': $('[name="cnameclass[]"]', item).val(),
			value: $('[name="cnamevalue[]"]', item).val()
		};
		break;
	case 'PTR':
		json = {
			name: $('[name="ptrname[]"]', item).val(),
			'class': $('[name="ptrclass[]"]', item).val(),
			value: $('[name="ptrvalue[]"]', item).val()
		};
		break;
	case 'TXT':
		json = {
			name: $('[name="txtname[]"]', item).val(),
			'class': $('[name="txtclass[]"]', item).val()
		};
		subitem = $('table', item);
		break;
	default:
		json = {
			origin: $('[name="origin"]', item).val(),
			ttl: $('[name="ttl"]', item).val()
		};
		break;
	}
	
	if(subitem) {
		subdata = [];
		subitem.each(function() {
			subitemjson = subFormToData(this, typ);
			if(subitemjson !== null) {
				subdata.push(subitemjson);
			}
		});
		if(subdata.length > 0) {
			json.value = subdata;
		} else {
			json.value = '';
		}
	}
	
	for(key in json) {
		if(key == 'origin' || key == 'ttl' || json[key] != '') {
			empty = false;
			break;
		}
	}
	
	if(empty === true) {
		return null;
	} else {
		return json;
	}
}

function subFormToData(item, typ) {
	var json, empty = true;
	switch(typ) {
	case 'A':
		json =  {
			weight: $('[name="aweight[][]"]', item).val(),
			ip: $('[name="avalue[][]"]', item).val()
		};
		break;
	case'AAAA':
		json = {
			weight: $('[name="aaaaweight[][]"]', item).val(),
			ip: $('[name="aaaavalue[][]"]', item).val()
		};
		break;
	case 'TXT':
		json = $('[name="txtvalue[][]"]', item).val();
		break;
	default:
		return null;
	}
	
	if(typeof json == 'object') {
		for(key in json) {
			if(json[key] != '') {
				empty = false;
				break;
			}
		}
	} else if(json != '') {
		empty = false;
	}
	
	if(empty === true) {
		return null;
	} else {
		return json;
	}
}

loadMenu(drawMenu);
// drawDomainForm();
loadDomain('example.com', drawDomainForm);