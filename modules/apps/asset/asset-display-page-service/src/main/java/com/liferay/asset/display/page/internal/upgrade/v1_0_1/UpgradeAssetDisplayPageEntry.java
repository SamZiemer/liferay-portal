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

package com.liferay.asset.display.page.internal.upgrade.v1_0_1;

import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Samuel Ziemer
 */
public class UpgradeAssetDisplayPageEntry extends UpgradeProcess {

	public UpgradeAssetDisplayPageEntry(
		CounterLocalService counterLocalService) {

		_counterLocalService = counterLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		long blogsClassNameId = PortalUtil.getClassNameId(
			"com.liferay.blogs.model.BlogsEntry");

		long dlFileEntryClassNameId = PortalUtil.getClassNameId(
			"com.liferay.document.library.kernel.model.DLFileEntry");

		long journalArticleClassNameId = PortalUtil.getClassNameId(
			"com.liferay.journal.model.JournalArticle");

		long fileEntryClassNameId = PortalUtil.getClassNameId(
			"com.liferay.portal.kernel.repository.model.FileEntry");

		StringBundler sb1 = new StringBundler(10);

		sb1.append("select groupId, companyId, userId, userName, createDate, ");
		sb1.append("modifiedDate, classNameId, classPK from AssetEntry where ");
		sb1.append("classNameId in (");
		sb1.append(blogsClassNameId);
		sb1.append(", ");
		sb1.append(dlFileEntryClassNameId);
		sb1.append(", ");
		sb1.append(journalArticleClassNameId);
		sb1.append(") and classPK not in (select classPK from ");
		sb1.append("AssetDisplayPageEntry)");

		StringBundler sb2 = new StringBundler(5);

		sb2.append("insert into AssetDisplayPageEntry (uuid_, ");
		sb2.append("assetDisplayPageEntryId, groupId, companyId, userId, ");
		sb2.append("userName, createDate, modifiedDate, classNameId, ");
		sb2.append("classPK, layoutPageTemplateEntryId, type_, plid) values( ");
		sb2.append("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

		try (PreparedStatement ps1 = connection.prepareStatement(
				sb1.toString());
			PreparedStatement ps2 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection, sb2.toString())) {

			try (ResultSet rs = ps1.executeQuery()) {
				while (rs.next()) {
					ps2.setString(1, PortalUUIDUtil.generate());
					ps2.setLong(2, _counterLocalService.increment());
					ps2.setLong(3, rs.getLong("groupId"));
					ps2.setLong(4, rs.getLong("companyId"));
					ps2.setLong(5, rs.getLong("userId"));
					ps2.setString(6, rs.getString("userName"));
					ps2.setTimestamp(7, rs.getTimestamp("createDate"));
					ps2.setTimestamp(8, rs.getTimestamp("modifiedDate"));

					long classNameId = rs.getLong("classNameId");

					if (classNameId == dlFileEntryClassNameId) {
						classNameId = fileEntryClassNameId;
					}

					ps2.setLong(9, classNameId);
					ps2.setLong(10, rs.getLong("classPK"));
					ps2.setLong(11, 0);
					ps2.setLong(12, 1);
					ps2.setLong(13, 0);

					ps2.addBatch();
				}

				ps2.executeBatch();
			}
		}
	}

	private final CounterLocalService _counterLocalService;

}