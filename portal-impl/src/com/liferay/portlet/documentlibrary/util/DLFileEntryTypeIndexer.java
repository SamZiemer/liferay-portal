/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portlet.documentlibrary.util;

import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.DocumentImpl;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.Summary;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntryType;
import com.liferay.portlet.documentlibrary.service.DLFileEntryTypeLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.permission.DLFileEntryTypePermission;

import java.util.Locale;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletURL;

/**
 * @author Sam Ziemer
 */
public class DLFileEntryTypeIndexer extends BaseIndexer {

	public static final String[] CLASS_NAMES =
		{DLFileEntryType.class.getName()};

	public static final String PORTLET_ID = PortletKeys.DOCUMENT_LIBRARY_ADMIN;

	public DLFileEntryTypeIndexer() {
		setDefaultSelectedFieldNames(
			Field.NAME, Field.COMPANY_ID, Field.GROUP_ID, Field.UID,
			Field.CREATE_DATE, Field.MODIFIED_DATE);
		setFilterSearch(true);
		setPermissionAware(true);
	}

	@Override
	public String[] getClassNames() {
		return CLASS_NAMES;
	}

	@Override
	public String getPortletId() {
		return PORTLET_ID;
	}

	@Override
	public boolean hasPermission(
			PermissionChecker permissionChecker, String entryClassName,
			long entryClassPK, String actionId)
			throws Exception {

		return DLFileEntryTypePermission.contains(
				permissionChecker, entryClassPK, ActionKeys.VIEW);
	}

	@Override
	protected void doDelete(Object obj) throws Exception {
		DLFileEntryType dlFileEntryType = (DLFileEntryType)obj;

		Document document = new DocumentImpl();

		document.addUID(PORTLET_ID, dlFileEntryType.getFileEntryTypeId());

		SearchEngineUtil.deleteDocument(
				getSearchEngineId(), dlFileEntryType.getCompanyId(),
				document.get(Field.UID), isCommitImmediately());
	}

	@Override
	protected Document doGetDocument(Object obj) throws Exception {
	DLFileEntryType dlFileEntryType = (DLFileEntryType)obj;

	if (_log.isDebugEnabled()) {
		_log.debug("Indexing document type " + dlFileEntryType);
	}

		Document document = getBaseModelDocument(PORTLET_ID, dlFileEntryType);

		document.addKeyword(Field.NAME, dlFileEntryType.getName());
		document.addDate(Field.CREATE_DATE, dlFileEntryType.getCreateDate());
		document.addDate(Field.MODIFIED_DATE,
			dlFileEntryType.getModifiedDate());



	if (_log.isDebugEnabled()) {
		_log.debug("Document type " + dlFileEntryType +
			" indexed successfully.");
	}
		return document;
	}

	@Override
	protected Summary doGetSummary(
		Document document, Locale locale, String snippet, PortletURL portletURL,
		PortletRequest portletRequest, PortletResponse portletResponse) {

		return null;
	}

	@Override
	protected void doReindex(Object obj) throws Exception {
		DLFileEntryType dlFileEntryType = (DLFileEntryType)obj;

		Document document = getDocument(dlFileEntryType);

		if (document != null) {
			SearchEngineUtil.updateDocument(
					getSearchEngineId(), dlFileEntryType.getCompanyId(), document,
					isCommitImmediately());
		}
	}

	@Override
	protected void doReindex(String className, long classPK) throws Exception {
		DLFileEntryType dlFileEntryType =
			DLFileEntryTypeLocalServiceUtil.getDLFileEntryType(classPK);

		doReindex(dlFileEntryType);
	}

	@Override
	protected void doReindex(String[] ids) throws Exception {
		long companyId = GetterUtil.getLong(ids[0]);

		reindexFileEntryTypes(companyId);
	}

	@Override
	protected String getPortletId(SearchContext searchContext) {
		return PORTLET_ID;
	}

	protected void reindexFileEntryTypes(final long companyId)
			throws PortalException {

		final ActionableDynamicQuery actionableDynamicQuery =
				AssetCategoryLocalServiceUtil.getActionableDynamicQuery();

		actionableDynamicQuery.setCompanyId(companyId);
		actionableDynamicQuery.setPerformActionMethod(
				new ActionableDynamicQuery.PerformActionMethod() {

					@Override
					public void performAction(Object object)
							throws PortalException {

						DLFileEntryType dlFileEntryType = (DLFileEntryType)object;

						Document document = getDocument(dlFileEntryType);

						if (document != null) {
							actionableDynamicQuery.addDocument(document);
						}
					}

				});
		actionableDynamicQuery.setSearchEngineId(getSearchEngineId());

		actionableDynamicQuery.performActions();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DLFileEntryType.class);
}
