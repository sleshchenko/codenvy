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
package com.codenvy.api.license;

import com.codenvy.api.license.model.GrantedResources;

import org.eclipse.che.api.core.ServerException;

import java.util.List;

/**
 * Bridge class that link license and resources granting mechanisms.
 *
 * @author Sergii Leschenko
 */
public interface ResourcesProvider {
    /**
     * Returns list of provided resources for given account
     *
     * @param account
     *         account name
     * @return list of provided resources for given account
     * @throws ServerException
     *         when some exception occurs
     */
    List<GrantedResources> getResources(String account) throws ServerException;
}
