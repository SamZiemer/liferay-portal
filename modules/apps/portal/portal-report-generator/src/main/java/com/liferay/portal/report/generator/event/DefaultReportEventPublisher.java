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

package com.liferay.portal.report.generator.event;

import com.liferay.portal.kernel.report.ReportEventPublisher;
import com.liferay.portal.report.generator.UpgradeReportGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

/**
 * @author Jonathan McCann
 */
@Component(immediate = true, service = ReportEventPublisher.class)
public class DefaultReportEventPublisher implements ReportEventPublisher {

	public static Map<String, ArrayList<String>> getErrors() {
		return _errors;
	}

	public static List<ReportEvent> getEvents() {
		return _reportEvents;
	}

	public static Map<String, HashMap<String, Integer>> getWarnings() {
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
		ReportEvent eventInformation = new ReportEvent(className, duration);

		_reportEvents.add(eventInformation);
	}

	@Override
	public void addWarning(String className, String warning) {
		Map<String, Integer> warnings = _warnings.computeIfAbsent(
			className, key -> new HashMap<>());

		warnings.put(warning, warnings.get(warning) + 1);
	}

	@Override
	public void generateUpgradeReport() {
		UpgradeReportGenerator.generateReport();
	}

	private static final Map<String, ArrayList<String>> _errors =
		new HashMap<>();
	private static final List<ReportEvent> _reportEvents = new ArrayList<>();
	private static final Map<String, HashMap<String, Integer>> _warnings =
		new HashMap<>();

}