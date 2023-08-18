/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.document.library.web.internal.portlet.action;

import com.liferay.depot.group.provider.SiteConnectedGroupGroupProvider;
import com.liferay.document.library.constants.DLPortletKeys;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFileShortcut;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileShortcutLocalService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;

import java.util.HashMap;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Sam Ziemer
 */
@Component(
	property = {
		"javax.portlet.name=" + DLPortletKeys.DOCUMENT_LIBRARY,
		"javax.portlet.name=" + DLPortletKeys.DOCUMENT_LIBRARY_ADMIN,
		"javax.portlet.name=" + DLPortletKeys.MEDIA_GALLERY_DISPLAY,
		"mvc.command.name=/document_library/bulk_copy_entries"
	},
	service = MVCActionCommand.class
)
public class BulkCopyEntriesMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		_copyEntities(actionRequest);

		if (!_errorsMap.isEmpty()) {
			String errorMessage = themeDisplay.translate(
				"documents-folders-could-not-be-copied");

			if (_errorsMap.size() < 10) {
				errorMessage = themeDisplay.translate(
						"the-following-documents-folders-could-not-be-copied");
			}

			JSONPortletResponseUtil.writeJSON(
				actionRequest, actionResponse,
				JSONUtil.put("errorMessage", errorMessage));

			hideDefaultSuccessMessage(actionRequest);

			_errorsMap.clear();
		}
	}

	private void _checkDestinationGroup(Group group) throws PortalException {
		if ((group != null) && group.isStaged() && !group.isStagingGroup()) {
			throw new PortalException(
				"cannot-copy-into-the-live-version-of-a-group");
		}
	}

	private void _copyEntities(ActionRequest actionRequest)
		throws PortalException {

		long destinationFolderId = ParamUtil.getLong(
			actionRequest, "destinationFolderId");
		long destinationRepositoryId = ParamUtil.getLong(
			actionRequest, "destinationRepositoryId");
		long[] entries = ParamUtil.getLongValues(
			actionRequest, "selectedEntries");

		Group group = _groupLocalService.fetchGroup(destinationRepositoryId);

		_checkDestinationGroup(group);

		long[] currentAndAncestorSiteAndDepotGroupIds =
			_siteConnectedGroupGroupProvider.
				getCurrentAndAncestorSiteAndDepotGroupIds(group.getGroupId());

		for (long entryId : entries) {
			DLFileEntry dlFileEntry = _dlFileEntryLocalService.fetchDLFileEntry(entryId);

			DLFileShortcut dlFileShortcut =
				_dlFileShortcutLocalService.fetchDLFileShortcut(entryId);

			DLFolder dlFolder = _dlFolderLocalService.fetchDLFolder(entryId);

			String name = StringPool.BLANK;

			try {
				if (dlFileEntry != null) {
					name = dlFileEntry.getName();

					_dlAppService.copyFileEntry(
						entryId, destinationFolderId, destinationRepositoryId,
						currentAndAncestorSiteAndDepotGroupIds,
						ServiceContextFactory.getInstance(
							DLFileEntry.class.getName(), actionRequest));
				}
				else if (dlFileShortcut != null) {
					name = dlFileShortcut.getToTitle();

					_dlAppService.copyFileShortcut(
						entryId, destinationFolderId, destinationRepositoryId,
						ServiceContextFactory.getInstance(
							DLFileShortcut.class.getName(), actionRequest));
				}
				else if (dlFolder != null) {
					name = dlFolder.getName();

					_dlAppService.copyFolder(
						dlFolder.getRepositoryId(), entryId,
						destinationRepositoryId, destinationFolderId,
						currentAndAncestorSiteAndDepotGroupIds,
						ServiceContextFactory.getInstance(
							DLFolder.class.getName(), actionRequest));
				}
			}
			catch (PortalException portalException) {
				_errorsMap.put(entryId, StringBundler.concat(
					name, StringPool.SPACE, portalException.getMessage()));
			}
		}
	}

	@Reference
	private DLAppService _dlAppService;

	@Reference
	private DLFileEntryLocalService _dlFileEntryLocalService;

	@Reference
	private DLFileShortcutLocalService _dlFileShortcutLocalService;

	@Reference
	private DLFolderLocalService _dlFolderLocalService;

	private final Map<Long, String> _errorsMap = new HashMap<>();

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private SiteConnectedGroupGroupProvider _siteConnectedGroupGroupProvider;

}