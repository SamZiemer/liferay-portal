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

package com.liferay.message.boards.internal.upgrade.v3_1_0;

import com.liferay.message.boards.internal.upgrade.v3_1_0.util.MBMessageTable;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Gamarra
 */
public class UrlSubjectUpgradeProcess extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		if (!hasColumn("MBMessage", "urlSubject")) {
			alter(
				MBMessageTable.class,
				new AlterTableAddColumn("urlSubject", "VARCHAR(255) null"));
		}

		_populateUrlSubject();
	}

	private String _getUrlSubject(long id, String subject) {
		if (subject == null) {
			return String.valueOf(id);
		}

		subject = StringUtil.toLowerCase(subject.trim());

		if (Validator.isNull(subject) || Validator.isNumber(subject) ||
			subject.equals("rss")) {

			subject = String.valueOf(id);
		}
		else {
			subject = FriendlyURLNormalizerUtil.normalizeWithPeriodsAndSlashes(
				subject);
		}

		return subject.substring(0, Math.min(subject.length(), 254));
	}

	private void _populateUrlSubject() throws Exception {
		try (PreparedStatement ps1 = connection.prepareStatement(
				"select messageId, subject from MBMessage");
			ResultSet rs = ps1.executeQuery();
			PreparedStatement ps2 = AutoBatchPreparedStatementUtil.autoBatch(
				connection.prepareStatement(
					"update MBMessage set urlSubject = ? where messageId = " +
						"?"))) {

			Map<String, Integer> map = new HashMap<>();

			while (rs.next()) {
				long messageId = rs.getLong(1);
				String subject = rs.getString(2);

				String urlSubject = _getUrlSubject(messageId, subject);

				if (map.containsKey(urlSubject)) {
					map.put(urlSubject, map.get(urlSubject) + 1);

					ps2.setString(
						1, urlSubject + StringPool.DASH + map.get(urlSubject));
				}
				else {
					map.put(urlSubject, 0);

					ps2.setString(1, urlSubject);
				}

				ps2.setLong(2, messageId);

				ps2.addBatch();
			}

			ps2.executeBatch();
		}
	}

}