/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';
import {readFileSync} from 'fs';
import path from 'path';

import {dataApiHelpersTest} from '../../../fixtures/dataApiHelpersTest';
import {featureFlagsTest} from '../../../fixtures/featureFlagsTest';
import {loginTest} from '../../../fixtures/loginTest';
import getRandomString from '../../../utils/getRandomString';
import {performUserSwitchViaApi, userData} from '../../../utils/performLogin';
import {waitForAlert} from '../../../utils/waitForAlert';
import {cmsPagesTest} from './fixtures/cmsPagesTest';

const test = mergeTests(
	cmsPagesTest,
	dataApiHelpersTest,
	featureFlagsTest({
		'LPD-11235': {enabled: false},
		'LPD-17564': {enabled: true},
		'LPD-34594': {enabled: true},
	}),
	loginTest()
);

test(
	'Confirmation modal is shown when delete a single content in a space with recycle bin disabled',
	{tag: '@LPD-64867'},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const spaceName = `Space ${getRandomString()}`;
		const file1Title = `<b>Content ${getRandomString()}</b>`;
		let space = null;

		await test.step('Create a new Space with recycle bin disabled', async () => {
			space = await apiHelpers.headlessAssetLibrary.createAssetLibrary({
				name: spaceName,
				settings: {
					trashEnabled: false,
				},
				type: 'Space',
			});
		});

		await test.step('Create a content for that space', async () => {
			await apiHelpers.objectEntry.postObjectEntry(
				{
					objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
					title: file1Title,
				},
				applicationName,
				spaceName
			);
		});

		await test.step('Delete content', async () => {
			await assetsPage.gotoAll();

			await assetsPage.execItemAction({
				action: 'Delete',
				filter: file1Title,
			});
		});

		await test.step('Accept confirmation modal', async () => {
			await expect(
				page.getByRole('heading', {name: `Delete "${file1Title}"`})
			).toBeVisible();

			await expect(
				page.getByText('You are about to delete the asset')
			).toBeVisible();

			await page.getByRole('button', {name: 'Delete'}).click();

			await waitForAlert(page, `${file1Title} was successfully deleted.`);

			await expect(
				page.getByRole('cell', {name: file1Title})
			).not.toBeVisible();
		});

		await test.step('delete created space', async () => {
			await apiHelpers.headlessAssetLibrary.deleteAssetLibrary(space.id);
		});
	}
);

test(
	'Only content folders will be displayed when copying content',
	{tag: '@LPD-72879'},
	async ({apiHelpers, assetsPage, page}) => {
		const file1Title = `Content ${getRandomString()}`;
		const file2Title = `File ${getRandomString()}`;
		const spaceName = `Space ${getRandomString()}`;

		await test.step('Create a new Space', async () => {
			await apiHelpers.headlessAssetLibrary.createAssetLibrary({
				name: spaceName,
				settings: {},
				type: 'Space',
			});
		});

		await test.step('Create a content for that space', async () => {
			await apiHelpers.objectEntry.postObjectEntry(
				{
					objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
					title: file1Title,
				},
				'cms/basic-web-contents',
				spaceName
			);
		});

		await test.step('Create a file for that space', async () => {
			await apiHelpers.objectEntry.postObjectEntry(
				{
					file: {
						fileBase64: 'R0lGODlhAQABAAAAACw=',
						name: `file_${getRandomString()}.png`,
					},
					objectEntryFolderExternalReferenceCode: 'L_FILES',
					title: file2Title,
				},
				'cms/basic-documents',
				spaceName
			);
		});

		await test.step('Copy content', async () => {
			await assetsPage.gotoAll();

			await assetsPage.execItemAction({
				action: 'Copy To',
				filter: file1Title,
				parentAction: 'Copy',
			});
		});

		await test.step('Check content folders', async () => {
			await page.getByLabel(spaceName).click();
			await expect(
				page.getByText('Showing 1 to 1 of 1 entries.')
			).toBeVisible();

			await expect(
				page.getByLabel('Contents', {exact: true})
			).toBeVisible();
		});

		await test.step('Copy file', async () => {
			await assetsPage.gotoAll();

			await assetsPage.execItemAction({
				action: 'Copy To',
				filter: file2Title,
				parentAction: 'Copy',
			});
		});

		await test.step('Check file folders', async () => {
			await page.getByLabel(spaceName).click();
			await expect(
				page.getByText('Showing 1 to 1 of 1 entries.')
			).toBeVisible();

			await expect(page.getByLabel('Files', {exact: true})).toBeVisible();
		});
	}
);

test(
	'Duplicating content creates a draft copy in the same Space',
	{tag: '@LPD-88346'},
	async ({apiHelpers, assetsPage, page}) => {
		const fileTitle = `Content ${getRandomString()}`;
		const spaceName = 'Default';

		await test.step('Create a content for the Space', async () => {
			await apiHelpers.objectEntry.postObjectEntry(
				{
					objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
					title: fileTitle,
				},
				'cms/basic-web-contents',
				spaceName
			);
		});

		await test.step('Duplicate content', async () => {
			await assetsPage.gotoAll();

			await assetsPage.execItemAction({
				action: 'Duplicate',
				filter: fileTitle,
				parentAction: 'Copy',
			});

			await expect(
				page.getByRole('link', {
					exact: true,
					name: `${fileTitle} (Copy)`,
				})
			).toBeVisible();

			await expect(
				assetsPage.table.bodyRows
					.filter({
						has: page.getByRole('link', {
							exact: true,
							name: `${fileTitle} (Copy)`,
						}),
					})
					.getByText('Draft')
			).toBeVisible();
		});

		await test.step('Duplicate the original again and check the suffix increments', async () => {
			await assetsPage.execItemAction({
				action: 'Duplicate',
				filter: fileTitle,
				parentAction: 'Copy',
			});

			await expect(
				page.getByRole('link', {
					exact: true,
					name: `${fileTitle} (Copy 1)`,
				})
			).toBeVisible();
		});
	}
);

