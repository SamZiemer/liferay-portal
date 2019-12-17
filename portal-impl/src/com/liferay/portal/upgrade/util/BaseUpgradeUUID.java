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

package com.liferay.portal.upgrade.util;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Samuel Ziemer
 */
public abstract class BaseUpgradeUUID extends UpgradeProcess {

	protected void doUpgrade() throws Exception {
		upgradeUUID();
	}

	protected abstract String getPrimaryKeyColumnName();

	protected abstract String getTableName();

	protected void upgradeUUID() throws Exception {
		DB db = DBManagerUtil.getDB();

		if (db.isSupportsNewUuidFunction()) {
			try (PreparedStatement ps = connection.prepareStatement(
					StringBundler.concat(
						"update ", getTableName(), " set uuid_ = ",
						db.getNewUuidFunctionName(),
						" where uuid_ is null or uuid_ = ''"))) {

				ps.executeUpdate();

				return;
			}
		}

		StringBundler sb = new StringBundler(5);

		sb.append("update ");
		sb.append(getTableName());
		sb.append(" set uuid_ = ? where ");
		sb.append(getPrimaryKeyColumnName());
		sb.append(" = ?");

		try (PreparedStatement ps1 = connection.prepareStatement(
				StringBundler.concat(
					"select ", getPrimaryKeyColumnName(), " from ",
					getTableName(), " where uuid_ is null or uuid_ = ''"));
			ResultSet rs = ps1.executeQuery();
			PreparedStatement ps2 = AutoBatchPreparedStatementUtil.autoBatch(
				connection.prepareStatement(sb.toString()))) {

			while (rs.next()) {
				long pk = rs.getLong(getPrimaryKeyColumnName());

				ps2.setString(1, PortalUUIDUtil.generate());
				ps2.setLong(2, pk);

				ps2.addBatch();
			}

			ps2.executeBatch();
		}
	}

}