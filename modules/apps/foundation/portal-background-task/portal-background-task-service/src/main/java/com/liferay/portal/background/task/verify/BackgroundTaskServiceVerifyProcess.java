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

package com.liferay.portal.background.task.verify;

import com.liferay.portal.background.task.model.BackgroundTask;
import com.liferay.portal.background.task.service.BackgroundTaskLocalService;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskConstants;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.verify.VerifyProcess;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alec Shay
 */
@Component(
    immediate = true,
    property = {"verify.process.name=com.liferay.background.task.service"},
    service = VerifyProcess.class
)
public class BackgroundTaskServiceVerifyProcess extends VerifyProcess {

    @Override
    protected void doVerify() throws Exception {
        deleteBackgroundTasks();
    }

    protected void deleteBackgroundTasks() {
        try {
            if (_log.isInfoEnabled()) {
                _log.info("Deleting unnecessary backgroundtask entries");
            }

            java.util.List<BackgroundTask> tasksToRemove =
                _backgroundTaskLocalService.getBackgroundTasks(
                    "com.liferay.portal.lar.backgroundtask." +
                        "StagingIndexingBackgroundTaskExecutor",
                    BackgroundTaskConstants.STATUS_SUCCESSFUL);

            if (tasksToRemove != null) {
                for (int i = 0; i < tasksToRemove.size(); i++) {
                    BackgroundTask backgroundTask = tasksToRemove.get(i);

                    _backgroundTaskLocalService.deleteBackgroundTask(
                        backgroundTask);
                }

                if (_log.isInfoEnabled()) {
                    _log.info("Deleted StagingIndexingBackgroundTaskExecutors");
                }
            }
        }
        catch (Exception e) {
            _log.error(e, e);
        }
    }

    @Reference(unbind = "-")
    protected void setBackgroundTaskLocalService(
        BackgroundTaskLocalService backgroundTaskLocalService) {

        _backgroundTaskLocalService = backgroundTaskLocalService;
    }

    private static final Log _log = LogFactoryUtil.getLog(
        BackgroundTaskServiceVerifyProcess.class);

    private BackgroundTaskLocalService _backgroundTaskLocalService;
}