test(
	'Can view Share modal for added content',
	{tag: '@LPD-62554'},
	async ({apiHelpers, assetsPage}) => {
		const applicationName = 'cms/basic-web-contents';
		const file1Title = `Title ${getRandomString()}`;
		const spaceName = `Space ${getRandomString()}`;
		let objectEntry1;

		await apiHelpers.headlessAssetLibrary.createAssetLibrary({
			name: spaceName,
			settings: {
				logoColor: 'outline-3',
				sharingEnabled: true,
			},
			type: 'Space',
		});

		try {
			objectEntry1 = await apiHelpers.objectEntry.postObjectEntry(
				{
					objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
					title: file1Title,
				},
				applicationName,
				spaceName
			);

			await assetsPage.gotoAll();

			await assetsPage.execItemAction({
				action: 'Share',
				filter: file1Title,
			});

			await expect(assetsPage.modal.title).toContainText(file1Title);
		}
		finally {
			await apiHelpers.objectEntry.deleteObjectEntry(
				applicationName,
				String(objectEntry1.id)
			);
		}
	}
);

test(
	'Dragging and dropping files into the data set opens upload modal',
	{tag: '@LPD-58618'},
	async ({assetsPage, page}) => {
		await assetsPage.gotoAll();

		const dataSetWrapper = page.locator('div.data-set-wrapper').first();
		const dataTransfer = await page.evaluateHandle(
			(data) => {
				const dt = new DataTransfer();

				const file = new File(
					[data.toString('hex')],
					'file_upload_image_1.jpeg',
					{
						type: 'image/jpg',
					}
				);
				dt.items.add(file);

				return dt;
			},
			readFileSync(
				path.join(__dirname, '/dependencies/file_upload_image_1.jpg')
			)
		);

		await dataSetWrapper.dispatchEvent('dragstart', {dataTransfer});
		await dataSetWrapper.dispatchEvent('dragenter', {dataTransfer});
		await dataSetWrapper.dispatchEvent('dragover', {dataTransfer});

		await dataSetWrapper.dispatchEvent('drop', {dataTransfer});
		await dataSetWrapper.dispatchEvent('dragend', {dataTransfer});

		await expect(assetsPage.modal.container).toBeVisible();

		await expect(assetsPage.modal.title).toContainText(
			'Upload Multiple Files'
		);
		await expect(assetsPage.modal.body).toContainText(
			'file_upload_image_1.jpeg'
		);
	}
);

test(
	'Expiration date filter allows future dates',
	{tag: '@LPD-69189'},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const file1Title = `Content ${getRandomString()}`;

		const futureDate = new Date();

		futureDate.setDate(futureDate.getDate() + 1);

		await apiHelpers.objectEntry.postObjectEntry(
			{
				expirationDate: futureDate.toISOString(),
				objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
				title: file1Title,
			},
			applicationName,
			'Default'
		);

		await assetsPage.gotoAll();

		await expect(
			page.getByRole('cell', {exact: true, name: file1Title})
		).toBeVisible();

		// Choose to filter by Expiration Date

		await page.getByRole('button', {name: 'Filter'}).click();

		await page.getByRole('menuitem', {name: 'Expiration Date'}).click();

		const fromDateInput = page.getByLabel('From');
		const toDateInput = page.getByLabel('To', {exact: true});

		// Set future From and To dates covering futureDate

		const fromDate = new Date();
		const toDate = new Date();

		toDate.setDate(toDate.getDate() + 2);

		// Fill in future dates and see that filter label is applied

		await fromDateInput.fill(fromDate.toISOString().split('T')[0]);
		await toDateInput.fill(toDate.toISOString().split('T')[0]);

		await page.getByRole('button', {name: 'Add Filter'}).click();

		await expect(
			page
				.getByRole('button', {name: /Expiration Date:/})
				.locator('.label-section')
		).toBeVisible();

		// Verify that the content is still visible (it was filtered out before the fix)

		await expect(
			page.getByRole('cell', {exact: true, name: file1Title})
		).toBeVisible();
	}
);

test(
	'Content can be filtered by Review Date',
	{tag: '@LPD-85206'},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const file1Title = `Content ${getRandomString()}`;

		const pastDate = new Date();

		pastDate.setDate(pastDate.getDate() - 1);

		await apiHelpers.objectEntry.postObjectEntry(
			{
				objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
				reviewDate: pastDate.toISOString(),
				title: file1Title,
			},
			applicationName,
			'Default'
		);

		await assetsPage.gotoAll();

		await expect(
			page.getByRole('cell', {exact: true, name: file1Title})
		).toBeVisible();

		// Choose to filter by Review Date

		await page.getByRole('button', {name: 'Filter'}).click();

		await page.getByRole('menuitem', {name: 'Review Date'}).click();

		const fromDateInput = page.getByLabel('From');
		const toDateInput = page.getByLabel('To', {exact: true});

		// Set past From and To dates covering pastDate

		const fromDate = new Date();
		const toDate = new Date();

		fromDate.setDate(fromDate.getDate() - 2);

		// Fill in dates and see that filter label is applied

		await fromDateInput.fill(fromDate.toISOString().split('T')[0]);
		await toDateInput.fill(toDate.toISOString().split('T')[0]);

		await page.getByRole('button', {name: 'Add Filter'}).click();

		await expect(
			page
				.getByRole('button', {name: /Review Date:/})
				.locator('.label-section')
		).toBeVisible();

		// Verify that the content is visible

		await expect(
			page.getByRole('cell', {exact: true, name: file1Title})
		).toBeVisible();
	}
);

test(
	'Expiration date filter does not allow "to" date to be before "from" date',
	{tag: '@LPD-78935'},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const fileTitle = `Content ${getRandomString()}`;
		const addFilterButton = page.getByRole('button', {name: 'Add Filter'});
		let objectEntry;

		try {
			objectEntry = await apiHelpers.objectEntry.postObjectEntry(
				{
					objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
					title: fileTitle,
				},
				applicationName,
				'Default'
			);

			await test.step('Go to All section', async () => {
				await assetsPage.gotoAll();
			});

			await test.step('Choose to filter by Expiration Date', async () => {
				await page.getByRole('button', {name: 'Filter'}).click();

				await page
					.getByRole('menuitem', {name: 'Expiration Date'})
					.click();
			});

			const fromDateInput = page.getByLabel('From');
			const toDateInput = page.getByLabel('To', {exact: true});

			const fromDate = new Date();
			const toDate = new Date();

			fromDate.setDate(fromDate.getDate() + 1);

			await test.step('Set "from" date to a future date', async () => {
				await fromDateInput.fill(fromDate.toISOString().split('T')[0]);
			});

			await test.step('Check that the "Add filter" button is disabled if "to" date is before "from date"', async () => {
				await toDateInput.fill(toDate.toISOString().split('T')[0]);
				await expect(addFilterButton).toBeDisabled();
			});

			await test.step('Check that the "Add filter" button is enabled if "to" date is after "from date"', async () => {
				toDate.setDate(toDate.getDate() + 5);
				await toDateInput.fill(toDate.toISOString().split('T')[0]);
				await expect(addFilterButton).toBeEnabled();
			});
		}
		finally {
			if (objectEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry.id)
				);
			}
		}
	}
);

