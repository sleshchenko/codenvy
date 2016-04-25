/*
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.api.workspace.server.stack;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.acl.AclEntryImpl;
import org.eclipse.che.api.workspace.server.spi.StackDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Sergii Leschenko
 */
public class StackPermissionsRemover implements MethodInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(StackCreatorPermissionsProvider.class);

    @Inject
    private StackDao stackDao;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        String stackId = (String)methodInvocation.getArguments()[0];
        Object proceed = methodInvocation.proceed();
        try {
            List<AclEntryImpl> acLs = stackDao.getACLs(stackId);
            for (AclEntryImpl acL : acLs) {
                stackDao.removeACL(stackId, acL.getUser());
            }
        } catch (ServerException e) {
            LOG.error("Can't remove permissions related to stack {} after it removing", stackId);
        }

        return proceed;
    }
}
