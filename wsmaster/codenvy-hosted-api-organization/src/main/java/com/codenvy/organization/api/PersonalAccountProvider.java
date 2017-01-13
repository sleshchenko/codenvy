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
package com.codenvy.organization.api;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.user.server.event.BeforeUserRemovedEvent;
import org.eclipse.che.api.user.server.event.PostUserPersistedEvent;
import org.eclipse.che.api.user.server.event.PostUserUpdatedEvent;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * //TODO Revise this class
 *
 * @author Sergii Leschenko
 */
@Singleton
public class PersonalAccountProvider {
    private final AccountManager             accountManager;
    private final EventService               eventService;
    private final PersonalAccountCreator     accountCreator;
    private final PersonalAccountNameUpdater accountNameUpdater;
    private final PersonalAccountRemover     accountRemover;

    @Inject
    public PersonalAccountProvider(AccountManager accountManager, EventService eventService) {
        this.accountManager = accountManager;
        this.eventService = eventService;
        this.accountCreator = new PersonalAccountCreator();
        this.accountNameUpdater = new PersonalAccountNameUpdater();
        this.accountRemover = new PersonalAccountRemover();
    }

    @PostConstruct
    public void subscribe() {
        eventService.subscribe(accountCreator, PostUserPersistedEvent.class);
        eventService.subscribe(accountNameUpdater, PostUserUpdatedEvent.class);
        eventService.subscribe(accountRemover, BeforeUserRemovedEvent.class);
    }

    public class PersonalAccountCreator extends CascadeEventSubscriber<PostUserPersistedEvent> {
        @Override
        public void onCascadeEvent(PostUserPersistedEvent event) throws ApiException {
            accountManager.create(new AccountImpl(event.getUser().getId(), event.getUser().getName(), "personal"));
        }
    }

    public class PersonalAccountNameUpdater extends CascadeEventSubscriber<PostUserUpdatedEvent> {
        @Override
        public void onCascadeEvent(PostUserUpdatedEvent event) throws ApiException {
            String oldUsername = event.getOriginalUser().getName();
            String newUserName = event.getUpdatedUser().getName();
            if (!oldUsername.equals(newUserName)) {
                //TODO
//                accountManager.update();
            }
        }
    }

    public class PersonalAccountRemover extends CascadeEventSubscriber<BeforeUserRemovedEvent> {
        @Override
        public void onCascadeEvent(BeforeUserRemovedEvent event) throws ApiException {
            accountManager.remove(event.getUser().getId());
        }
    }
}
