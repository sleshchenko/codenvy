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
package com.codenvy.api.workspace.server.recipe;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.api.core.acl.AclEntryImpl;
import org.eclipse.che.api.machine.server.dao.RecipeDao;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adds acl entry for creator before recipe creation
 *
 * @author Sergii Leschenko
 */
@Singleton
public class RecipeCreatorPermissionsProvider implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        final RecipeImpl recipe = (RecipeImpl)methodInvocation.getArguments()[0];
        final String creator = recipe.getCreator();
        recipe.getAcl().removeIf(aclEntry -> aclEntry.getUser().equals(creator));
        recipe.getAcl().add(new AclEntryImpl(creator,
                                             Stream.of(RecipeAction.values())
                                                   .map(RecipeAction::toString)
                                                   .collect(Collectors.toList())));
        return methodInvocation.proceed();
    }
}
