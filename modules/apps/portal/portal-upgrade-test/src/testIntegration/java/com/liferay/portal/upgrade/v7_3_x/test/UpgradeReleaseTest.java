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

package com.liferay.portal.upgrade.v7_3_x.test;

import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.upgrade.v7_3_x.UpgradeRelease;

import java.sql.Connection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Samuel Ziemer
 */
public class UpgradeReleaseTest {

	@Before
	public void setUp() throws Exception {
		_db = DBManagerUtil.getDB();

		_db.runSQL("alter table Release_ add verified tinyint(1)");
	}

	@Test
	public void testUpgrade() throws Exception {
		DBInspector dbInspector;

		try (Connection connection = DataAccess.getConnection()) {
			dbInspector = new DBInspector(connection);

			Assert.assertTrue(dbInspector.hasColumn("Release_", "verified"));
		}

		UpgradeProcess upgradeProcess = new UpgradeRelease();

		upgradeProcess.upgrade();

		Assert.assertFalse(dbInspector.hasColumn("Release_", "verified"));
	}

	private DB _db;

}