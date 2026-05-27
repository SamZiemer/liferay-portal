/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {dataApiHelpersTest} from '../../../../fixtures/dataApiHelpersTest';
import {featureFlagsTest} from '../../../../fixtures/featureFlagsTest';
import {isolatedSiteTest} from '../../../../fixtures/isolatedSiteTest';
import {loginTest} from '../../../../fixtures/loginTest';
import getRandomString from '../../../../utils/getRandomString';
import {enableLocalStaging} from '../../../../utils/staging';
import {cmsPagesTest} from '../fixtures/cmsPagesTest';

const test = mergeTests(
	cmsPagesTest,
	dataApiHelpersTest,
	featureFlagsTest({
		'LPD-17564': {enabled: true},
	}),
	isolatedSiteTest,
	loginTest()
);

test(
	'Distinguishes live and staging rows and hides disconnect on the live row',
	{tag: ['@LPD-91060', '@LPD-91062']},
	async ({apiHelpers, page, site, spaceSummaryPage}) => {
		const spaceName = `Space ${getRandomString()}`;

		// Create the Space and enable staging on the site first

		const space = await apiHelpers.headlessAssetLibrary.createAssetLibrary({
			name: spaceName,
			settings: {logoColor: 'outline-3'},
			type: 'Space',
		});

		await enableLocalStaging(apiHelpers, page, site);

		await expect(async () => {
			await page.reload();

			await expect(
				page.getByText('An initial staging publish')
			).toBeHidden({timeout: 5000});
		}).toPass({intervals: [1000], timeout: 60_000});

		// Connect the staged site, which mirrors the rel onto the staging group

		await apiHelpers.headlessAssetLibrary.connectSite(
			space.externalReferenceCode,
			site.externalReferenceCode
		);

		// Navigate to the Space summary

		await spaceSummaryPage.goto(spaceName);

		const stagingWord = await page.evaluate(() =>
			Liferay.Language.get('staging')
		);

		const connectedSites = page.getByTestId(
			'space-summary-connected-sites'
		);

		const liveRow = connectedSites.getByRole('row').filter({
			has: page.getByText(site.name, {exact: true}),
		});
		const stagingRow = connectedSites.getByRole('row').filter({
			hasText: `${site.name} (${stagingWord})`,
		});

		// Both rows are visible with the right labels

		await expect(liveRow).toBeVisible();
		await expect(stagingRow).toBeVisible();

		// The live row's actions menu hides Disconnect but keeps the kebab

		await liveRow.getByRole('button', {name: 'Actions'}).click();

		await expect(
			page.getByRole('menuitem', {name: 'Disconnect'})
		).not.toBeVisible();
		await expect(
			page
				.getByRole('menuitem', {name: 'Make Searchable'})
				.or(page.getByRole('menuitem', {name: 'Make Unsearchable'}))
		).toBeVisible();

		await page.keyboard.press('Escape');

		// The staging row's actions menu still offers Disconnect

		await stagingRow.getByRole('button', {name: 'Actions'}).click();

		await expect(
			page.getByRole('menuitem', {name: 'Disconnect'})
		).toBeVisible();
	}
);
