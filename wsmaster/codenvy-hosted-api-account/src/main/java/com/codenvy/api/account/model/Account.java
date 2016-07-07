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
package com.codenvy.api.account.model;

/**
 * Describes entity that can have resources for using
 *
 * @author Sergii Leschenko
 */
public interface Account {
    enum Type {
        PERSONAL,
        ORGANIZATIONAL
    }

    /**
     * Returns account id
     */
    String getId();

    /**
     * Returns name of account
     *
     * <p>It should be equals to user name if it is {@link Type#PERSONAL} account
     * or it should be equals to organization name if it is {@link Type#ORGANIZATIONAL} account
     */
    String getName();

    /**
     * Returns type of account
     */
    Type getType();
}
