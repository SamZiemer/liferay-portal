/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.portal.reports.engine.console.internal.upgrade.v1_0_1;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.util.LoggingTimer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Marcellus Tavares
 */
public class UpgradeKernelPackage
	extends com.liferay.portal.upgrade.v7_0_0.UpgradeKernelPackage {

	protected void deleteDuplicateResources(String newName, String oldName)
		throws UpgradeException {

		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			String selectSQL =
				"select actionId from ResourceAction where name like '" +
					newName + "%'";

			try (PreparedStatement ps = connection.prepareStatement(selectSQL);
				ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					runSQL(
						StringBundler.concat(
							"delete from ResourceAction where actionId = '",
							rs.getString(1), "' and name like '", oldName,
							"%'"));
				}
			}
			catch (Exception exception) {
				throw new UpgradeException(exception);
			}
		}
	}

	@Override
	protected void doUpgrade() throws UpgradeException {
		deleteDuplicateResources(_CLASS_NAMES[0][1], _CLASS_NAMES[0][0]);

		deleteDuplicateResources(_RESOURCE_NAMES[0][1], _RESOURCE_NAMES[0][0]);

		super.doUpgrade();
	}

	@Override
	protected String[][] getClassNames() {
		return _CLASS_NAMES;
	}

	@Override
	protected String[][] getResourceNames() {
		return _RESOURCE_NAMES;
	}

	private static final String[][] _CLASS_NAMES = {
		{
			"com.liferay.reports.model.",
			"com.liferay.portal.reports.engine.console.model."
		}
	};

	private static final String[][] _RESOURCE_NAMES = {
		{
			"com.liferay.reports.admin",
			"com.liferay.portal.reports.engine.console.admin"
		}
	};

}