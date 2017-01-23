LPS-23874

edit_permissions.jsp is mostly finished. 2 tabs are implemented, all that needs to be finalized is lines 331-346 which are dependant on the new methods that need to be created in ResourcePermissionUtil and RoleLocalServiceImpl.

ResourcePermissionUtil.getResourcePermissions returns active roles. PrimKey parameter needs to be added to the query. Otherwise the file is finished unless a new finder method should be used instead of the dynamicQuery.

RoleLocalServiceImpl.getGroupRolesAndTeamRolesByRoleId. Returns intersect of getGroupRolesAndTeamRoles and active roles to populate the "Current" tab. Currently using 2 queries and finding intersect, should be modified to use 1 query. Service builder should be used to generate the method throughout the rest of the Role*Service* files.