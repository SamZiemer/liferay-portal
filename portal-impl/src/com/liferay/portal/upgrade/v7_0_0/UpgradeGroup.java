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

package com.liferay.portal.upgrade.v7_0_0;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.model.UserGroupGroupRole;
import com.liferay.portal.kernel.model.UserGroupRole;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.OrganizationLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserGroupGroupRoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserGroupLocalServiceUtil;
import com.liferay.portal.kernel.service.UserGroupRoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.settings.LocalizedValuesMap;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.LoggingTimer;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.language.LanguageResources;
import com.liferay.portal.upgrade.v7_0_0.util.GroupTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author Eudaldo Alonso
 */
public class UpgradeGroup extends UpgradeProcess {

	protected void createIndex() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			runSQL("create index IX_8257E37B on Group_ (classNameId, classPK)");
		}
	}

	@Override
	protected void doUpgrade() throws Exception {
		alter(GroupTable.class, new AlterColumnType("name", "STRING null"));

		createIndex();

		updateGlobalGroupName();

		upgradeStagedGroups();
	}

	protected void updateGlobalGroupName() throws Exception {
		List<Long> companyIds = new ArrayList<>();

		try (PreparedStatement ps = connection.prepareStatement(
				"select companyId from Company")) {

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long companyId = rs.getLong("companyId");

					companyIds.add(companyId);
				}
			}
		}

		for (Long companyId : companyIds) {
			LocalizedValuesMap localizedValuesMap = new LocalizedValuesMap();

			for (Locale locale :
					LanguageUtil.getCompanyAvailableLocales(companyId)) {

				localizedValuesMap.put(
					locale,
					LanguageUtil.get(
						LanguageResources.getResourceBundle(locale), "global"));
			}

			String nameXML = LocalizationUtil.getXml(
				localizedValuesMap, "global");

			try (PreparedStatement ps = connection.prepareStatement(
					"update Group_ set name = ? where companyId = ? and " +
						"friendlyURL = '/global'")) {

				ps.setString(1, nameXML);
				ps.setLong(2, companyId);

				ps.executeUpdate();
			}
		}
	}

	protected void upgradeStagedGroups() throws Exception {
		List<Group> groups = GroupLocalServiceUtil.getLiveGroups();

		for (Group group : groups) {
			if (!group.hasStagingGroup()) {
				continue;
			}

			UnicodeProperties typeSettingsProperties =
				group.getTypeSettingsProperties();

			upgradeStagingTypeSettingsProperties(typeSettingsProperties);

			GroupLocalServiceUtil.updateGroup(
				group.getGroupId(), typeSettingsProperties.toString());

			Group stagingGroup = group.getStagingGroup();

			if (!stagingGroup.isStagedRemotely()) {
				upgradeStagingGroupOrganizationMembership(stagingGroup);
				upgradeStagingGroupRoleMembership(stagingGroup);
				upgradeStagingGroupUserGroupMembership(stagingGroup);
				upgradeStagingGroupUserMembership(stagingGroup);
				upgradeStagingUserGroupRolesAssignments(stagingGroup);
				upgradeStagingUserGroupGroupRolesAssignments(stagingGroup);
			}
		}
	}

	protected void upgradeStagingGroupOrganizationMembership(
		Group stagingGroup) {

		List<Organization> stagingOrganizations =
			OrganizationLocalServiceUtil.getGroupOrganizations(
				stagingGroup.getGroupId());

		if (ListUtil.isEmpty(stagingOrganizations)) {
			return;
		}

		List<Organization> liveOrganizations =
			OrganizationLocalServiceUtil.getGroupOrganizations(
				stagingGroup.getLiveGroupId());

		for (Organization stagingGroupOrganization : stagingOrganizations) {
			if (!liveOrganizations.contains(stagingGroupOrganization)) {
				OrganizationLocalServiceUtil.addGroupOrganization(
					stagingGroup.getLiveGroupId(), stagingGroupOrganization);
			}
		}

		OrganizationLocalServiceUtil.clearGroupOrganizations(
			stagingGroup.getGroupId());
	}

	protected void upgradeStagingGroupRoleMembership(Group stagingGroup) {
		List<Role> stagingRoles = RoleLocalServiceUtil.getGroupRoles(
			stagingGroup.getGroupId());

		if (ListUtil.isEmpty(stagingRoles)) {
			return;
		}

		List<Role> liveRoles = RoleLocalServiceUtil.getGroupRoles(
			stagingGroup.getLiveGroupId());

		for (Role stagingRole : stagingRoles) {
			if (!liveRoles.contains(stagingRole)) {
				RoleLocalServiceUtil.addGroupRole(
					stagingGroup.getLiveGroupId(), stagingRole);
			}
		}

		RoleLocalServiceUtil.clearGroupRoles(stagingGroup.getGroupId());
	}

	protected void upgradeStagingGroupUserGroupMembership(Group stagingGroup) {
		List<UserGroup> stagingUserGroups =
			UserGroupLocalServiceUtil.getGroupUserGroups(
				stagingGroup.getGroupId());

		if (ListUtil.isEmpty(stagingUserGroups)) {
			return;
		}

		List<UserGroup> liveUserGroups =
			UserGroupLocalServiceUtil.getGroupUserGroups(
				stagingGroup.getLiveGroupId());

		for (UserGroup stagingUserGroup : stagingUserGroups) {
			if (!liveUserGroups.contains(stagingUserGroup)) {
				UserGroupLocalServiceUtil.addGroupUserGroup(
					stagingGroup.getLiveGroupId(), stagingUserGroup);
			}
		}

		UserGroupLocalServiceUtil.clearGroupUserGroups(
			stagingGroup.getGroupId());
	}

	protected void upgradeStagingGroupUserMembership(Group stagingGroup) {
		List<User> stagingGroupUsers = UserLocalServiceUtil.getGroupUsers(
			stagingGroup.getGroupId());

		if (ListUtil.isEmpty(stagingGroupUsers)) {
			return;
		}

		List<User> liveGroupUsers = UserLocalServiceUtil.getGroupUsers(
			stagingGroup.getLiveGroupId());

		for (User stagingGroupUser : stagingGroupUsers) {
			if (!liveGroupUsers.contains(stagingGroupUser)) {
				UserLocalServiceUtil.addGroupUser(
					stagingGroup.getLiveGroupId(), stagingGroupUser);
			}
		}

		UserLocalServiceUtil.clearGroupUsers(stagingGroup.getGroupId());
	}

	protected void upgradeStagingTypeSettingsProperties(
		UnicodeProperties typeSettingsProperties) {

		Set<String> keys = typeSettingsProperties.keySet();

		Iterator<String> iterator = keys.iterator();

		while (iterator.hasNext()) {
			String key = iterator.next();

			if (ArrayUtil.contains(
					_LEGACY_STAGED_PORTLET_TYPE_SETTINGS_KEYS, key)) {

				if (_log.isInfoEnabled()) {
					_log.info("Removing type settings property " + key);
				}

				iterator.remove();
			}
		}
	}

	protected void upgradeStagingUserGroupGroupRolesAssignments(
		Group stagingGroup) {

		DynamicQuery dynamicQuery =
			UserGroupGroupRoleLocalServiceUtil.dynamicQuery();

		dynamicQuery.add(
			RestrictionsFactoryUtil.eq(
				"id.groupId", stagingGroup.getGroupId()));

		List<UserGroupGroupRole> stagingUserGroupGroupRoles =
			UserGroupGroupRoleLocalServiceUtil.dynamicQuery(dynamicQuery);

		if (stagingUserGroupGroupRoles.isEmpty()) {
			return;
		}

		dynamicQuery = UserGroupGroupRoleLocalServiceUtil.dynamicQuery();

		dynamicQuery.add(
			RestrictionsFactoryUtil.eq(
				"id.groupId", stagingGroup.getLiveGroupId()));

		List<UserGroupGroupRole> liveUserGroupGroupRoles =
			UserGroupGroupRoleLocalServiceUtil.dynamicQuery(dynamicQuery);

		for (UserGroupGroupRole userGroupGroupRole :
				stagingUserGroupGroupRoles) {

			userGroupGroupRole.setGroupId(stagingGroup.getLiveGroupId());

			if (!liveUserGroupGroupRoles.contains(userGroupGroupRole)) {
				UserGroupGroupRoleLocalServiceUtil.updateUserGroupGroupRole(
					userGroupGroupRole);
			}
		}

		UserGroupGroupRoleLocalServiceUtil.deleteUserGroupGroupRolesByGroupId(
			stagingGroup.getGroupId());
	}

	protected void upgradeStagingUserGroupRolesAssignments(Group stagingGroup) {
		List<UserGroupRole> stagingUserGroupRoles =
			UserGroupRoleLocalServiceUtil.getUserGroupRolesByGroup(
				stagingGroup.getGroupId());

		if (ListUtil.isEmpty(stagingUserGroupRoles)) {
			return;
		}

		List<UserGroupRole> liveUserGroupRoles =
			UserGroupRoleLocalServiceUtil.getUserGroupRolesByGroup(
				stagingGroup.getLiveGroupId());

		for (UserGroupRole stagingUserGroupRole : stagingUserGroupRoles) {
			stagingUserGroupRole.setGroupId(stagingGroup.getLiveGroupId());

			if (!liveUserGroupRoles.contains(stagingUserGroupRole)) {
				UserGroupRoleLocalServiceUtil.updateUserGroupRole(
					stagingUserGroupRole);
			}
		}

		UserGroupRoleLocalServiceUtil.deleteUserGroupRolesByGroupId(
			stagingGroup.getGroupId());
	}

	private static final String[] _LEGACY_STAGED_PORTLET_TYPE_SETTINGS_KEYS = {
		"staged-portlet_39", "staged-portlet_54", "staged-portlet_56",
		"staged-portlet_59", "staged-portlet_107", "staged-portlet_108",
		"staged-portlet_110", "staged-portlet_166", "staged-portlet_169"
	};

	private static final Log _log = LogFactoryUtil.getLog(UpgradeGroup.class);

}