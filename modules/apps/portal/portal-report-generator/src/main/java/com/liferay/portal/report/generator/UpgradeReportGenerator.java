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
// Portal Report Generator

package com.liferay.portal.report.generator;

import com.liferay.portal.report.generator.event.DefaultReportEventPublisher;
import com.liferay.portal.report.generator.event.ReportEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jonathan McCann
 */
public class UpgradeReportGenerator {

	public static void generateReport() {
		Map<String, ArrayList<String>> errors =
			DefaultReportEventPublisher.getErrors();

		for (Map.Entry<String, ArrayList<String>> entry : errors.entrySet()) {
			System.out.println("entry.getKey() = " + entry.getKey());

			for (String error : entry.getValue()) {
				System.out.println("error = " + error);
			}
		}

		Map<String, HashMap<String, Integer>> classes =
			DefaultReportEventPublisher.getWarnings();

		for (Map.Entry<String, HashMap<String, Integer>> entry : classes.entrySet()) {
			System.out.println("entry.getKey() (className) = " + entry.getKey());

			Map<String, Integer> warnings = entry.getValue();

			for (Map.Entry<String, Integer> warning : warnings.entrySet()) {
				System.out.println("warning.getKey()) (warning message) = " + warning.getKey());
				System.out.println("warning.getValue()) (number of occurrences) = " + warning.getValue());
			}
		}

		List<ReportEvent> upgradeEvents =
			DefaultReportEventPublisher.getEvents();

		Collections.sort(upgradeEvents, _comparator.reversed());

		System.out.println(
			"_upgradeInformations.get(0).getClassName() = " +
				upgradeEvents.get(
					0
				).getClassName());
		System.out.println(
			"_upgradeInformations.get(0).getDuration() = " +
				upgradeEvents.get(
					0
				).getDuration());
		System.out.println();
		System.out.println(
			"_upgradeInformations.get(1).getClassName() = " +
				upgradeEvents.get(
					1
				).getClassName());
		System.out.println(
			"_upgradeInformations.get(1).getDuration() = " +
				upgradeEvents.get(
					1
				).getDuration());
		System.out.println();
		System.out.println(
			"_upgradeInformations.get(2).getClassName() = " +
				upgradeEvents.get(
					2
				).getClassName());
		System.out.println(
			"_upgradeInformations.get(2).getDuration() = " +
				upgradeEvents.get(
					2
				).getDuration());
		System.out.println();
		System.out.println(
			"_upgradeInformations.get(3).getClassName() = " +
				upgradeEvents.get(
					3
				).getClassName());
		System.out.println(
			"_upgradeInformations.get(3).getDuration() = " +
				upgradeEvents.get(
					3
				).getDuration());
		System.out.println();
		System.out.println(
			"_upgradeInformations.get(4).getClassName() = " +
				upgradeEvents.get(
					4
				).getClassName());
		System.out.println(
			"_upgradeInformations.get(4).getDuration() = " +
				upgradeEvents.get(
					4
				).getDuration());
		System.out.println();
	}

	private static final Comparator<ReportEvent> _comparator =
		Comparator.comparingLong(ReportEvent::getDuration);

}