test(
	'FDS Table content disappears after clicking "Show Details" and then "Expire"',
	{tag: '@LPD-69267'},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const file1Title = `Title ${getRandomString()}`;
		const spaceName = 'Default';
		let objectEntry;

		try {
			await test.step('Create an object and go to All section', async () => {
				objectEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: file1Title,
					},
					applicationName,
					spaceName
				);

				await assetsPage.gotoAll();
			});

			await test.step('Select the asset, open the Side Panel and then expire the asset', async () => {
				await assetsPage.execItemAction({
					action: 'Show Details',
					filter: file1Title,
				});

				await expect(
					page.getByRole('heading', {name: file1Title})
				).toBeVisible();

				await page.getByLabel('Close the side panel.').click();

				await assetsPage.execItemAction({
					action: 'Expire',
					filter: file1Title,
				});

				await waitForAlert(page);
			});

			await test.step('Expect that FDS table content is visible', async () => {
				await expect(
					assetsPage
						.getItem(file1Title)
						.getByRole('cell', {name: 'expired'})
				).toBeVisible();

				await expect(
					assetsPage.dataSetFragmentPage.assetLink(file1Title)
				).toBeVisible();
			});
		}
		finally {
			if (objectEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry.id)
				);
			}
		}
	}
);

test(
	'All section places most recently modified content at the top',
	{tag: '@LPD-85725'},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const spaceName = 'Default';
		const thirdTitle = getRandomString();

		await apiHelpers.objectEntry.postObjectEntry(
			{
				objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
				title: getRandomString(),
			},
			applicationName,
			spaceName
		);

		await apiHelpers.objectEntry.postObjectEntry(
			{
				objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
				title: getRandomString(),
			},
			applicationName,
			spaceName
		);

		await apiHelpers.objectEntry.postObjectEntry(
			{
				objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
				title: thirdTitle,
			},
			applicationName,
			spaceName
		);

		await expect(async () => {
			await assetsPage.gotoAll();

			await expect(page.locator('tbody tr').first()).toContainText(
				thirdTitle
			);
		}).toPass();
	}
);

test(
	'Review Date column shows "--" when unset and a date when set',
	{tag: '@LPD-79678'},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const spaceName = 'Default';
		const noReviewDateTitle = getRandomString();
		const reviewDateTitle = getRandomString();

		const toIsoDate = (date: Date) => date.toISOString().slice(0, 10);
		const tomorrow = new Date();
		tomorrow.setDate(tomorrow.getDate() + 1);

		let noReviewEntry;
		let reviewEntry;

		try {
			noReviewEntry = await apiHelpers.objectEntry.postObjectEntry(
				{
					objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
					title: noReviewDateTitle,
				},
				applicationName,
				spaceName
			);

			reviewEntry = await apiHelpers.objectEntry.postObjectEntry(
				{
					objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
					reviewDate: toIsoDate(tomorrow),
					title: reviewDateTitle,
				},
				applicationName,
				spaceName
			);

			await expect(async () => {
				await assetsPage.gotoAll();

				await expect(
					page.getByRole('row').filter({hasText: noReviewDateTitle})
				).toContainText('--');

				await expect(
					page.getByRole('row').filter({hasText: reviewDateTitle})
				).not.toContainText('--');
			}).toPass();
		}
		finally {
			for (const entry of [noReviewEntry, reviewEntry]) {
				if (entry) {
					await apiHelpers.objectEntry.deleteObjectEntry(
						applicationName,
						String(entry.id)
					);
				}
			}
		}
	}
);

test(
	'Content can be filtered by Create Date',
	{tag: ['@LPD-85551', '@LPD-87955']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const fileTitle = `Content ${getRandomString()}`;
		let objectEntry;

		try {
			await test.step('Create a content', async () => {
				objectEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: fileTitle,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: fileTitle})
				).toBeVisible();
			});

			await test.step('Apply Create Date filter', async () => {
				await page.getByRole('button', {name: 'Filter'}).click();

				await page.getByRole('menuitem', {name: 'Create Date'}).click();

				const fromDate = new Date();
				const toDate = new Date();

				fromDate.setDate(fromDate.getDate() - 1);

				await page
					.getByLabel('From')
					.fill(fromDate.toISOString().split('T')[0]);
				await page
					.getByLabel('To', {exact: true})
					.fill(toDate.toISOString().split('T')[0]);

				await page.getByRole('button', {name: 'Add Filter'}).click();
			});

			await test.step('Check filter chip and entry are visible', async () => {
				await expect(
					page
						.getByRole('button', {name: /Create Date:/})
						.locator('.label-section')
				).toBeVisible();

				await expect(
					page.getByRole('cell', {exact: true, name: fileTitle})
				).toBeVisible();
			});
		}
		finally {
			if (objectEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry.id)
				);
			}
		}
	}
);

test(
	'Content can be filtered by Display Date',
	{tag: ['@LPD-85551', '@LPD-87955']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const matchingTitle = `Matching ${getRandomString()}`;
		const otherTitle = `Other ${getRandomString()}`;
		let matchingEntry;
		let otherEntry;

		try {
			await test.step('Create matching and non-matching contents', async () => {
				const matchingDisplayDate = new Date();

				matchingDisplayDate.setDate(matchingDisplayDate.getDate() + 5);

				matchingEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						displayDate: matchingDisplayDate.toISOString(),
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: matchingTitle,
					},
					applicationName,
					'Default'
				);

				otherEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: otherTitle,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {
						exact: true,
						name: matchingTitle,
					})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {
						exact: true,
						name: otherTitle,
					})
				).toBeVisible();
			});

			await test.step('Apply Display Date filter', async () => {
				await page.getByRole('button', {name: 'Filter'}).click();

				await page
					.getByRole('menuitem', {name: 'Display Date'})
					.click();

				const fromDate = new Date();
				const toDate = new Date();

				fromDate.setDate(fromDate.getDate() + 4);
				toDate.setDate(toDate.getDate() + 6);

				await page
					.getByLabel('From')
					.fill(fromDate.toISOString().split('T')[0]);
				await page
					.getByLabel('To', {exact: true})
					.fill(toDate.toISOString().split('T')[0]);

				await page.getByRole('button', {name: 'Add Filter'}).click();
			});

			await test.step('Check only the matching content remains visible', async () => {
				await expect(
					page
						.getByRole('button', {name: /Display Date:/})
						.locator('.label-section')
				).toBeVisible();

				await expect(
					page.getByRole('cell', {
						exact: true,
						name: matchingTitle,
					})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {
						exact: true,
						name: otherTitle,
					})
				).not.toBeVisible();
			});
		}
		finally {
			if (matchingEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(matchingEntry.id)
				);
			}
			if (otherEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(otherEntry.id)
				);
			}
		}
	}
);

