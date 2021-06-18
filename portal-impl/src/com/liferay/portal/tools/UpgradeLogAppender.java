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

package com.liferay.portal.tools;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeReport;
import com.liferay.portal.kernel.util.ServiceProxyFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

import java.io.Serializable;
/**
 * @author Sam Ziemer
 */

public class UpgradeLogAppender implements Appender {

	@Override
	public void append(LogEvent event) {
		String loggerName = event.getLoggerName();

		String message = event.getMessage().getFormattedMessage();

		if (event.getLevel() == Level.ERROR) {
			_upgradeReport.addError(loggerName, message);
		}
		else if (event.getLevel() == Level.WARN) {
			_upgradeReport.addWarning(loggerName, message);
		}
		else if (event.getLevel() == Level.INFO) {
			if (loggerName.equals(UpgradeProcess.class.getName())) {
				_upgradeReport.addEvent(loggerName, message);
			}
		}
	}

	@Override
	public String getName() {
		return "UpgradeLogAppender";
	}

	@Override
	public Layout<? extends Serializable> getLayout() {
		return null;
	}

	public static void close() {
		_upgradeReport.generateReport();
	}

	@Override
	public boolean ignoreExceptions() {
		return false;
	}

	@Override
	public ErrorHandler getHandler() {
		return null;
	}

	@Override
	public void setHandler(ErrorHandler handler) {
		return;
	}

	@Override
	public State getState() {
		return null;
	}

	@Override
	public void initialize() {
		return;
	}

	@Override
	public void start() {
		_started = true;
	}

	@Override
	public void stop() {
		_started = false;
	}

	@Override
	public boolean isStarted() {
		return _started;
	}

	@Override
	public boolean isStopped() {
		return !_started;
	}

	private boolean _started = false;

	private static volatile UpgradeReport _upgradeReport =
		ServiceProxyFactory.newServiceTrackedInstance(UpgradeReport.class,
			UpgradeLogAppender.class, "_upgradeReport", false);
}
