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
import org.eclipse.che.api.workspace.shared.dto.stack.StackDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Sergii Leschenko
 */
@Singleton
public class StackCreatorPermissionsProvider implements MethodInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(StackCreatorPermissionsProvider.class);

    @Inject
    StackDao stackDao;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object proceed = methodInvocation.proceed();
        try {
            Response response = (Response)proceed;
            StackDto createdStack = (StackDto)response.getEntity();
            try {
                stackDao.storeACL(createdStack.getId(), new AclEntryImpl(createdStack.getCreator(),
                                                                    Stream.of(StackAction.values())
                                                                          .map(StackAction::toString)
                                                                          .collect(Collectors.toList())));
            } catch (ServerException e) {
                LOG.error("Can't add creator's permissions for created stack {}", createdStack.getId());
            }
        } catch (Exception e) {
            LOG.error("Can't add creator's permissions for created stack");
        }
        return proceed;
    }
}