test(
	'Content can be filtered by Modified Date',
	{tag: ['@LPD-85551', '@LPD-87955']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const fileTitle = `Content ${getRandomString()}`;
		let objectEntry;

		try {
			await test.step('Create a content', async () => {
				objectEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: fileTitle,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: fileTitle})
				).toBeVisible();
			});

			await test.step('Apply Modified Date filter', async () => {
				await page.getByRole('button', {name: 'Filter'}).click();

				await page
					.getByRole('menuitem', {name: 'Modified Date'})
					.click();

				const fromDate = new Date();
				const toDate = new Date();

				fromDate.setDate(fromDate.getDate() - 1);

				await page
					.getByLabel('From')
					.fill(fromDate.toISOString().split('T')[0]);
				await page
					.getByLabel('To', {exact: true})
					.fill(toDate.toISOString().split('T')[0]);

				await page.getByRole('button', {name: 'Add Filter'}).click();
			});

			await test.step('Check filter chip and entry are visible', async () => {
				await expect(
					page
						.getByRole('button', {name: /Modified Date:/})
						.locator('.label-section')
				).toBeVisible();

				await expect(
					page.getByRole('cell', {exact: true, name: fileTitle})
				).toBeVisible();
			});
		}
		finally {
			if (objectEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry.id)
				);
			}
		}
	}
);

test(
	"Author filter lists only members of the current user's Spaces",
	{tag: '@LPD-70773'},
	async ({apiHelpers, assetsPage, page}) => {
		const space1 = await apiHelpers.headlessAssetLibrary.createAssetLibrary(
			{
				name: `Space ${getRandomString()}`,
				settings: {},
				type: 'Space',
			}
		);

		const space2 = await apiHelpers.headlessAssetLibrary.createAssetLibrary(
			{
				name: `Space ${getRandomString()}`,
				settings: {},
				type: 'Space',
			}
		);

		const viewer = await apiHelpers.headlessAdminUser.postUserAccount();

		userData[viewer.alternateName] = {
			name: viewer.givenName,
			password: 'test',
			surname: viewer.familyName,
		};

		await apiHelpers.headlessAssetLibrary.putAssetLibraryUserAccount(
			space1.externalReferenceCode,
			viewer.externalReferenceCode
		);

		const insider = await apiHelpers.headlessAdminUser.postUserAccount();
		const insiderFullName = `${insider.givenName} ${insider.familyName}`;

		await apiHelpers.headlessAssetLibrary.putAssetLibraryUserAccount(
			space1.externalReferenceCode,
			insider.externalReferenceCode
		);

		const outsider = await apiHelpers.headlessAdminUser.postUserAccount();
		const outsiderFullName = `${outsider.givenName} ${outsider.familyName}`;

		await apiHelpers.headlessAssetLibrary.putAssetLibraryUserAccount(
			space2.externalReferenceCode,
			outsider.externalReferenceCode
		);

		await performUserSwitchViaApi(page, viewer.alternateName);

		await assetsPage.gotoAll();

		await page.getByRole('button', {name: 'Filter'}).click();
		await page.getByRole('menuitem', {name: 'Author'}).click();

		await expect(
			page.getByRole('checkbox', {name: insiderFullName})
		).toBeVisible();
		await expect(
			page.getByRole('checkbox', {name: outsiderFullName})
		).toBeHidden();
	}
);

test(
	"All section lists only content from the current user's Spaces",
	{tag: '@LPD-76453'},
	async ({apiHelpers, assetsPage, page}) => {
		const space1 = await apiHelpers.headlessAssetLibrary.createAssetLibrary(
			{
				name: `Space ${getRandomString()}`,
				settings: {},
				type: 'Space',
			}
		);

		const space2 = await apiHelpers.headlessAssetLibrary.createAssetLibrary(
			{
				name: `Space ${getRandomString()}`,
				settings: {},
				type: 'Space',
			}
		);

		const insideTitle = getRandomString();
		const outsideTitle = getRandomString();

		await apiHelpers.objectEntry.postObjectEntry(
			{
				objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
				title: insideTitle,
			},
			'cms/basic-web-contents',
			space1.name
		);

		await apiHelpers.objectEntry.postObjectEntry(
			{
				objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
				title: outsideTitle,
			},
			'cms/basic-web-contents',
			space2.name
		);

		const viewer = await apiHelpers.headlessAdminUser.postUserAccount();

		userData[viewer.alternateName] = {
			name: viewer.givenName,
			password: 'test',
			surname: viewer.familyName,
		};

		await apiHelpers.headlessAssetLibrary.putAssetLibraryUserAccount(
			space1.externalReferenceCode,
			viewer.externalReferenceCode
		);

		await performUserSwitchViaApi(page, viewer.alternateName);

		await assetsPage.gotoAll();

		await expect(assetsPage.getItem(insideTitle)).toBeVisible();
		await expect(assetsPage.getItem(outsideTitle)).toBeHidden();

		await test.step("Search results are scoped to the viewer's Spaces", async () => {
			await assetsPage.dataSetFragmentPage.search(outsideTitle);

			await expect(assetsPage.getItem(insideTitle)).toBeHidden();
			await expect(assetsPage.getItem(outsideTitle)).toBeHidden();
		});
	}
);

