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

package com.liferay.portal.upgrade.v7_0_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.portal.kernel.concurrent.ThrowableAwareRunnable;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.test.rule.ExpectedDBType;
import com.liferay.portal.test.rule.ExpectedLog;
import com.liferay.portal.test.rule.ExpectedLogs;
import com.liferay.portal.test.rule.ExpectedType;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.util.BaseUpgradeUUID;
import com.liferay.portal.upgrade.v7_0_0.UpgradeAssetTagUUID;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Manuel de la Peña
 * @author Samuel Ziemer
 */
@RunWith(Arquillian.class)
public class UpgradeUUIDTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testUpgrade() throws Exception {
		new UpgradeAssetTagUUID().upgrade();
	}

	@ExpectedLogs(
		expectedLogs = {
			@ExpectedLog(
				expectedDBType = ExpectedDBType.DB2,
				expectedLog = "DB2 SQL Error: SQLCODE=",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.HYPERSONIC,
				expectedLog = "user lacks privilege or object not found:",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.MARIADB,
				expectedLog = "Unknown column 'Unknown' in 'field list'",
				expectedType = ExpectedType.EXACT
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.MYSQL,
				expectedLog = "Unknown column 'Unknown' in 'field list'",
				expectedType = ExpectedType.EXACT
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.ORACLE,
				expectedLog = "ORA-00904: \"UNKNOWN\": invalid identifier",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.POSTGRESQL,
				expectedLog = "ERROR: column \"unknown\" does not exist",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.SYBASE,
				expectedLog = "Invalid column name 'Unknown'.",
				expectedType = ExpectedType.PREFIX
			)
		},
		level = "ERROR", loggerClass = ThrowableAwareRunnable.class
	)
	@Test
	public void testUpgradeModelWithUnknownPKColumnName() {
		try {
			new BaseUpgradeUUID() {

				@Override
				public String getPrimaryKeyColumnName() {
					return _UNKNOWN;
				}

				@Override
				public String getTableName() {
					return "Layout";
				}

			}.upgrade();
		}
		catch (Exception e) {
			_upgradeException("testUpgradeModelWithUnknownPKColumnName", e);
		}
	}

	@ExpectedLogs(
		expectedLogs = {
			@ExpectedLog(
				expectedDBType = ExpectedDBType.DB2,
				expectedLog = "DB2 SQL Error: SQLCODE=",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.HYPERSONIC,
				expectedLog = "user lacks privilege or object not found:",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.MARIADB,
				expectedLog = "java.sql.SQLSyntaxErrorException: Table ",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.MYSQL,
				expectedLog = "java.sql.SQLSyntaxErrorException: Table ",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.ORACLE,
				expectedLog = "ORA-00942: table or view does not exist",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.POSTGRESQL,
				expectedLog = "ERROR: relation \"unknown\" does not exist",
				expectedType = ExpectedType.PREFIX
			),
			@ExpectedLog(
				expectedDBType = ExpectedDBType.SYBASE,
				expectedLog = "Unknown not found.",
				expectedType = ExpectedType.PREFIX
			)
		},
		level = "ERROR", loggerClass = ThrowableAwareRunnable.class
	)
	@Test
	public void testUpgradeUnknownModelWithUnknownPKColumnName() {
		try {
			new BaseUpgradeUUID() {

				@Override
				public String getPrimaryKeyColumnName() {
					return _UNKNOWN;
				}

				@Override
				public String getTableName() {
					return _UNKNOWN;
				}

			}.upgrade();
		}
		catch (Exception e) {
			_upgradeException(
				"testUpgradeUnknownModelWithUnknownPKColumnName", e);
		}
	}

	private static void _upgradeException(String methodName, Exception e) {
		Method method = null;

		try {
			method = UpgradeUUIDTest.class.getMethod(methodName);
		}
		catch (NoSuchMethodException nsme) {
			ReflectionUtil.throwException(nsme);

			return;
		}

		ExpectedLogs expectedLogs = method.getAnnotation(ExpectedLogs.class);

		String message = e.getMessage();

		DB db = DBManagerUtil.getDB();

		DBType dbType = db.getDBType();

		for (ExpectedLog expectedLog : expectedLogs.expectedLogs()) {
			ExpectedDBType expectedDBType = expectedLog.expectedDBType();

			if (dbType != expectedDBType.getDBType()) {
				continue;
			}

			String logMessage = expectedLog.expectedLog();

			ExpectedType expectedType = expectedLog.expectedType();

			if (expectedType == ExpectedType.CONTAINS) {
				Assert.assertTrue(
					message + " does not contain " + logMessage,
					message.contains(logMessage));
			}
			else if (expectedType == ExpectedType.EXACT) {
				Assert.assertEquals(message, logMessage);
			}
			else if (expectedType == ExpectedType.POSTFIX) {
				Assert.assertTrue(
					message + " does not end " + logMessage,
					message.endsWith(logMessage));
			}
			else if (expectedType == ExpectedType.PREFIX) {
				Assert.assertTrue(
					message + " does not start " + logMessage,
					message.startsWith(logMessage));
			}
			else {
				throw new IllegalStateException(
					"Unknown ExpectedType" + expectedLog.expectedType(), e);
			}
		}
	}

	private static final String _UNKNOWN = "Unknown";

}