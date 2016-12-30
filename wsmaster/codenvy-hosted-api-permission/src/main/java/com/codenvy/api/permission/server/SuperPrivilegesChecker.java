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
package com.codenvy.api.permission.server;

import com.codenvy.api.permission.shared.model.PermissionsDomain;

import org.eclipse.che.commons.env.EnvironmentContext;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks that current subject has privileges to perform some operation without required permissions.
 *
 * <p>Super privileges is designed to give some extra abilities
 * for users who have permission to perform {@link SystemDomain#MANAGE_SYSTEM_ACTION manage system}.<br>
 * Super privileges are optional, they can be disabled by configuration.
 *
 * <p>User has super privileges if he has {@link SystemDomain#MANAGE_SYSTEM_ACTION manage system} permission and
 * system configuration property {@link #SYSTEM_SUPER_PRIVILEGED_MODE} is true.
 *
 * <p>It is required to perform {@link #hasSuperPrivileges()} checks manually before permissions checking
 * if user should be able to perform some operation.
 * <pre>
 * public class ExamplePermissionsFilter extends CheMethodInvokerFilter {
 *     &#064;Inject
 *     private SuperPrivilegesChecker superPrivilegesChecker;
 *
 *     &#064;Override
 *     protected void filter(GenericResourceMethod genericMethodResource, Object[] arguments) throws ApiException {
 *         if (superPrivilegesChecker.hasSuperPrivileges()) {
 *             return;
 *         }
 *         EnvironmentContext.getCurrent().getSubject().checkPermissions("domain", "domain123", "action");
 *     }
 * }
 * </pre>
 *
 * If user should be able to manage permissions for some permission domain then
 * this domain should be present in multibinder named with {@link #SUPER_PRIVILEGED_DOMAINS}.<br>
 * Binding example:
 * <pre>
 * public class ExampleModule extends AbstractModule {
 *     &#064;Override
 *     protected void configure() {
 *         Multibinder.newSetBinder(binder(), PermissionsDomain.class, Names.named(SuperPrivilegesChecker.SUPER_PRIVILEGED_DOMAINS))
 *                    .addBinding().to(ExampleDomain.class);
 *     }
 * }
 * </pre>
 *
 * @author Sergii Leschenko
 */
public class SuperPrivilegesChecker {
    /**
     * Configuration parameter that indicates extended abilities for users
     * who have {@link SystemDomain#MANAGE_SYSTEM_ACTION manageSytem} permission.
     */
    public static final String SYSTEM_SUPER_PRIVILEGED_MODE = "system.super_privileged_mode";

    /** Permissions of these domains can be managed by any user who has super privileges. */
    public static final String SUPER_PRIVILEGED_DOMAINS = "system.super_privileged_domains";

    private boolean     superPrivilegedMode;
    private Set<String> privilegesDomainsIds;

    @Inject
    public SuperPrivilegesChecker(@Named(SYSTEM_SUPER_PRIVILEGED_MODE) boolean superPrivilegedMode,
                                  @Named(SUPER_PRIVILEGED_DOMAINS) Set<PermissionsDomain> domains) {
        this.superPrivilegedMode = superPrivilegedMode;
        this.privilegesDomainsIds = domains.stream()
                                           .map(PermissionsDomain::getId)
                                           .collect(Collectors.toSet());
    }

    /**
     * Checks that current subject has super privileges.
     *
     * @return true if current subject has super privileges, false otherwise
     */
    public boolean hasSuperPrivileges() {
        return superPrivilegedMode
               && EnvironmentContext.getCurrent().getSubject().hasPermission(SystemDomain.DOMAIN_ID,
                                                                             null,
                                                                             SystemDomain.MANAGE_SYSTEM_ACTION);
    }

    /**
     * Checks that current subject is privileged to manage permissions of specified domain.
     *
     * @return true if current subject is privileged to manage permissions of specified domain, false otherwise
     */
    public boolean isPrivilegedToManagePermissions(String domainId) {
        return superPrivilegedMode
               && privilegesDomainsIds.contains(domainId)
               && EnvironmentContext.getCurrent().getSubject().hasPermission(SystemDomain.DOMAIN_ID,
                                                                             null,
                                                                             SystemDomain.MANAGE_SYSTEM_ACTION);
    }
}
