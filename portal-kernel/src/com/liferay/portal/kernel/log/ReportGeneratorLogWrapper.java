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

package com.liferay.portal.kernel.log;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.SystemProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sam Ziemer
 */
public class ReportGeneratorLogWrapper extends LogWrapper {

	public static Map<String, HashMap<String, Integer>> getWarnings() {
		return _warnings;
	}

	public static boolean isEnabled() {
		return _GENERATE_REPORT;
	}

	public ReportGeneratorLogWrapper(Log log, String name) {
		super(log);

		_className = name;

		setLogWrapperClassName(ReportGeneratorLogWrapper.class.getName());
	}

	public void addWarning(Object msg, Throwable throwable) {
		String warning = null;

		if (throwable == null) {
			if (msg != null) {
				warning = (String)msg;
			}
			else {
				return;
			}
		}
		else if (msg == null) {
			warning = throwable.getMessage();
		}

		Map<String, Integer> warnings = _warnings.computeIfAbsent(
			_className, key -> new HashMap<>());

		warnings.put(warning, warnings.getOrDefault(warning, 0) + 1);
	}

	@Override
	public void warn(Object msg) {
		super.warn(msg);

		addWarning(msg, null);
	}

	@Override
	public void warn(Object msg, Throwable throwable) {
		super.warn(msg, throwable);

		addWarning(msg, throwable);
	}

	@Override
	public void warn(Throwable throwable) {
		super.warn(throwable);

		addWarning(null, throwable);
	}

	private static final boolean _GENERATE_REPORT = GetterUtil.getBoolean(
		SystemProperties.get(PropsKeys.GENERATE_REPORT));

	private static final Map<String, HashMap<String, Integer>> _warnings =
		new HashMap<>();

	private final String _className;

}