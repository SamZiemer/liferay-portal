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

package com.liferay.staging.internal.messaging;

import com.liferay.exportimport.kernel.staging.Staging;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.BaseMessageListener;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.SchedulerEntry;
import com.liferay.portal.kernel.scheduler.SchedulerEntryImpl;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.Trigger;
import com.liferay.portal.kernel.scheduler.TriggerFactory;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.util.PropsValues;

import java.net.ConnectException;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Kimberly Chau
 */
@Component(
	immediate = true, property = "destination.name=" + DestinationNames.STAGING,
	service = StagingMessageListener.class
)
public class StagingMessageListener extends BaseMessageListener {

	@Activate
	protected void activate() {
		Class<?> clazz = getClass();

		int connectTimeout = GetterUtil.getInteger(
			PropsValues.TUNNELING_SERVLET_TIMEOUT);

		String className = clazz.getName();

		Trigger trigger = _triggerFactory.createTrigger(
			className, className, null, null, connectTimeout,
			TimeUnit.MILLISECOND);

		SchedulerEntry schedulerEntry = new SchedulerEntryImpl(
			className, trigger);

		_schedulerEngineHelper.register(
			this, schedulerEntry, DestinationNames.STAGING);
	}

	@Deactivate
	protected void deactivate() {
		_schedulerEngineHelper.unregister(this);
	}

	@Override
	protected void doReceive(Message message) throws Exception {
		String cachedRemoteURL;

		Map<String, Object> messageValues = message.getValues();

		if (messageValues.get("group") != null) {
			_group = (Group)messageValues.get("group");

			if (_group.isStaged() || _group.isStagedRemotely()) {
				_typeSettingsProperties = _group.getTypeSettingsProperties();

				_liveGroupId = GetterUtil.getLong(
					_typeSettingsProperties.getProperty("remoteGroupId"));
			}
			else {
				_liveGroupId = 0;
			}
		}

		if (_liveGroupId > 0) {
			if (messageValues.get("layout") != null) {
				_layout = (Layout)messageValues.get("layout");
			}

			if (messageValues.get("user") != null) {
				_user = (User)messageValues.get("user");
			}

			initThreadLocals(_user.getUserId());

			try {
				cachedRemoteURL = _staging.getRemoteSiteURL(
					_group, _layout.isPrivateLayout());
			}
			catch (SystemException se) {
				Throwable cause = se.getCause();

				if (!(cause instanceof ConnectException)) {
					_log.error(se, se);
				}

				cachedRemoteURL = null;
			}

			_typeSettingsProperties.setProperty("remoteURL", cachedRemoteURL);
		}
	}

	protected void initThreadLocals(long userId) throws PortalException {
		User user = _userLocalService.getUserById(userId);

		CompanyThreadLocal.setCompanyId(user.getCompanyId());

		PrincipalThreadLocal.setName(userId);

		PermissionChecker permissionChecker = null;

		try {
			permissionChecker = PermissionCheckerFactoryUtil.create(user);
		}
		catch (Exception e) {
			throw new SystemException(
				"Unable to initialize thread locals because an error occured " +
					"when creating a permission checker for user " + userId,
				e);
		}

		PermissionThreadLocal.setPermissionChecker(permissionChecker);
	}

	@Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED, unbind = "-")
	protected void setModuleServiceLifecycle(
		ModuleServiceLifecycle moduleServiceLifecycle) {
	}

	@Reference(unbind = "-")
	protected void setSchedulerEngineHelper(
		SchedulerEngineHelper schedulerEngineHelper) {

		_schedulerEngineHelper = schedulerEngineHelper;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		StagingMessageListener.class);

	private Group _group;
	private Layout _layout;
	private long _liveGroupId;
	private SchedulerEngineHelper _schedulerEngineHelper;

	@Reference
	private Staging _staging;

	@Reference
	private TriggerFactory _triggerFactory;

	private UnicodeProperties _typeSettingsProperties;
	private User _user;

	@Reference
	private UserLocalService _userLocalService;

}