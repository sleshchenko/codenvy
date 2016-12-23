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
package com.codenvy.organization.api;

import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.google.inject.persist.Transactional;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * Convert personal accounts to personal organizations
 *
 * @author Sergii Leschenko
 */
@Singleton //must be eager
public class PersonalAccountConverter {
    private static final Logger LOG = LoggerFactory.getLogger(PersonalAccountConverter.class);

    //TODO Move personal organizations to saas
    private final Provider<EntityManager> emProvider;
    private final OrganizationManager     organizationManager;
    private final AccountManager          accountManager;

    @Inject
    public PersonalAccountConverter(Provider<EntityManager> emProvider,
                                    OrganizationManager organizationManager,
                                    AccountManager accountManager) {
        this.emProvider = emProvider;
        this.organizationManager = organizationManager;
        this.accountManager = accountManager;
    }

    @PostConstruct
    public void migrate() {
        for (AccountImpl personalAccount : getPersonalAccounts()) {
            convertToPersonalOrganization(personalAccount);
        }
    }

    @Transactional
    protected List<AccountImpl> getPersonalAccounts() {
        return emProvider.get()
                         .createQuery("SELECT a FROM Account a WHERE a.type=:type",
                                      AccountImpl.class)
                         .setParameter("type", "personal")
                         .getResultList();
    }

    @Transactional
    protected void convertToPersonalOrganization(AccountImpl account) {
        try {
            accountManager.remove(account.getId());

            organizationManager.create(new OrganizationImpl(null,
                                                            account.getName(),
                                                            null));

        } catch (ConflictException | ServerException e) {
            LOG.error(String.format("Cannot convert personal account with id '%s' and name '%s' to personal organization",
                                    account.getId(), account.getName()), e);
        }
    }
}
