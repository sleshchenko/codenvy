/*
 *  [2012] - [2017] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.activity.server.inject;

import com.codenvy.activity.server.ActivityPermissionsFilter;
import com.codenvy.activity.server.TimeoutResourceType;
import com.codenvy.activity.server.WorkspaceActivityManager;
import com.codenvy.activity.server.WorkspaceActivityService;
import com.codenvy.resource.model.ResourceType;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class WorkspaceActivityModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivityPermissionsFilter.class);
        bind(WorkspaceActivityService.class);
        bind(WorkspaceActivityManager.class);

        Multibinder.newSetBinder(binder(), ResourceType.class)
                   .addBinding().to(TimeoutResourceType.class);
    }
}
