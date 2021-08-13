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
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.version.Version;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.internal.report.UpgradeReport;

import java.io.File;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Sam Ziemer
 */
@RunWith(Arquillian.class)
public class UpgradeReportTest extends UpgradeReport {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testBuildNumber() {
		Assert.assertTrue(initialBuildNumber == -1);

		setInitialBuildNumber();

		Assert.assertTrue(initialBuildNumber == ReleaseInfo.getBuildNumber());
	}

	@Test
	public void testLogFileRename() {
		File logFile = getLogFile();

		File parentFile = logFile.getParentFile();

		String[] parentDirectoryList = parentFile.list();

		logFile = getLogFile();

		parentFile = logFile.getParentFile();

		String[] newParentDirectoryList = parentFile.list();

		Assert.assertTrue(
			newParentDirectoryList.length > parentDirectoryList.length);
	}

	@Test
	public void testSchemaVersion() {
		Assert.assertTrue(initialSchemaVersion == null);

		setInitialSchemaVersion();

		Version initialVersion = Version.parseVersion(initialSchemaVersion);

		Version releaseVersion = Version.parseVersion(ReleaseInfo.getVersion());

		Assert.assertTrue(initialVersion.equals(releaseVersion));
	}

}