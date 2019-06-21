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
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.BaseMessageListener;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.model.Group;
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
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.util.PropsValues;

import java.net.ConnectException;

import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Kimberly Chau
 */
@Component(immediate = true, service = StagingMessageListener.class)
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
			this, schedulerEntry, DestinationNames.SCHEDULER_DISPATCH);
	}

	@Deactivate
	protected void deactivate() {
		_schedulerEngineHelper.unregister(this);
	}

	@Override
	protected void doReceive(Message message) throws Exception {
		DynamicQuery groupDynamicQuery = _groupLocalService.dynamicQuery();

		List<Group> groups = _groupLocalService.dynamicQuery(groupDynamicQuery);

		for (Group group : groups) {
			if (group.isStagedRemotely()) {
				UnicodeProperties typeSettingsProperties =
					group.getTypeSettingsProperties();

				long creatorUserId = group.getCreatorUserId();

				if (group.hasPrivateLayouts()) {
					_cacheRemoteURL = setRemoteSiteURL(
						group, true, creatorUserId);
				}
				else {
					_cacheRemoteURL = setRemoteSiteURL(
						group, false, creatorUserId);
				}

				typeSettingsProperties.setProperty(
					"remoteURL", _cacheRemoteURL);

				_groupLocalService.updateGroup(
					group.getGroupId(), typeSettingsProperties.toString());
			}
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

	@Reference(unbind = "-")
	protected void setGroupLocalService(GroupLocalService groupLocalService) {
		_groupLocalService = groupLocalService;
	}

	@Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED, unbind = "-")
	protected void setModuleServiceLifecycle(
		ModuleServiceLifecycle moduleServiceLifecycle) {
	}

	protected String setRemoteSiteURL(
			Group group, boolean privateLayout, long userId)
		throws PortalException {

		initThreadLocals(userId);

		try {
			_cacheRemoteURL = _staging.getRemoteSiteURL(group, privateLayout);
		}
		catch (SystemException se) {
			Throwable cause = se.getCause();

			if (!(cause instanceof ConnectException)) {
				_log.error(se, se);
			}

			_cacheRemoteURL = null;
		}

		return _cacheRemoteURL;
	}

	@Reference(unbind = "-")
	protected void setSchedulerEngineHelper(
		SchedulerEngineHelper schedulerEngineHelper) {

		_schedulerEngineHelper = schedulerEngineHelper;
	}

	@Reference(unbind = "-")
	protected void setUserLocalService(UserLocalService userLocalService) {
		_userLocalService = userLocalService;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		StagingMessageListener.class);

	private String _cacheRemoteURL;
	private GroupLocalService _groupLocalService;
	private SchedulerEngineHelper _schedulerEngineHelper;

	@Reference
	private Staging _staging;

	@Reference
	private TriggerFactory _triggerFactory;

	private UserLocalService _userLocalService;

}