test(
	'Content can be filtered by Category',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const categoryName = `Category ${getRandomString()}`;
		const file1Title = `Categorized ${getRandomString()}`;
		const file2Title = `Uncategorized ${getRandomString()}`;
		const vocabularyName = `Vocabulary ${getRandomString()}`;
		let categoryId: number;
		let objectEntry1: ObjectEntry;
		let objectEntry2: ObjectEntry;
		let vocabularyId: number;

		try {
			await test.step('Create a vocabulary, category, and contents', async () => {
				const siteId = await apiHelpers.headlessAdminUser
					.getSiteByFriendlyUrlPath('cms')
					.then((response) => response.id);

				vocabularyId = await apiHelpers.headlessAdminTaxonomy
					.postSiteTaxonomyVocabulary({
						assetLibraries: [{id: -1}],
						assetTypes: [
							{
								required: false,
								subtype: 'AllAssetSubtypes',
								type: 'AllAssetTypes',
							},
						],
						name: vocabularyName,
						siteId,
						visibilityType: 'PUBLIC',
					})
					.then((response) => response.id);

				categoryId = await apiHelpers.headlessAdminTaxonomy
					.postTaxonomyVocabularyTaxonomyCategory({
						name: categoryName,
						vocabularyId,
					})
					.then((response) => response.id);

				objectEntry1 = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						taxonomyCategoryIds: [categoryId],
						title: file1Title,
					},
					applicationName,
					'Default'
				);

				objectEntry2 = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: file2Title,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: file1Title})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: file2Title})
				).toBeVisible();
			});

			await test.step('Apply Category filter', async () => {
				await page.getByRole('button', {name: 'Filter'}).click();

				await page.getByRole('menuitem', {name: 'Category'}).click();

				await page
					.getByRole('textbox', {name: 'Search'})
					.fill(categoryName);

				await page.getByRole('checkbox', {name: categoryName}).check();

				await page.getByRole('button', {name: 'Add Filter'}).click();
			});

			await test.step('Check only the categorized content is visible', async () => {
				await expect(
					page
						.getByRole('button', {name: /Category:/})
						.locator('.label-section')
				).toBeVisible();

				await expect(
					page.getByRole('cell', {exact: true, name: file1Title})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: file2Title})
				).not.toBeVisible();
			});
		}
		finally {
			if (objectEntry1) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry1.id)
				);
			}
			if (objectEntry2) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry2.id)
				);
			}
			if (vocabularyId) {
				await apiHelpers.headlessAdminTaxonomy.deleteTaxonomyVocabulary(
					vocabularyId
				);
			}
		}
	}
);

test(
	'Content can be filtered by Tag',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const file1Title = `Tagged ${getRandomString()}`;
		const file2Title = `Untagged ${getRandomString()}`;
		const tagName = `Tag${getRandomString()}`;
		let objectEntry1: ObjectEntry;
		let objectEntry2: ObjectEntry;

		try {
			await test.step('Create tagged and untagged contents', async () => {
				objectEntry1 = await apiHelpers.objectEntry.postObjectEntry(
					{
						keywords: [tagName],
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: file1Title,
					},
					applicationName,
					'Default'
				);

				objectEntry2 = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: file2Title,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: file1Title})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: file2Title})
				).toBeVisible();
			});

			await test.step('Apply Tags filter', async () => {
				await page.getByRole('button', {name: 'Filter'}).click();

				await page.getByRole('menuitem', {name: 'Tags'}).click();

				await page.getByRole('textbox', {name: 'Search'}).fill(tagName);

				await page.getByRole('checkbox', {name: tagName}).check();

				await page.getByRole('button', {name: 'Add Filter'}).click();
			});

			await test.step('Check only the tagged content is visible', async () => {
				await expect(
					page
						.getByRole('button', {name: /Tags:/})
						.locator('.label-section')
				).toBeVisible();

				await expect(
					page.getByRole('cell', {exact: true, name: file1Title})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: file2Title})
				).not.toBeVisible();
			});
		}
		finally {
			if (objectEntry1) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry1.id)
				);
			}
			if (objectEntry2) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry2.id)
				);
			}
		}
	}
);

test(
	'Content can be filtered by Space',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const file1Title = `Default ${getRandomString()}`;
		const file2Title = `Other ${getRandomString()}`;
		const otherSpaceName = `Space ${getRandomString()}`;
		let objectEntry1: ObjectEntry;
		let objectEntry2: ObjectEntry;
		let otherSpace;

		try {
			await test.step('Create a second space and contents in each', async () => {
				otherSpace =
					await apiHelpers.headlessAssetLibrary.createAssetLibrary({
						name: otherSpaceName,
						settings: {},
						type: 'Space',
					});

				objectEntry1 = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: file1Title,
					},
					applicationName,
					'Default'
				);

				objectEntry2 = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: file2Title,
					},
					applicationName,
					otherSpaceName
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: file1Title})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: file2Title})
				).toBeVisible();
			});

			await test.step('Apply Space filter for Default', async () => {
				await page.getByRole('button', {name: 'Filter'}).click();

				await page.getByRole('menuitem', {name: 'Space'}).click();

				await page.getByRole('checkbox', {name: 'Default'}).check();

				await page.getByRole('button', {name: 'Add Filter'}).click();
			});

			await test.step('Check only the Default space content is visible', async () => {
				await expect(
					page
						.getByRole('button', {name: /Space:/})
						.locator('.label-section')
				).toBeVisible();

				await expect(
					page.getByRole('cell', {exact: true, name: file1Title})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: file2Title})
				).not.toBeVisible();
			});
		}
		finally {
			if (objectEntry1) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry1.id)
				);
			}
			if (objectEntry2) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry2.id)
				);
			}
			if (otherSpace) {
				await apiHelpers.headlessAssetLibrary.deleteAssetLibrary(
					otherSpace.id
				);
			}
		}
	}
);

