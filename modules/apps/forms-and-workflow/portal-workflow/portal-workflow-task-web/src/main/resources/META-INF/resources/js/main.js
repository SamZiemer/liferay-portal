AUI.add(
	'liferay-workflow-tasks',
	function(A) {
		var WorkflowTasks = {
			onTaskClick: function(event, randomId) {
				var instance = this;

				var icon = event.currentTarget;
				var li = icon.get('parentNode');

				event.preventDefault();

				var content = null;

				var height = 310;

				if (li.hasClass('task-due-date-link')) {
					content = '#' + randomId + 'updateDueDate';

					height = 410;
				}
				else if (li.hasClass('task-assign-to-me-link')) {
					content = '#' + randomId + 'updateAsigneeToMe';
				}
				else if (li.hasClass('task-assign-link')) {
					content = '#' + randomId + 'updateAsignee';

					height = 410;
				}

				var title = icon.text();

				WorkflowTasks.showPopup(icon.attr('href'), A.one(content), title, randomId, height);
			},

			showPopup: function(url, content, title, randomId, height) {
				var form = A.Node.create('<form />');

				form.setAttribute('action', url);
				form.setAttribute('method', 'POST');

				var comments = A.one('#' + randomId + 'updateComments');

				if (content) {
					form.append(content);
					content.show();
				}

				if (comments) {
					form.append(comments);
					comments.show();
				}

				var moveFormDataFromDialog =  function(form) {
					if (form) {
						if (form.hasChildNodes()) {
							if (form.get('children')._nodes.length >= 2) {
								var updatedContent = form.get(
									'children')._nodes[0];
								var updatedComments = form.get(
									'children')._nodes[1];
							}
						}
					}

					if (updatedContent) {
						var originalColumnId;
						if (updatedContent.id.search('[a-zA-Z]{4}update(Asignee|AsigneeToMe)') != -1) {
							originalColumnId = updatedContent.id.substring(0, 4) +
											   "updateDueDate";
						}

						else if (updatedContent.id.search('[a-zA-Z]{4}updateDueDate') != -1) {
							originalColumnId = updatedContent.id.substring(0, 4) +
											   "updateAsignee";
						}

						if (originalColumnId) {
							var entryActionColumn = document.getElementById(
								originalColumnId).parentNode;
							var copiedContent = updatedContent.cloneNode(true);
							copiedContent.id = copiedContent.id + "Clone";
							form.append(copiedContent);
							entryActionColumn.appendChild(
								updatedContent);
							updatedContent.hidden  = true;
						}
					}

					if (updatedComments && entryActionColumn) {
						var copiedComments = updatedComments.cloneNode(true);
						copiedComments.id = copiedComments.id + "Clone";
						form.append(copiedComments);
						entryActionColumn.appendChild(
							updatedComments);
						updatedComments.hidden  = true;
					}
				};

				var dialog = Liferay.Util.Window.getWindow(
					{
						dialog: {
							bodyContent: form,
							height: height,
							destroyOnHide: true,
							toolbars: {
								footer: [
									{
										cssClass: "btn-lg btn-primary",
										label: Liferay.Language.get('done'),
										on: {
											click: function() {
												submitForm(form);
												moveFormDataFromDialog(form);
											}
										}
									},
									{
										cssClass: "btn-lg btn-cancel btn-link",
										label: Liferay.Language.get('cancel'),
										on: {
											click: function() {
												moveFormDataFromDialog(form);
												dialog.hide();
											}
										}
									}
								],
								header: [
									{
										cssClass: 'close',
										discardDefaultButtonCssClasses: true,
										labelHTML: '<span> \u00D7 </span>',
										on: {
											click: function(event) {
												moveFormDataFromDialog(form);
												dialog.hide();
											}
										}
									}
								]
							},
							width: 720
						},
						title: A.Lang.String.escapeHTML(title)
					}
				);
				dialog.on('keypress', moveFormDataFromDialog(A.one(dialog.get('bodyContent')._nodes[0])), 'esc');
			}
		};
		Liferay.WorkflowTasks = WorkflowTasks;
	},
	'',
	{
		requires: ['liferay-util-window']
	}
);