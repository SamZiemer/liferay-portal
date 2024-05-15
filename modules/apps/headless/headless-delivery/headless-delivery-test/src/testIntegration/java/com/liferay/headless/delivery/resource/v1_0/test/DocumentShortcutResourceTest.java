/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.headless.delivery.resource.v1_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.headless.delivery.client.dto.v1_0.DocumentShortcut;
import com.liferay.headless.delivery.client.resource.v1_0.DocumentShortcutResource;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileShortcut;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.constants.TestDataConstants;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.Inject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.text.DateFormat;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Javier Gamarra
 */
@RunWith(Arquillian.class)
public class DocumentShortcutResourceTest
	extends BaseDocumentShortcutResourceTestCase {

	@BeforeClass
	public static void setUpClass() throws Exception {
		_dateFormat = DateFormatFactoryUtil.getSimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	@Override
	@Test
	public void testGetDocumentShortcut() throws Exception {
		byte[] bytes = TestDataConstants.TEST_BYTE_ARRAY;

		InputStream inputStream = new ByteArrayInputStream(bytes);

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				testGroup.getGroupId(), TestPropsValues.getUserId());

		String name = PrincipalThreadLocal.getName();

		PermissionChecker permissionChecker =
			PermissionThreadLocal.getPermissionChecker();

		String password = StringUtil.randomString();

		User user = UserTestUtil.addUser(
			testCompany.getCompanyId(), testCompany.getUserId(), password,
			RandomTestUtil.randomString() + "@liferay.com",
			RandomTestUtil.randomString(), LocaleUtil.getDefault(),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(), null,
			ServiceContextTestUtil.getServiceContext());

		Role adminRole = RoleLocalServiceUtil.getRole(
			testCompany.getCompanyId(), RoleConstants.ADMINISTRATOR);

		_userLocalService.addRoleUser(adminRole.getRoleId(), user);

		PrincipalThreadLocal.setName(user.getUserId());

		PermissionThreadLocal.setPermissionChecker(
			PermissionCheckerFactoryUtil.create(user));

		try {
			FileEntry fileEntry = _dlAppService.addFileEntry(
				null, testGroup.getGroupId(),
				DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
				RandomTestUtil.randomString(),
				ContentTypes.APPLICATION_OCTET_STREAM, "title", "urltitle",
				StringPool.BLANK, StringPool.BLANK, inputStream, bytes.length,
				null, null, null, serviceContext);

			FileShortcut fileShortcut = _dlAppService.addFileShortcut(
				fileEntry.getRepositoryId(), fileEntry.getFolderId(),
				fileEntry.getFileEntryId(), serviceContext);

			DocumentShortcutResource.Builder builder =
				DocumentShortcutResource.builder();

			DocumentShortcutResource userDocumentResource =
				builder.authentication(
					user.getLogin(), password
				).build();

			DocumentShortcut getDocumentShortcut =
				userDocumentResource.getDocumentShortcut(
					fileShortcut.getFileShortcutId());

			assertEquals(fileShortcut, getDocumentShortcut);
			assertValid(getDocumentShortcut);
		}
		finally {
			PrincipalThreadLocal.setName(name);

			PermissionThreadLocal.setPermissionChecker(permissionChecker);

			_userLocalService.deleteUser(user);
		}
	}

	protected void assertEquals(
		FileShortcut fileShortcut, DocumentShortcut documentShortcut) {

		Assert.assertTrue(
			fileShortcut + " does not equal " + documentShortcut,
			equals(fileShortcut, documentShortcut));
	}

	protected boolean equals(
		FileShortcut fileShortcut, DocumentShortcut documentShortcut) {

		if (!StringUtil.equals(
				_dateFormat.format(fileShortcut.getCreateDate()),
				_dateFormat.format(documentShortcut.getDateCreated())) ||
			!StringUtil.equals(
				_dateFormat.format(fileShortcut.getModifiedDate()),
				_dateFormat.format(documentShortcut.getDateModified())) ||
			(fileShortcut.getFolderId() != documentShortcut.getFolderId()) ||
			(fileShortcut.getFileShortcutId() != documentShortcut.getId()) ||
			(fileShortcut.getGroupId() != documentShortcut.getSiteId()) ||
			(fileShortcut.getToFileEntryId() !=
				documentShortcut.getTargetDocumentId()) ||
			!StringUtil.equals(
				fileShortcut.getToTitle(), documentShortcut.getTitle())) {

			return false;
		}

		return true;
	}

	private static DateFormat _dateFormat;

	@Inject
	private static DLAppService _dlAppService;

	@Inject
	private static UserLocalService _userLocalService;

}