test(
	'Content can be filtered by Type',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		const contentApplicationName = 'cms/basic-web-contents';
		const documentApplicationName = 'cms/basic-documents';
		const contentTitle = `Content ${getRandomString()}`;
		const documentTitle = `Document ${getRandomString()}`;
		let contentEntry: ObjectEntry;
		let documentEntry: ObjectEntry;

		try {
			await test.step('Create a content and a document', async () => {
				contentEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: contentTitle,
					},
					contentApplicationName,
					'Default'
				);

				documentEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						file: {
							fileBase64: 'R0lGODlhAQABAAAAACw=',
							name: `file_${getRandomString()}.png`,
						},
						objectEntryFolderExternalReferenceCode: 'L_FILES',
						title: documentTitle,
					},
					documentApplicationName,
					'Default'
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: contentTitle})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: documentTitle})
				).toBeVisible();
			});

			await test.step('Apply Type filter for Basic Web Content', async () => {
				await page.getByRole('button', {name: 'Filter'}).click();

				await page.getByRole('menuitem', {name: 'Type'}).click();

				await page
					.getByRole('checkbox', {name: 'Basic Web Content'})
					.check();

				await page.getByRole('button', {name: 'Add Filter'}).click();
			});

			await test.step('Check only the content row is visible', async () => {
				await expect(
					page
						.getByRole('button', {name: /Type:/})
						.locator('.label-section')
				).toBeVisible();

				await expect(
					page.getByRole('cell', {
						exact: true,
						name: contentTitle,
					})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {
						exact: true,
						name: documentTitle,
					})
				).not.toBeVisible();
			});
		}
		finally {
			if (contentEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					contentApplicationName,
					String(contentEntry.id)
				);
			}
			if (documentEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					documentApplicationName,
					String(documentEntry.id)
				);
			}
		}
	}
);

test(
	'Content can be filtered by Author',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		test.slow();

		const applicationName = 'cms/basic-web-contents';
		const otherFileTitle = `OtherAuthored ${getRandomString()}`;
		const testFileTitle = `TestAuthored ${getRandomString()}`;
		let otherEntry: ObjectEntry;
		let otherUser;
		let testEntry: ObjectEntry;

		try {
			await test.step('Create a second admin user and have them post a content', async () => {
				otherUser =
					await apiHelpers.headlessAdminUser.postUserAccount();

				userData[otherUser.alternateName] = {
					name: otherUser.givenName,
					password: 'test',
					surname: otherUser.familyName,
				};

				const cmsAdminRole =
					await apiHelpers.headlessAdminUser.getRoleByName(
						'CMS Administrator'
					);

				await apiHelpers.headlessAdminUser.postRoleUserAccountAssociation(
					cmsAdminRole.id,
					Number(otherUser.id)
				);

				await performUserSwitchViaApi(page, otherUser.alternateName);

				otherEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: otherFileTitle,
					},
					applicationName,
					'Default'
				);
			});

			await test.step('Switch back to the default user and post their own content', async () => {
				await performUserSwitchViaApi(page, 'test');

				testEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: testFileTitle,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: testFileTitle})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: otherFileTitle})
				).toBeVisible();
			});

			await test.step('Apply Author filter for Test Test', async () => {
				await page.getByRole('button', {name: 'Filter'}).click();

				await page.getByRole('menuitem', {name: 'Author'}).click();

				await page.getByRole('checkbox', {name: 'Test Test'}).check();

				await page.getByRole('button', {name: 'Add Filter'}).click();
			});

			await test.step('Check the filter chip surfaces only Test Test content', async () => {
				await expect(
					page
						.getByRole('button', {name: /Author:/})
						.locator('.label-section')
				).toBeVisible();

				await expect(
					page.getByRole('cell', {exact: true, name: testFileTitle})
				).toBeVisible();

				await expect(
					page.getByRole('cell', {exact: true, name: otherFileTitle})
				).not.toBeVisible();
			});
		}
		finally {
			await performUserSwitchViaApi(page, 'test');

			if (testEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(testEntry.id)
				);
			}
			if (otherEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(otherEntry.id)
				);
			}
			if (otherUser) {
				await apiHelpers.headlessAdminUser.deleteUserAccount(
					otherUser.id
				);
			}
		}
	}
);

test(
	'Content can be filtered by Status',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const token = `Status${getRandomString()}`;

		const future = new Date();
		future.setDate(future.getDate() + 1);

		const entries: {data: DataObject; label: string; title: string}[] = [
			{
				data: {},
				label: 'Approved',
				title: `${token} Approved`,
			},
			{
				data: {status: {code: 2}},
				label: 'Draft',
				title: `${token} Draft`,
			},
			{
				data: {displayDate: future.toISOString()},
				label: 'Scheduled',
				title: `${token} Scheduled`,
			},
		];
		const objectEntries: ObjectEntry[] = [];

		try {
			await test.step('Seed one content per status', async () => {
				for (const entry of entries) {
					objectEntries.push(
						await apiHelpers.objectEntry.postObjectEntry(
							{
								...entry.data,
								objectEntryFolderExternalReferenceCode:
									'L_CONTENTS',
								title: entry.title,
							},
							applicationName,
							'Default'
						)
					);
				}
			});

			for (const entry of entries) {
				await test.step(`Apply Status filter for ${entry.label}`, async () => {
					await assetsPage.gotoAll();

					await page.getByRole('button', {name: 'Filter'}).click();
					await page.getByRole('menuitem', {name: 'Status'}).click();
					await page
						.getByRole('checkbox', {exact: true, name: entry.label})
						.check();
					await page
						.getByRole('button', {name: 'Add Filter'})
						.click();

					await expect(
						page
							.getByRole('button', {name: /Status:/})
							.locator('.label-section')
					).toBeVisible();

					await expect(
						page.getByRole('cell', {
							exact: true,
							name: entry.title,
						})
					).toBeVisible();

					for (const otherEntry of entries) {
						if (otherEntry.label === entry.label) {
							continue;
						}

						await expect(
							page.getByRole('cell', {
								exact: true,
								name: otherEntry.title,
							})
						).not.toBeVisible();
					}
				});
			}
		}
		finally {
			for (const entry of objectEntries) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(entry.id)
				);
			}
		}
	}
);

test(
	'Content can be searched from the All section',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const uniqueToken = getRandomString();
		const file1Title = `Findable ${uniqueToken}`;
		const file2Title = `Other ${getRandomString()}`;
		let objectEntry1: ObjectEntry;
		let objectEntry2: ObjectEntry;

		try {
			await test.step('Create a findable and an unrelated content', async () => {
				objectEntry1 = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: file1Title,
					},
					applicationName,
					'Default'
				);

				objectEntry2 = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: file2Title,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: file1Title})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: file2Title})
				).toBeVisible();
			});

			await test.step('Search for the unique token', async () => {
				const searchInput = page.getByPlaceholder('Search');

				await searchInput.fill(uniqueToken);
				await searchInput.press('Enter');
			});

			await test.step('Check only the matching content is visible', async () => {
				await expect(
					page.getByRole('cell', {exact: true, name: file1Title})
				).toBeVisible();
				await expect(
					page.getByRole('cell', {exact: true, name: file2Title})
				).not.toBeVisible();
			});
		}
		finally {
			if (objectEntry1) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry1.id)
				);
			}
			if (objectEntry2) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry2.id)
				);
			}
		}
	}
);

