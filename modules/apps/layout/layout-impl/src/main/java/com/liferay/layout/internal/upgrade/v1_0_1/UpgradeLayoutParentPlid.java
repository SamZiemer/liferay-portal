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

package com.liferay.layout.internal.upgrade.v1_0_1;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.dao.orm.common.SQLTransformer;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Matthew Chan
 */
public class UpgradeLayoutParentPlid extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		StringBundler sb = new StringBundler(3);

		sb.append("select child.plid as childPlid, parent.plid as parentPlid ");
		sb.append("from Layout child, Layout parent ");
		sb.append("where child.parentLayoutId = parent.layoutId");

		String sql = SQLTransformer.transform(sb.toString());

		try (PreparedStatement ps = connection.prepareStatement(sql);
			PreparedStatement ps2 = AutoBatchPreparedStatementUtil.autoBatch(
				connection.prepareStatement(
					"update Layout set parentPlid = ? where plid = ?"));
			ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				long childPlid = rs.getLong("childPlid");
				long parentPlid = rs.getLong("parentPlid");

				ps2.setLong(1, parentPlid);
				ps2.setLong(2, childPlid);

				ps2.addBatch();
			}

			ps2.executeBatch();
		}
	}

}