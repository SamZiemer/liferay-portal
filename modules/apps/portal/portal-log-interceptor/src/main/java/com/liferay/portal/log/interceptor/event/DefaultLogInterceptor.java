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

package com.liferay.portal.log.interceptor.event;

import com.liferay.portal.kernel.log.LogInterceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

/**
 * @author Jonathan McCann
 * @author Sam Ziemer
 */
@Component(immediate = true, service = LogInterceptor.class)
public class DefaultLogInterceptor implements LogInterceptor {

	public static Map<String, ArrayList<String>> getErrors() {
		return _errors;
	}

	public static List<LogEvent> getEvents() {
		return _logEvents;
	}

	public static Map<String, ArrayList<String>> getWarnings() {
		return _warnings;
	}

	@Override
	public void addError(String className, String error) {
		List<String> errors = _errors.computeIfAbsent(
			className, key -> new ArrayList<>());

		errors.add(error);
	}

	@Override
	public void addEvent(String className, long duration) {
		LogEvent eventInformation = new LogEvent(className, duration);

		_logEvents.add(eventInformation);
	}

	@Override
	public void addWarning(String className, String warning) {
		List<String> warnings = _warnings.computeIfAbsent(
			className, key -> new ArrayList<>());

		warnings.add(warning);
	}

	private static final Map<String, ArrayList<String>> _errors =
		new HashMap<>();
	private static final List<LogEvent> _logEvents = new ArrayList<>();
	private static final Map<String, ArrayList<String>> _warnings =
		new HashMap<>();

}