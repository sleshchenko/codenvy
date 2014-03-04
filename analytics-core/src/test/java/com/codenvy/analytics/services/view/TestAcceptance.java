/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.services.view;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.Configurator;
import com.codenvy.analytics.Injector;
import com.codenvy.analytics.Utils;
import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.datamodel.StringValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.metrics.Parameters;
import com.codenvy.analytics.persistent.CollectionsManagement;
import com.codenvy.analytics.persistent.JdbcDataPersisterFactory;
import com.codenvy.analytics.pig.PigServer;
import com.codenvy.analytics.services.configuration.XmlConfigurationManager;
import com.codenvy.analytics.services.pig.PigRunner;
import com.codenvy.analytics.services.pig.PigRunnerConfiguration;
import com.google.common.io.ByteStreams;
import com.google.common.io.OutputSupplier;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class TestAcceptance extends BaseTest {

    private StringBuilder builder = new StringBuilder();
    private ViewBuilder viewBuilder;
    private PigRunner   pigRunner;

    private static final String BASE_TEST_RESOURCE_DIR =
            BASE_DIR + "/test-classes/" + TestAcceptance.class.getSimpleName();

    private static final String TEST_VIEW_CONFIGURATION_FILE = BASE_TEST_RESOURCE_DIR + "/view.xml";
    private static final String TEST_STATISTICS_ARCHIVE      =
            TestAcceptance.class.getSimpleName() + "/messages_2013-11-24";

    @BeforeClass
    public void prepare() throws Exception {
        pigRunner = getPigRunner();
        viewBuilder = getViewBuilder(TEST_VIEW_CONFIGURATION_FILE);
        runScript();
    }

    private void runScript() throws Exception {
        Map<String, String> context = Utils.initializeContext(Parameters.TimeUnit.DAY);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        Parameters.LOG.put(context, getResourceAsBytes("2013-11-24", df.format(calendar.getTime())).getAbsolutePath());
        pigRunner.forceExecute(context);
    }

    private File getResourceAsBytes(String originalDate, String newDate) throws Exception {
        String archive = getClass().getClassLoader().getResource(TEST_STATISTICS_ARCHIVE).getFile();


        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry zipEntry = in.getNextEntry();

            try {
                String name = zipEntry.getName();
                File resource = new File(BASE_TEST_RESOURCE_DIR, name);

                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(resource))) {
                    String resourceAsString = new String(ByteStreams.toByteArray(in), "UTF-8");
                    resourceAsString = resourceAsString.replace(originalDate, newDate);

                    ByteStreams.write(resourceAsString.getBytes("UTF-8"), new OutputSupplier<OutputStream>() {
                        @Override
                        public OutputStream getOutput() throws IOException {
                            return out;
                        }
                    });

                    return resource;
                }
            } finally {
                in.closeEntry();
            }
        }
    }

    @Test
    public void test() throws Exception {
        viewBuilder.forceExecute(Utils.initializeContext(Parameters.TimeUnit.DAY));

        ArgumentCaptor<String> viewId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ViewData> viewData = ArgumentCaptor.forClass(ViewData.class);
        ArgumentCaptor<Map> context = ArgumentCaptor.forClass(Map.class);

        verify(viewBuilder, atLeastOnce()).retainViewData(viewId.capture(), viewData.capture(), context.capture());

        for (ViewData actualData : viewData.getAllValues()) {
            System.out.println(viewData.getAllValues().toString());
            for (Map.Entry<String, SectionData> entry : actualData.entrySet()) {
                acceptResult(entry.getKey(), entry.getValue());
            }
        }

        assertEquals(builder.length(), 0, builder.toString());
    }

    private void acceptResult(String tableName, SectionData sectionData) {
        if (tableName.endsWith("day")) {
            switch (tableName) {
                case "invitations_day":
                    assertInvitationsDay(sectionData);
                    break;
                case "time_spent_day":
                    assertTimeSpentDay(sectionData);
                    break;
                case "workspaces_day":
                    assertWorkspacesDay(sectionData);
                    break;
                case "projects_day":
                    assertProjectsDay(sectionData);
                    break;
                case "users_day":
                    assertUsersDay(sectionData);
                    break;
                case "ide_usage_day":
                    assertIdeUsageDay(sectionData);
                    break;
                case "usage_time_day":
                    assertUsageTimeDay(sectionData);
                    break;
                case "workspaces_usage_day":
                    assertWorkspaceUsageDay(sectionData);
                    break;
                case "user_sessions_day":
                    assertUserSessionsDay(sectionData);
                    break;
                case "users_usage_day":
                    assertUsersUsageDay(sectionData);
                    break;
                case "active_users_usage_day":
                    assertActiveUsersUsageDay(sectionData);
                    break;
                case "authentications_day":
                    assertAuthenticationsDay(sectionData);
                    break;
                case "users_engagement_day":
                    assertUsersEngagementDay(sectionData);
                    break;
                case "projects_types_day":
                    assertProjectsTypesDay(sectionData);
                    break;
                case "projects_paas_day":
                    assertProjectsPaasDay(sectionData);
                    break;
                case "factories_day":
                    assertFactoriesDay(sectionData);
                    break;
                case "authenticated_factory_sessions_day":
                    assertAuthenticatedFactorySessionsDay(sectionData);
                    break;
                case "converted_factory_sessions_day":
                    assertConvertedFactorySessionsDay(sectionData);
                    break;
                case "factory_sessions_ide_usage_events_day":
                    assertFactorySessionsIdeUsageEventsDay(sectionData);
                    break;
                case "factory_users_sessions_day":
                    assertFactoryUsersSessionsDay(sectionData);
                    break;
                case "factory_product_usage_day":
                    assertFactoryProductUsageDay(sectionData);
                    break;
                default:
                    break;
            }
        } else if (tableName.endsWith("lifetime")) {
            switch (tableName) {
                case "user_profile_lifetime":
                    assertUsersProfiles(sectionData);
                    break;
            }
        }
    }

    private void assertUsersProfiles(SectionData sectionData) {
        aggregateResult("User's profiles", new StringValueData("Email"), sectionData.get(0).get(0));
        aggregateResult("User's profiles", new StringValueData("First Name"), sectionData.get(0).get(1));
        aggregateResult("User's profiles", new StringValueData("Last Name"), sectionData.get(0).get(2));
        aggregateResult("User's profiles", new StringValueData("Company"), sectionData.get(0).get(3));
        aggregateResult("User's profiles", new StringValueData("Job"), sectionData.get(0).get(4));

        aggregateResult("User's profiles", LongValueData.valueOf(46), LongValueData.valueOf(sectionData.size()));
    }

    private void assertFactoryProductUsageDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Product Usage Mins"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("13:16:33"), sectionData.get(1).get(1));
    }

    private void assertFactoryUsersSessionsDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Factory Sessions"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("130"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("< 10 Mins"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("117"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("> 10 Mins"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("13"), sectionData.get(3).get(1));
    }

    private void assertFactorySessionsIdeUsageEventsDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Factory Sessions"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("130"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("% Built"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("8%"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("% Run"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("13%"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData("% Deployed"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData("6%"), sectionData.get(4).get(1));
    }

    private void assertConvertedFactorySessionsDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Factory Sessions"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("130"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Abandoned Sessions"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("124"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Converted Sessions"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("6"), sectionData.get(3).get(1));
    }

    private void assertAuthenticatedFactorySessionsDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Factory Sessions"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("130"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Anonymous Sessions"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("123"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Authenticated Sessions"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("7"), sectionData.get(3).get(1));
    }

    private void assertFactoriesDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Factories Created"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("7"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Accounts Created From Factories"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("5"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Temporary Workspaces Created"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("110"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData("# with more than one session"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData("8"), sectionData.get(4).get(1));

        row = sectionData.get(5).get(0).getAsString();
        aggregateResult(row, new StringValueData("# with empty sessions"), sectionData.get(5).get(0));
        aggregateResult(row, new StringValueData("41"), sectionData.get(5).get(1));
    }

    private void assertProjectsPaasDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("170"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("AWS"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("AppFog"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("7"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData("CloudBees"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(4).get(1));

        row = sectionData.get(5).get(0).getAsString();
        aggregateResult(row, new StringValueData("CloudFoundry"), sectionData.get(5).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(5).get(1));

        row = sectionData.get(6).get(0).getAsString();
        aggregateResult(row, new StringValueData("GAE"), sectionData.get(6).get(0));
        aggregateResult(row, new StringValueData("2"), sectionData.get(6).get(1));

        row = sectionData.get(7).get(0).getAsString();
        aggregateResult(row, new StringValueData("Heroku"), sectionData.get(7).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(7).get(1));

        row = sectionData.get(8).get(0).getAsString();
        aggregateResult(row, new StringValueData("OpenShift"), sectionData.get(8).get(0));
        aggregateResult(row, new StringValueData("5"), sectionData.get(8).get(1));

        row = sectionData.get(9).get(0).getAsString();
        aggregateResult(row, new StringValueData("Tier3"), sectionData.get(9).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(9).get(1));

        row = sectionData.get(10).get(0).getAsString();
        aggregateResult(row, new StringValueData("Manymo"), sectionData.get(10).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(10).get(1));

        row = sectionData.get(11).get(0).getAsString();
        aggregateResult(row, new StringValueData("No PaaS Defined"), sectionData.get(11).get(0));
        aggregateResult(row, new StringValueData("150"), sectionData.get(11).get(1));
    }

    private void assertProjectsTypesDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("170"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Java Jar"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("14"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Java War"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("2"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData("Java JSP"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData("18"), sectionData.get(4).get(1));

        row = sectionData.get(5).get(0).getAsString();
        aggregateResult(row, new StringValueData("Java Spring"), sectionData.get(5).get(0));
        aggregateResult(row, new StringValueData("2"), sectionData.get(5).get(1));

        row = sectionData.get(6).get(0).getAsString();
        aggregateResult(row, new StringValueData("PHP"), sectionData.get(6).get(0));
        aggregateResult(row, new StringValueData("52"), sectionData.get(6).get(1));

        row = sectionData.get(7).get(0).getAsString();
        aggregateResult(row, new StringValueData("Python"), sectionData.get(7).get(0));
        aggregateResult(row, new StringValueData("13"), sectionData.get(7).get(1));

        row = sectionData.get(8).get(0).getAsString();
        aggregateResult(row, new StringValueData("JavaScript"), sectionData.get(8).get(0));
        aggregateResult(row, new StringValueData("20"), sectionData.get(8).get(1));

        row = sectionData.get(9).get(0).getAsString();
        aggregateResult(row, new StringValueData("Ruby"), sectionData.get(9).get(0));
        aggregateResult(row, new StringValueData("6"), sectionData.get(9).get(1));

        row = sectionData.get(10).get(0).getAsString();
        aggregateResult(row, new StringValueData("Maven Multi Project"), sectionData.get(10).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(10).get(1));

        row = sectionData.get(11).get(0).getAsString();
        aggregateResult(row, new StringValueData("Node.js"), sectionData.get(11).get(0));
        aggregateResult(row, new StringValueData("6"), sectionData.get(11).get(1));

        row = sectionData.get(12).get(0).getAsString();
        aggregateResult(row, new StringValueData("Android"), sectionData.get(12).get(0));
        aggregateResult(row, new StringValueData("34"), sectionData.get(12).get(1));

        row = sectionData.get(13).get(0).getAsString();
        aggregateResult(row, new StringValueData("Django"), sectionData.get(13).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(13).get(1));

        row = sectionData.get(14).get(0).getAsString();
        aggregateResult(row, new StringValueData("Others"), sectionData.get(14).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(14).get(1));
    }

    private void assertUsersEngagementDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("96"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("< 10 Min"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("52"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData(">= 10 And < 60 Mins"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("31"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData(">= 60 And < 300 Mins"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData("12"), sectionData.get(4).get(1));

        row = sectionData.get(5).get(0).getAsString();
        aggregateResult(row, new StringValueData("> 300 Mins"), sectionData.get(5).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(5).get(1));
    }

    private void assertAuthenticationsDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Google Auth"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("69%"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Github Auth"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("11%"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Form Auth"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("20%"), sectionData.get(3).get(1));
    }

    private void assertUserSessionsDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("303"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("<= 1 Min"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("132"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("> 1 And < 10 Mins"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("101"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData(">= 10 And <= 60 Mins"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData("56"), sectionData.get(4).get(1));

        row = sectionData.get(5).get(0).getAsString();
        aggregateResult(row, new StringValueData("> 60 Mins"), sectionData.get(5).get(0));
        aggregateResult(row, new StringValueData("14"), sectionData.get(5).get(1));
    }

    private void assertWorkspaceUsageDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("40"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("New Active Workspaces"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("31"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Returning Active Workspaces"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("195"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData("Non-Active Workspaces"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData(""), sectionData.get(4).get(1));
    }

    private void assertUsageTimeDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("55:32:07"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("<= 1 Min"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("02:12:00"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("> 1 And < 10 Mins"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("06:45:03"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData(">= 10 And <= 60 Mins"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData("21:35:58"), sectionData.get(4).get(1));

        row = sectionData.get(5).get(0).getAsString();
        aggregateResult(row, new StringValueData("> 60 Mins"), sectionData.get(5).get(0));
        aggregateResult(row, new StringValueData("24:59:05"), sectionData.get(5).get(1));
    }

    private void assertIdeUsageDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("# Refactors"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("# Code Completions"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("39"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("# Builds"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("190"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData("# Runs"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData("105"), sectionData.get(4).get(1));

        row = sectionData.get(5).get(0).getAsString();
        aggregateResult(row, new StringValueData("# Debugs"), sectionData.get(5).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(5).get(1));
    }

    private void assertUsersDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total Created"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("31"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Created From Factory"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("5"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Created From Form / oAuth"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("26"), sectionData.get(3).get(1));

        row = sectionData.get(4).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(4).get(0));
        aggregateResult(row, new StringValueData("51"), sectionData.get(4).get(1));
    }

    private void assertUsersUsageDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("51"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Active Users"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("96"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Non-Active Users"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData(""), sectionData.get(3).get(1));
    }

    private void assertActiveUsersUsageDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total Active Users"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("96"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("New Active Users"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("31"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Returning Active Users"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("65"), sectionData.get(3).get(1));
    }

    private void assertProjectsDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Created"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("170"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Destroyed"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("44"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("156"), sectionData.get(3).get(1));
    }

    private void assertWorkspacesDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Created"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("31"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Destroyed"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("1"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Total"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("40"), sectionData.get(3).get(1));
    }

    private void assertTimeSpentDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Builds"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("00:12:29"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Runs"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("02:43:37"), sectionData.get(2).get(1));

        row = sectionData.get(3).get(0).getAsString();
        aggregateResult(row, new StringValueData("Debugs"), sectionData.get(3).get(0));
        aggregateResult(row, new StringValueData("00:02:56"), sectionData.get(3).get(1));
    }

    private void assertInvitationsDay(SectionData sectionData) {
        String row = sectionData.get(1).get(0).getAsString();
        aggregateResult(row, new StringValueData("Sent"), sectionData.get(1).get(0));
        aggregateResult(row, new StringValueData("2"), sectionData.get(1).get(1));

        row = sectionData.get(2).get(0).getAsString();
        aggregateResult(row, new StringValueData("Accepted"), sectionData.get(2).get(0));
        aggregateResult(row, new StringValueData("50%"), sectionData.get(2).get(1));
    }

    private void aggregateResult(String row, ValueData expected, ValueData actual) {
        if (!expected.equals(actual)) {
            builder.append('[');
            builder.append(row);
            builder.append(']');
            builder.append(" expected: ");
            builder.append(expected.getAsString());
            builder.append(" actual: ");
            builder.append(actual.getAsString());
            builder.append('\n');
        }
    }

    /** Creates view builder with test configuration */
    private ViewBuilder getViewBuilder(final String viewConfigurationPath) throws IOException {
        XmlConfigurationManager viewConfigurationManager = mock(XmlConfigurationManager.class);

        when(viewConfigurationManager.loadConfiguration(any(Class.class), anyString())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        XmlConfigurationManager manager = new XmlConfigurationManager();
                        return manager.loadConfiguration(DisplayConfiguration.class, viewConfigurationPath);
                    }
                });

        Configurator viewConfigurator = spy(Injector.getInstance(Configurator.class));
        doReturn(new String[]{viewConfigurationPath}).when(viewConfigurator).getArray(anyString());

        return spy(new ViewBuilder(Injector.getInstance(JdbcDataPersisterFactory.class),
                                   Injector.getInstance(CSVReportPersister.class),
                                   viewConfigurationManager,
                                   viewConfigurator));
    }

    /** Creates pig runner with test configuration */
    private PigRunner getPigRunner(final String scriptConfigurationPath) throws IOException {
        XmlConfigurationManager pigRunnerConfigurationManager = mock(XmlConfigurationManager.class);

        when(pigRunnerConfigurationManager.loadConfiguration(any(Class.class), anyString())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        XmlConfigurationManager manager = new XmlConfigurationManager();
                        return manager.loadConfiguration(PigRunnerConfiguration.class, scriptConfigurationPath);
                    }
                });

        return spy(new PigRunner(Injector.getInstance(CollectionsManagement.class),
                                 pigRunnerConfigurationManager,
                                 Injector.getInstance(PigServer.class)));
    }

    /** Get pig runner with default configuration */
    private PigRunner getPigRunner() {
        return Injector.getInstance(PigRunner.class);
    }
}
