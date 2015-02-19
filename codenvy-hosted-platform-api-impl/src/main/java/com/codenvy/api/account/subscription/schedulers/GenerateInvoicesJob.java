/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.api.account.subscription.schedulers;

import com.codenvy.api.account.billing.BillingPeriod;
import com.codenvy.api.account.billing.BillingService;
import com.codenvy.api.account.billing.Period;
import com.codenvy.api.core.ServerException;
import com.codenvy.commons.schedule.ScheduleCron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Generates invoices for previous billing period
 *
 * @author Sergii Leschenko
 */
@Singleton
public class GenerateInvoicesJob implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GenerateInvoicesJob.class);

    @Inject
    BillingService billingService;

    @Inject
    BillingPeriod billingPeriod;

    // 0sec 0min 07hour 1st day of every month
    @ScheduleCron(cron = "0 0 7 1 * ?")
    @Override
    public void run() {
        final Period previousPeriod = billingPeriod.getCurrent().getPreviousPeriod();

        try {
            billingService.generateInvoices(previousPeriod.getStartDate().getTime(), previousPeriod.getEndDate().getTime());
        } catch (ServerException e) {
            LOG.error("Can't generate invoices", e);
        }
    }
}
