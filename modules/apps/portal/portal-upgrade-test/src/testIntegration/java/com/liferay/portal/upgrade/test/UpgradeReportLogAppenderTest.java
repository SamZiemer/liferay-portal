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
import com.liferay.portal.kernel.model.Release;
import com.liferay.portal.kernel.service.ReleaseLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.io.File;
import java.io.InputStream;

import org.apache.logging.log4j.core.Appender;

import org.hamcrest.CoreMatchers;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Sam Ziemer
 */
@RunWith(Arquillian.class)
public class UpgradeReportLogAppenderTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		Release release = _releaseLocalService.getRelease(1);

		_initialBuildNumber = release.getBuildNumber();
		_initialSchemaVersion = release.getSchemaVersion();

		release = _releaseLocalService.getRelease(1);

		release.setBuildNumber(ReleaseInfo.RELEASE_7_1_0_BUILD_NUMBER);
		release.setSchemaVersion("1.0.0");

		_releaseLocalService.updateRelease(release);

		_appender.start();

		release = _releaseLocalService.getRelease(1);

		release.setBuildNumber(7402);
		release.setSchemaVersion("12.2.2");

		_releaseLocalService.updateRelease(release);

		_appender.stop();
	}

	@After
	public void tearDown() throws Exception {
		Release release = _releaseLocalService.getRelease(1);

		release.setBuildNumber(_initialBuildNumber);
		release.setSchemaVersion(_initialSchemaVersion);

		_releaseLocalService.updateRelease(release);
	}

	@Test
	public void testGenerateReportDir() {
		File reportsDir = new File(".", "reports");

		Assert.assertTrue(reportsDir.exists());
	}

	@Test
	public void testGenerateReportFile() {
		File reportsDir = new File(".", "reports");

		File reportFile = new File(reportsDir, "upgrade_report.info");

		Assert.assertTrue(reportFile.exists());
	}

	@Test
	public void testReportContents() throws Exception {
		File reportsDir = new File(".", "reports");

		File reportFile = new File(reportsDir, "upgrade_report.info");

		Assert.assertThat(
			FileUtil.read(reportFile),
			CoreMatchers.containsString(_read("upgrade_reportTest.info")));
	}

	private String _read(String fileName) throws Exception {
		Class<?> clazz = getClass();

		InputStream inputStream = clazz.getResourceAsStream(
			"dependencies/" + fileName);

		return StringUtil.read(inputStream);
	}

	@Inject
	private Appender _appender;

	private int _initialBuildNumber;
	private String _initialSchemaVersion;

	@Inject
	private ReleaseLocalService _releaseLocalService;

}