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
package com.codenvy.api.workspace.server;

import com.codenvy.api.permission.server.PermissionsDomain;
import com.codenvy.api.permission.server.PermissionsImpl;
import com.codenvy.api.workspace.server.dao.WorkerDao;
import com.codenvy.api.workspace.server.model.WorkerImpl;
import com.google.common.collect.ImmutableSet;

import org.eclipse.che.api.core.NotFoundException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.codenvy.api.workspace.server.WorkspaceDomain.DOMAIN_ID;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link WorkspacePermissionStorage}
 *
 * @author Sergii Leschenko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class WorkspacePermissionStorageTest {
    @Mock
    WorkerDao workerDao;

    @InjectMocks
    WorkspacePermissionStorage permissionStorage;

    @Test
    public void shouldBeAbleToStorePermissions() throws Exception {
        permissionStorage.store(new PermissionsImpl("user123",
                                                    "workspace",
                                                    "workspace123",
                                                    Arrays.asList("read", "use")));

        verify(workerDao).store(eq(new WorkerImpl("user123", "workspace123", Arrays.asList(WorkspaceAction.READ,
                                                                                           WorkspaceAction.USE))));
    }
/*
    @Test
    public void shouldBeAbleToGetPermissionsByUser() throws Exception {
        when(workerDao.getWorkersByUser(anyString()))
                .thenReturn(Collections.singletonList(new WorkerImpl("user123", "workspace123", Arrays.asList(WorkspaceAction.READ,
                                                                                                              WorkspaceAction.USE))));

        List<PermissionsImpl> result = permissionStorage.get("user123");

        verify(workerDao).getWorkersByUser(eq("user123"));
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), new PermissionsImpl("user123",
                                                        "workspace",
                                                        "workspace123",
                                                        Arrays.asList("read", "use")));
    }

    @Test
    public void shouldBeAbleToGetPermissionsByUserAndDomain() throws Exception {
        when(workerDao.getWorkersByUser(anyString()))
                .thenReturn(Collections.singletonList(new WorkerImpl("user123", "workspace123", Arrays.asList(WorkspaceAction.READ,
                                                                                                              WorkspaceAction.USE))));

        List<PermissionsImpl> result = permissionStorage.get("user123", DOMAIN_ID);

        assertEquals(result.size(), 1);
        verify(workerDao).getWorkersByUser(eq("user123"));
        assertEquals(result.get(0), new PermissionsImpl("user123",
                                                        "workspace",
                                                        "workspace123",
                                                        Arrays.asList("read", "use")));
    }
*/
    @Test
    public void shouldBeAbleToGetPermissionsByUserAndDomainAndInstance() throws Exception {
        when(workerDao.getWorker(anyString(), anyString()))
                .thenReturn(new WorkerImpl("user123", "workspace123", Arrays.asList(WorkspaceAction.READ,
                                                                                    WorkspaceAction.USE)));

        PermissionsImpl result = permissionStorage.get("user123", DOMAIN_ID, "workspace123");

        verify(workerDao).getWorker(eq("workspace123"), eq("user123"));
        assertEquals(result, new PermissionsImpl("user123",
                                                 "workspace",
                                                 "workspace123",
                                                 Arrays.asList("read", "use")));
    }

    @Test
    public void shouldBeAbleToGetPermissionsByDomainAndInstance() throws Exception {
        when(workerDao.getWorkers(anyString()))
                .thenReturn(Collections.singletonList(new WorkerImpl("user123", "workspace123", Arrays.asList(WorkspaceAction.READ,
                                                                                                              WorkspaceAction.USE))));

        List<PermissionsImpl> result = permissionStorage.getByInstance(DOMAIN_ID, "workspace123");

        assertEquals(result.size(), 1);
        verify(workerDao).getWorkers(eq("workspace123"));
        assertEquals(result.get(0), new PermissionsImpl("user123",
                                                        "workspace",
                                                        "workspace123",
                                                        Arrays.asList("read", "use")));
    }

    @Test
    public void shouldBeAbleToCheckPermissionExistence() throws Exception {
        when(workerDao.getWorker(anyString(), anyString()))
                .thenReturn(new WorkerImpl("user123", "workspace123", Arrays.asList(WorkspaceAction.READ,
                                                                                    WorkspaceAction.USE)));

        boolean existence = permissionStorage.exists("user123", DOMAIN_ID, "workspace123", "read");
        boolean nonExistence = permissionStorage.exists("user123", DOMAIN_ID, "workspace123", "delete");

        assertTrue(existence);
        assertFalse(nonExistence);
    }

    @Test
    public void shouldReturnFalseOnCheckPermissionExistenceWhenWorkerDoesNotExist() throws Exception {
        when(workerDao.getWorker(anyString(), anyString())).thenThrow(new NotFoundException(""));

        boolean result = permissionStorage.exists("user123", DOMAIN_ID, "workspace123", "delete");

        assertFalse(result);
    }

    @Test
    public void shouldBeAbleToRemovePermissions() throws Exception {
        permissionStorage.remove("user123", DOMAIN_ID, "workspace123");

        verify(workerDao).removeWorker(eq("workspace123"), eq("user123"));
    }

    @Test
    public void shouldReturnWorkspaceDomain() {
        Set<PermissionsDomain> supportedDomains = permissionStorage.getDomains();

        assertEquals(supportedDomains, ImmutableSet.of(new WorkspaceDomain()));
    }
}
