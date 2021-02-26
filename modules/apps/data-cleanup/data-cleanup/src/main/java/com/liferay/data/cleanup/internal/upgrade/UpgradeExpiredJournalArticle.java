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

package com.liferay.data.cleanup.internal.upgrade;

import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Kevin Lee
 */
public class UpgradeExpiredJournalArticle extends UpgradeProcess {

	public UpgradeExpiredJournalArticle(
		JournalArticleLocalService journalArticleLocalService) {

		_journalArticleLocalService = journalArticleLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement ps1 = connection.prepareStatement(
				"select id_ from JournalArticle where status = " +
					WorkflowConstants.STATUS_EXPIRED)) {

			ResultSet rs1 = ps1.executeQuery();

			while (rs1.next()) {
				long id = rs1.getLong("id_");

				_journalArticleLocalService.deleteArticle(
					_journalArticleLocalService.getArticle(id));
			}
		}
	}

	private final JournalArticleLocalService _journalArticleLocalService;

}