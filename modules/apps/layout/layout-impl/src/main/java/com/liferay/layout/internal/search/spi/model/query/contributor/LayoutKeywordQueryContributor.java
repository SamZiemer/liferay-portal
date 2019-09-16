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

package com.liferay.layout.internal.search.spi.model.query.contributor;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.ParseException;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.search.localization.SearchLocalizationHelper;
import com.liferay.portal.search.query.QueryHelper;
import com.liferay.portal.search.spi.model.query.contributor.KeywordQueryContributor;
import com.liferay.portal.search.spi.model.query.contributor.helper.KeywordQueryContributorHelper;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Vagner B.C
 */
@Component(
	immediate = true,
	property = "indexer.class.name=com.liferay.portal.kernel.model.Layout",
	service = KeywordQueryContributor.class
)
public class LayoutKeywordQueryContributor implements KeywordQueryContributor {

	@Override
	public void contribute(
		String keywords, BooleanQuery booleanQuery,
		KeywordQueryContributorHelper keywordQueryContributorHelper) {

		SearchContext searchContext =
			keywordQueryContributorHelper.getSearchContext();

		String[] fields = {Field.CONTENT, Field.TITLE};

		for (String field : fields) {
			String value = GetterUtil.getString(
				searchContext.getAttribute(field));

			if (Validator.isNull(value)) {
				continue;
			}

			_addLocalizedFields(
				booleanQuery, field, value, false, searchContext);
		}
	}

	@Reference
	protected QueryHelper queryHelper;

	private void _addLocalizedFields(
		BooleanQuery booleanQuery, String fieldName, String value, boolean like,
		SearchContext searchContext) {

		String[] localizedFieldNames =
			_searchLocalizationHelper.getLocalizedFieldNames(
				new String[] {fieldName}, searchContext);

		for (String localizedFieldName : localizedFieldNames) {
			try {
				booleanQuery.addTerm(localizedFieldName, value, like);

				searchContext.setAttribute(localizedFieldName, value);
			}
			catch (ParseException pe) {
				if (_log.isWarnEnabled()) {
					_log.warn("ParseException creating search query", pe);
				}
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		LayoutKeywordQueryContributor.class);

	@Reference
	private SearchLocalizationHelper _searchLocalizationHelper;

}