test(
	'All section pagination caps row count at the selected items-per-page value',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		test.slow();

		const applicationName = 'cms/basic-web-contents';
		const initialSeedCount = 21;
		const token = `Pagination${getRandomString()}`;
		const objectEntries: ObjectEntry[] = [];
		let deltas: number[] = [];

		const seedContent = async (count: number) => {
			for (let i = objectEntries.length; i < count; i++) {
				objectEntries.push(
					await apiHelpers.objectEntry.postObjectEntry(
						{
							objectEntryFolderExternalReferenceCode:
								'L_CONTENTS',
							title: `${token} ${i}`,
						},
						applicationName,
						'Default'
					)
				);
			}
		};

		const searchForToken = async () => {
			const searchInput = page.getByPlaceholder('Search');

			await searchInput.fill('');
			await searchInput.fill(token);
			await searchInput.press('Enter');
		};

		try {
			await test.step(`Seed an initial ${initialSeedCount} contents so the pagination dropdown renders`, async () => {
				await seedContent(initialSeedCount);
			});

			await test.step('Search to scope the listing and read the available items-per-page values', async () => {
				await assetsPage.gotoAll();

				await searchForToken();

				await page.getByLabel('Items Per Page').click();

				const optionLabels = await page
					.getByRole('option')
					.filter({hasText: 'Items'})
					.allInnerTexts();

				deltas = [
					...new Set(
						optionLabels
							.map((label) => Number(label.match(/\d+/)?.[0]))
							.filter((value) => Number.isFinite(value))
					),
				];

				await page.keyboard.press('Escape');

				expect(deltas.length).toBeGreaterThan(0);
			});

			const requiredCount = Math.max(...deltas) + 1;

			if (objectEntries.length < requiredCount) {
				await test.step(`Top up the seed count to ${requiredCount}`, async () => {
					await seedContent(requiredCount);

					await searchForToken();
				});
			}

			for (const delta of deltas) {
				await test.step(`Switch to ${delta} per page and verify the row count caps at ${delta}`, async () => {
					const itemsPerPageToggle =
						page.getByLabel('Items Per Page');

					await itemsPerPageToggle.click();
					await page
						.getByRole('option', {name: `${delta} Items`})
						.click();

					await expect(itemsPerPageToggle).toHaveText(
						new RegExp(`${delta} Items`)
					);
					await expect(assetsPage.table.bodyRows).toHaveCount(delta);
				});
			}
		}
		finally {
			for (const entry of objectEntries) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(entry.id)
				);
			}
		}
	}
);

test(
	'Table view shows the expected columns for an asset',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const fileTitle = `Columns ${getRandomString()}`;
		let objectEntry: ObjectEntry;

		try {
			await test.step('Create a content', async () => {
				objectEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: fileTitle,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();
			});

			await test.step('Check the expected columns are visible', async () => {
				for (const columnName of [
					'Title',
					'Type',
					'Space',
					'Author',
					'Modified',
					'Status',
				]) {
					await expect(
						page.getByRole('columnheader', {name: columnName})
					).toBeVisible();
				}
			});

			await test.step('Check the row exposes type, space, author, and status', async () => {
				const row = assetsPage.table.bodyRows.filter({
					hasText: fileTitle,
				});

				await expect(row).toBeVisible();
				await expect(row).toContainText('Basic Web Content');
				await expect(row).toContainText('Default');
				await expect(row).toContainText('Test Test');
				await expect(row).toContainText('Approved');
			});

			await test.step('Check the row shows structure icon, Space sticker, and Author avatar', async () => {
				const row = assetsPage.table.bodyRows.filter({
					hasText: fileTitle,
				});

				await expect(
					row.getByRole('cell', {name: fileTitle}).locator('.sticker')
				).toBeVisible();
				await expect(
					row.locator('.space-renderer-sticker')
				).toBeVisible();
				await expect(row.locator('.lexicon-icon-user')).toBeVisible();
			});
		}
		finally {
			if (objectEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry.id)
				);
			}
		}
	}
);

test(
	'Table view supports sorting by Modified date',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const sortToken = `Sort${getRandomString()}`;
		const firstTitle = `First ${sortToken}`;
		const secondTitle = `Second ${sortToken}`;
		let firstEntry: ObjectEntry;
		let secondEntry: ObjectEntry;

		try {
			await test.step('Create two contents in order', async () => {
				firstEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: firstTitle,
					},
					applicationName,
					'Default'
				);

				secondEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: secondTitle,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();
			});

			await test.step('Search to scope to the two created contents', async () => {
				const searchInput = page.getByPlaceholder('Search');

				await searchInput.fill(sortToken);
				await searchInput.press('Enter');

				await expect(assetsPage.table.bodyRows).toHaveCount(2);
				await expect(assetsPage.table.bodyRows.first()).toContainText(
					secondTitle
				);
			});

			await test.step('Toggle Modified sort and verify the order flips', async () => {
				await page
					.getByRole('columnheader', {name: 'Modified'})
					.getByRole('button', {name: 'Sortable Column'})
					.click();

				await expect(assetsPage.table.bodyRows.first()).toContainText(
					firstTitle
				);
			});
		}
		finally {
			if (firstEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(firstEntry.id)
				);
			}
			if (secondEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(secondEntry.id)
				);
			}
		}
	}
);

test(
	'Card view shows title, status, modified date, structure icon, and a thumbnail',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		const applicationName = 'cms/basic-web-contents';
		const fileTitle = `Card ${getRandomString()}`;
		let objectEntry: ObjectEntry;

		try {
			await test.step('Create a content', async () => {
				objectEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: fileTitle,
					},
					applicationName,
					'Default'
				);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: fileTitle})
				).toBeVisible();
			});

			await test.step('Switch to Card view', async () => {
				await assetsPage.changeVisualizationMode('Cards');
			});

			await test.step('Check the card shows title, status, modified date, structure icon, and a thumbnail', async () => {
				const card = assetsPage.getCardItem(fileTitle);

				await expect(card).toBeVisible();
				await expect(
					card.getByRole('link', {exact: true, name: fileTitle})
				).toBeVisible();
				await expect(card).toContainText('Approved');
				await expect(card).toContainText(/\w{3} \d{1,2}, \d{4}/);
				await expect(card.locator('.card-item-first')).toBeVisible();
				await expect(card.locator('.sticker-overlay')).toBeVisible();
			});
		}
		finally {
			if (objectEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry.id)
				);
			}
		}
	}
);

