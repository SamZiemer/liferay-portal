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
import com.liferay.portal.dao.orm.common.SQLTransformer;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.service.ResourceLocalServiceUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Ziemer
 */
public abstract class BaseUpgradeResourceModel extends UpgradeProcess {

	public void doUpgrade() throws Exception {
		List<Long> companyIds = getCompanyIds(connection);

		for (long companyId : companyIds) {
			PreparedStatement ps = connection.prepareStatement(
				"select roleId from Role where companyId = " + companyId +
					" and name = 'owner'");

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				_upgradeResourcedModel(companyId, rs.getLong("roleId"));
			}
		}
	}

	public abstract String getModelName();

	public abstract String getPrimaryKeyColumnName();

	public abstract String getTableName();

	public abstract String getUserIdColumnName();

	protected List<Long> getCompanyIds(Connection connection)
		throws SQLException {

		List<Long> companyIds = new ArrayList<>();

		try (PreparedStatement ps = connection.prepareStatement(
				"select distinct companyId from Company");
			ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				long companyId = rs.getLong(1);

				companyIds.add(companyId);
			}
		}

		return companyIds;
	}

	private String _getVerifyResourcedModelSQL(
		boolean count, long companyId, long roleId) {

		StringBundler sb = new StringBundler(27);

		sb.append("select ");

		if (count) {
			sb.append("count(*)");
		}
		else {
			sb.append(getTableName());
			sb.append(".");
			sb.append(getPrimaryKeyColumnName());
			sb.append(", ");
			sb.append(getTableName());
			sb.append(".");
			sb.append(getUserIdColumnName());
		}

		sb.append(" from ");
		sb.append(getTableName());
		sb.append(" left join ResourcePermission on (ResourcePermission.");
		sb.append("companyId = ");
		sb.append(companyId);
		sb.append(" and ResourcePermission.name = '");
		sb.append(getModelName());
		sb.append("' and ResourcePermission.scope = 4 ");
		sb.append("and ResourcePermission.primKeyId = ");
		sb.append(getTableName());
		sb.append(".");
		sb.append(getPrimaryKeyColumnName());
		sb.append(" and ResourcePermission.roleId = ");
		sb.append(roleId);
		sb.append(") where ");
		sb.append(getTableName());
		sb.append(".companyId = ");
		sb.append(companyId);
		sb.append(" and ResourcePermission.primKeyId is NULL");

		return SQLTransformer.transform(sb.toString());
	}

	private void _upgradeResourcedModel(long companyId, long roleId)
		throws Exception {

		int total = 0;

		try (PreparedStatement ps = connection.prepareStatement(
				_getVerifyResourcedModelSQL(true, companyId, roleId));
			ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				total = rs.getInt(1);
			}
		}

		if (total == 0) {
			return;
		}

		try (PreparedStatement ps = connection.prepareStatement(
				_getVerifyResourcedModelSQL(false, companyId, roleId));
			ResultSet rs = ps.executeQuery()) {

			for (int i = 1; rs.next(); i++) {
				long primKey = rs.getLong(getPrimaryKeyColumnName());
				long userId = rs.getLong(getUserIdColumnName());

				_upgradeResourcedModel(
					companyId, getModelName(), primKey, roleId, userId, i,
					total);
			}
		}
	}

	private void _upgradeResourcedModel(
			long companyId, String modelName, long primKey, long roleId,
			long ownerId, int cur, int total)
		throws Exception {

		if (_log.isInfoEnabled() && ((cur % 100) == 0)) {
			_log.info(
				StringBundler.concat(
					"Processed ", cur, " of ", total,
					" resource permissions for company = ", companyId,
					" and model ", modelName));
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				StringBundler.concat(
					"No resource found for {", companyId, ", ", modelName, ", ",
					ResourceConstants.SCOPE_INDIVIDUAL, ", ", primKey, ", ",
					roleId, "}"));
		}

		/**
		 * @TODO
		 * This needs to be changed into SQL queries if possible rather than a local service call.
		 */

		ResourceLocalServiceUtil.addResources(
			companyId, 0, ownerId, modelName, String.valueOf(primKey), false,
			false, false);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		BaseUpgradeResourceModel.class);

}