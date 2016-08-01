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
package com.codenvy.api.promotion.model;

import com.codenvy.api.resources.model.Resource;

import java.util.List;

/**
 * Entity for temporary granting account to use some resources.
 *
 * @author Sergii Leschenko
 */
public interface Promotion {
    /**
     * Returns id of promotion.
     */
    String getId();

    /**
     * Returns id of account owner of promotion.
     */
    String getAccountId();

    /**
     * Returns cause why account was provided with a promotion.
     */
    String getCause();

    /**
     * Returns time when promotion became active.
     */
    long getStartTime();

    /**
     * Returns time when promotion will be/became inactive.
     */
    long getEndTime();

    /**
     * Returns list of resources which can be used by account owner.
     */
    List<? extends Resource> getResources();
}