test(
	'Space filter shows only spaces the user has access to',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		test.slow();

		const applicationName = 'cms/basic-web-contents';
		const accessibleFileTitle = `Accessible ${getRandomString()}`;
		const accessibleSpaceName = `Accessible ${getRandomString()}`;
		const restrictedFileTitle = `Restricted ${getRandomString()}`;
		const restrictedSpaceName = `Restricted ${getRandomString()}`;
		let accessibleEntry;
		let accessibleSpace;
		let restrictedEntry;
		let restrictedSpace;
		let user;

		try {
			await test.step('Create an accessible and a restricted space with a content in each', async () => {
				accessibleSpace =
					await apiHelpers.headlessAssetLibrary.createAssetLibrary({
						name: accessibleSpaceName,
						settings: {},
						type: 'Space',
					});

				restrictedSpace =
					await apiHelpers.headlessAssetLibrary.createAssetLibrary({
						name: restrictedSpaceName,
						settings: {},
						type: 'Space',
					});

				accessibleEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: accessibleFileTitle,
					},
					applicationName,
					accessibleSpaceName
				);

				restrictedEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: restrictedFileTitle,
					},
					applicationName,
					restrictedSpaceName
				);
			});

			await test.step('Create a user and add only to the accessible space', async () => {
				user = await apiHelpers.headlessAdminUser.postUserAccount();

				userData[user.alternateName] = {
					name: user.givenName,
					password: 'test',
					surname: user.familyName,
				};

				await apiHelpers.headlessAssetLibrary.putAssetLibraryUserAccount(
					accessibleSpace.externalReferenceCode,
					user.externalReferenceCode
				);

				await apiHelpers.headlessAssetLibrary.putAssetLibraryUserAccountRoles(
					accessibleSpace.externalReferenceCode,
					user.externalReferenceCode,
					['Space Content Reviewer']
				);
			});

			await test.step('Log in as the space member and open the Space filter', async () => {
				await performUserSwitchViaApi(page, user.alternateName);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {
						exact: true,
						name: accessibleFileTitle,
					})
				).toBeVisible();

				await page.getByRole('button', {name: 'Filter'}).click();
				await page.getByRole('menuitem', {name: 'Space'}).click();
			});

			await test.step('Verify only the accessible space is listed', async () => {
				await expect(
					page.getByRole('checkbox', {name: accessibleSpaceName})
				).toBeVisible();
				await expect(
					page.getByRole('checkbox', {name: restrictedSpaceName})
				).not.toBeVisible();
			});
		}
		finally {
			await performUserSwitchViaApi(page, 'test');

			if (accessibleEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(accessibleEntry.id)
				);
			}
			if (restrictedEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(restrictedEntry.id)
				);
			}
			if (accessibleSpace) {
				await apiHelpers.headlessAssetLibrary.deleteAssetLibrary(
					accessibleSpace.id
				);
			}
			if (restrictedSpace) {
				await apiHelpers.headlessAssetLibrary.deleteAssetLibrary(
					restrictedSpace.id
				);
			}
		}
	}
);

test(
	'Author filter can be applied by a Space Content Reviewer',
	{tag: ['@LPD-85551', '@LPD-87956']},
	async ({apiHelpers, assetsPage, page}) => {
		test.slow();

		const applicationName = 'cms/basic-web-contents';
		const fileTitle = `Reviewable ${getRandomString()}`;
		const spaceName = `Reviewable ${getRandomString()}`;
		let objectEntry;
		let space;
		let user;

		try {
			await test.step('Create a space, content, and a Space Content Reviewer user', async () => {
				space =
					await apiHelpers.headlessAssetLibrary.createAssetLibrary({
						name: spaceName,
						settings: {},
						type: 'Space',
					});

				objectEntry = await apiHelpers.objectEntry.postObjectEntry(
					{
						objectEntryFolderExternalReferenceCode: 'L_CONTENTS',
						title: fileTitle,
					},
					applicationName,
					spaceName
				);

				user = await apiHelpers.headlessAdminUser.postUserAccount();

				userData[user.alternateName] = {
					name: user.givenName,
					password: 'test',
					surname: user.familyName,
				};

				await apiHelpers.headlessAssetLibrary.putAssetLibraryUserAccount(
					space.externalReferenceCode,
					user.externalReferenceCode
				);

				await apiHelpers.headlessAssetLibrary.putAssetLibraryUserAccountRoles(
					space.externalReferenceCode,
					user.externalReferenceCode,
					['Space Content Reviewer']
				);
			});

			await test.step('Log in as the reviewer and apply the Author filter', async () => {
				await performUserSwitchViaApi(page, user.alternateName);

				await assetsPage.gotoAll();

				await expect(
					page.getByRole('cell', {exact: true, name: fileTitle})
				).toBeVisible();

				await page.getByRole('button', {name: 'Filter'}).click();
				await page.getByRole('menuitem', {name: 'Author'}).click();

				await expect(
					page.getByRole('checkbox', {name: 'Test Test'})
				).toBeVisible();

				await page.getByRole('checkbox', {name: 'Test Test'}).check();

				await page.getByRole('button', {name: 'Add Filter'}).click();
			});

			await test.step('Verify the filter chip and content are visible', async () => {
				await expect(
					page
						.getByRole('button', {name: /Author:/})
						.locator('.label-section')
				).toBeVisible();

				await expect(
					page.getByRole('cell', {exact: true, name: fileTitle})
				).toBeVisible();
			});
		}
		finally {
			await performUserSwitchViaApi(page, 'test');

			if (objectEntry) {
				await apiHelpers.objectEntry.deleteObjectEntry(
					applicationName,
					String(objectEntry.id)
				);
			}
			if (space) {
				await apiHelpers.headlessAssetLibrary.deleteAssetLibrary(
					space.id
				);
			}
		}
	}
);
