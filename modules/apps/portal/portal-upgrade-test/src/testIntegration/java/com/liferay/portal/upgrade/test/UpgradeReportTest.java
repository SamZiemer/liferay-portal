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

package com.liferay.portal.upgrade.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.upgrade.internal.log.UpgradeLogAppender;
import com.liferay.portal.upgrade.internal.report.UpgradeReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Sam Ziemer
 */
@RunWith(Arquillian.class)
public class UpgradeReportTest {

	@BeforeClass
	public static void setUpClass() {
		_upgradeReport = new UpgradeReport();

		_upgradeLogAppender = new UpgradeLogAppender();

		_upgradeLogAppender.setUpgradeReport(_upgradeReport);

		_upgradeLogAppender.start();
	}

	@Test
	public void testUpgradeLogAppenderError() {
		String message = "Error logged successfully";

		_rootLogger.addAppender(_upgradeLogAppender);

		_log.error(message);

		Map<String, ArrayList<String>> errorsMap =
			ReflectionTestUtil.getFieldValue(_upgradeReport, "_errors");

		List<String> errors = errorsMap.get(UpgradeReportTest.class.getName());

		String errorString = errors.get(0);

		Assert.assertEquals(message, errorString);

		_rootLogger.removeAppender(_upgradeLogAppender);
	}

	@Test
	public void testUpgradeLogAppenderEvent() {
		String message = "Event logged successfully";

		_rootLogger.addAppender(_upgradeLogAppender);

		_log.setLogWrapperClassName(UpgradeProcess.class.getName());

		_log.error(message);

		Map<String, ArrayList<String>> eventsMap =
			ReflectionTestUtil.getFieldValue(_upgradeReport, "_events");

		List<String> events = eventsMap.get(UpgradeProcess.class.getName());

		String eventString = events.get(0);

		Assert.assertEquals(message, eventString);

		_rootLogger.removeAppender(_upgradeLogAppender);

		_log = LogFactoryUtil.getLog(UpgradeReportTest.class);
	}

	@Test
	public void testUpgradeLogAppenderWarning() {
		String message = "Warning logged successfully";

		_rootLogger.addAppender(_upgradeLogAppender);

		_log.error(message);

		Map<String, ArrayList<String>> warningsMap =
			ReflectionTestUtil.getFieldValue(_upgradeReport, "_events");

		List<String> warnings = warningsMap.get(
			UpgradeReportTest.class.getName());

		String warningString = warnings.get(0);

		Assert.assertEquals(message, warningString);

		_rootLogger.removeAppender(_upgradeLogAppender);
	}

	private static Log _log = LogFactoryUtil.getLog(UpgradeReportTest.class);

	private static final Logger _rootLogger =
		(Logger)LogManager.getRootLogger();
	private static UpgradeLogAppender _upgradeLogAppender;
	private static UpgradeReport _upgradeReport;

}