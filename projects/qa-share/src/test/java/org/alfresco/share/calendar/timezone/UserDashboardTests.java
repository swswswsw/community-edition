package org.alfresco.share.calendar.timezone;

/**
 * Tests for Calendar->TimeZone->UserDashboard
 * 
 * @author Corina.Nechifor
 */

import java.util.Map;

import org.alfresco.application.windows.MicorsoftOffice2010;
import org.alfresco.po.share.DashBoardPage;
import org.alfresco.po.share.dashlet.MyCalendarDashlet;
import org.alfresco.po.share.enums.Dashlets;
import org.alfresco.po.share.site.SiteDashboardPage;
import org.alfresco.po.share.site.SitePageType;
import org.alfresco.po.share.site.calendar.CalendarPage;
import org.alfresco.test.FailedTestListener;
import org.alfresco.share.util.AbstractUtils;
import org.alfresco.share.util.CalendarUtil;
import org.alfresco.share.util.ShareUser;
import org.alfresco.share.util.ShareUserDashboard;
import org.alfresco.share.util.api.CreateUserAPI;
import org.alfresco.utilities.Application;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.cobra.ldtp.Ldtp;

@Listeners(FailedTestListener.class)
public class UserDashboardTests extends AbstractUtils
{

    private static final Logger logger = Logger.getLogger(UserDashboardTests.class);
    private String testName;
    private String testUser;
    private String siteName;

    private String defaultTZ = "London";
    private String newTZ = "Bucharest";

    MicorsoftOffice2010 outlook = new MicorsoftOffice2010(Application.OUTLOOK, "2010");
    private String sharePointPath;

    @Override
    @BeforeClass(alwaysRun = true)
    public void setup() throws Exception
    {
        super.setup();

        testName = this.getClass().getSimpleName();
        testUser = getUserNameFreeDomain(testName);
        siteName = getSiteName(testName);

        logger.info("Start Tests in: " + testName);

        Runtime.getRuntime().exec("taskkill /F /IM OUTLOOK.EXE");
        sharePointPath = outlook.getSharePointPath();

        CalendarUtil.changeTimeZone(defaultTZ);

    }

    @AfterMethod(alwaysRun = true)
    public void teardownMEthod() throws Exception
    {
        Runtime.getRuntime().exec("taskkill /F /IM OUTLOOK.EXE");
        Runtime.getRuntime().exec("taskkill /F /IM CobraWinLDTP.EXE");
        CalendarUtil.changeTimeZone(defaultTZ);
    }

    @Test(groups = { "DataPrepCalendar" })
    public void dataPrep_AONE_UserDashboard() throws Exception
    {

        // Create normal User
        String[] testUser2 = new String[] { testUser };
        CreateUserAPI.CreateActivateUser(drone, ADMIN_USERNAME, testUser2);

        // login with user
        ShareUser.login(drone, testUser);

        // ShareUser.selectMyDashBoard(drone);
        ShareUserDashboard.addDashlet(drone, Dashlets.MY_CALENDAR);

        // Create public site
        ShareUser.createSite(drone, siteName, AbstractUtils.SITE_VISIBILITY_PUBLIC);

        // Calendar component is added to the site
        ShareUserDashboard.addPageToSite(drone, siteName, SitePageType.CALENDER);

    }

    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_673() throws Exception
    {
        boolean allDay = false;

        ShareUser.login(drone, testUser);

        // Calendar is opened
        SiteDashboardPage siteDashBoard = ShareUser.openSiteDashboard(drone, siteName).render(maxWaitTime);

        // navigate to Calendar
        CalendarPage calendarPage = siteDashBoard.getSiteNav().selectCalendarPage();

        // get startTime and endTime
        Map<String, String> timeValues = CalendarUtil.setTimeForSingleDay("3:40 AM", "7:30 AM", false);
        String expectedStartDate = timeValues.get("startDateValue");
        String expectedEndDate = timeValues.get("endDateValue");

        // set event name
        String event1 = "single_day_event_" + System.currentTimeMillis();

        // Create any single day event, e.g. event1
        calendarPage = calendarPage.createEvent(CalendarPage.ActionEventVia.ADD_EVENT_BUTTON, event1, event1, event1, timeValues.get("startYear"),
                timeValues.get("startMonth"), timeValues.get("startDay"), timeValues.get("startTime"), timeValues.get("endYear"), timeValues.get("endMonth"),
                timeValues.get("endDay"), timeValues.get("endTime"), null, allDay);

        // navigate to MyCalendar
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        String eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getTimeFromDate(expectedEndDate) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

        // logout
        ShareUser.logout(drone);

        // change TimeZone
        CalendarUtil.changeTimeZone(newTZ);

        // login
        ShareUser.login(drone, testUser);

        // navigate to MyCalendar
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        expectedStartDate = CalendarUtil.convertDateToNewTimeZone(expectedStartDate, defaultTZ, newTZ, allDay);
        expectedEndDate = CalendarUtil.convertDateToNewTimeZone(expectedEndDate, defaultTZ, newTZ, allDay);

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getTimeFromDate(expectedEndDate) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

    }

    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_674() throws Exception
    {
        boolean allDay = true;

        ShareUser.login(drone, testUser);

        // Calendar is opened
        SiteDashboardPage siteDashBoard = ShareUser.openSiteDashboard(drone, siteName).render(maxWaitTime);

        // navigate to Calendar
        CalendarPage calendarPage = siteDashBoard.getSiteNav().selectCalendarPage();

        // get startTime and endTime
        Map<String, String> timeValues = CalendarUtil.setTimeForSingleDay("3:00 AM", "7:00 AM", true);
        String expectedStartDate = timeValues.get("startDateValue");

        // set event name
        String event1 = "single_day_allDay_event_" + System.currentTimeMillis();

        // Create any single day event, e.g. event1
        calendarPage = calendarPage.createEvent(CalendarPage.ActionEventVia.ADD_EVENT_BUTTON, event1, event1, event1, timeValues.get("startYear"),
                timeValues.get("startMonth"), timeValues.get("startDay"), timeValues.get("startTime"), timeValues.get("endYear"), timeValues.get("endMonth"),
                timeValues.get("endDay"), timeValues.get("endTime"), null, allDay);

        // navigate to MyCalendar
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        String eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

        // logout
        ShareUser.logout(drone);

        // change TimeZone
        CalendarUtil.changeTimeZone(newTZ);

        // login
        ShareUser.login(drone, testUser);

        // navigate to MyCalendar
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

    }

    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_675() throws Exception
    {
        boolean allDay = false;

        ShareUser.login(drone, testUser);

        // Calendar is opened
        SiteDashboardPage siteDashBoard = ShareUser.openSiteDashboard(drone, siteName).render(maxWaitTime);

        // navigate to Calendar
        CalendarPage calendarPage = siteDashBoard.getSiteNav().selectCalendarPage();

        // get startTime and endTime
        Map<String, String> timeValues = CalendarUtil.addValuesToCurrentDate(0, 2, "7:00 AM", "9:00 AM", allDay);
        String expectedStartDate = timeValues.get("startDateValue");
        String expectedEndDate = timeValues.get("endDateValue");

        // set event name
        String event1 = "multiple_days_event_" + System.currentTimeMillis();

        // Create any single day event, e.g. event1
        calendarPage = calendarPage.createEvent(CalendarPage.ActionEventVia.ADD_EVENT_BUTTON, event1, event1, event1, timeValues.get("startYear"),
                timeValues.get("startMonth"), timeValues.get("startDay"), timeValues.get("startTime"), timeValues.get("endYear"), timeValues.get("endMonth"),
                timeValues.get("endDay"), timeValues.get("endTime"), null, allDay);

        // navigate to MyCalendar
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        String eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy h:mm a", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

        ShareUser.logout(drone);

        CalendarUtil.changeTimeZone(newTZ);

        ShareUser.login(drone, testUser);

        // navigate to MyCalendar
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        expectedStartDate = CalendarUtil.convertDateToNewTimeZone(expectedStartDate, defaultTZ, newTZ, allDay);
        expectedEndDate = CalendarUtil.convertDateToNewTimeZone(expectedEndDate, defaultTZ, newTZ, allDay);

        // create the detail string for new event
        eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy h:mm a", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

    }

    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_676() throws Exception
    {
        boolean allDay = true;

        ShareUser.login(drone, testUser);

        // Calendar is opened
        SiteDashboardPage siteDashBoard = ShareUser.openSiteDashboard(drone, siteName).render(maxWaitTime);

        // navigate to Calendar
        CalendarPage calendarPage = siteDashBoard.getSiteNav().selectCalendarPage();

        // get startTime and endTime
        Map<String, String> timeValues = CalendarUtil.addValuesToCurrentDate(0, 2, "7:00 AM", "9:00 AM", allDay);
        String expectedStartDate = timeValues.get("startDateValue");
        String expectedEndDate = timeValues.get("endDateValue");

        // set event name
        String event1 = "multiple_days_allDay_event_" + System.currentTimeMillis();

        // Create any single day event, e.g. event1
        calendarPage = calendarPage.createEvent(CalendarPage.ActionEventVia.ADD_EVENT_BUTTON, event1, event1, event1, timeValues.get("startYear"),
                timeValues.get("startMonth"), timeValues.get("startDay"), timeValues.get("startTime"), timeValues.get("endYear"), timeValues.get("endMonth"),
                timeValues.get("endDay"), timeValues.get("endTime"), null, allDay);

        // navigate to MyCalendar
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        String eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

        ShareUser.logout(drone);

        CalendarUtil.changeTimeZone(newTZ);

        ShareUser.login(drone, testUser);

        // navigate to MyCalendar
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

    }

    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_677() throws Exception
    {
        boolean allDay = false;

        ShareUser.login(drone, testUser);

        // Calendar is opened
        SiteDashboardPage siteDashBoard = ShareUser.openSiteDashboard(drone, siteName).render(maxWaitTime);

        // navigate to Calendar
        CalendarPage calendarPage = siteDashBoard.getSiteNav().selectCalendarPage();

        // get startTime and endTime
        Map<String, String> timeValues = CalendarUtil.addValuesToCurrentDate(0, 14, "5:15 AM", "8:35 AM", allDay);
        String expectedStartDate = timeValues.get("startDateValue");
        String expectedEndDate = timeValues.get("endDateValue");

        // set event name
        String event1 = "multiple_weeks_event_" + System.currentTimeMillis();

        // Create any single day event, e.g. event1
        calendarPage = calendarPage.createEvent(CalendarPage.ActionEventVia.ADD_EVENT_BUTTON, event1, event1, event1, timeValues.get("startYear"),
                timeValues.get("startMonth"), timeValues.get("startDay"), timeValues.get("startTime"), timeValues.get("endYear"), timeValues.get("endMonth"),
                timeValues.get("endDay"), timeValues.get("endTime"), null, allDay);

        // navigate to MyCalendar
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        String eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy h:mm a", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

        ShareUser.logout(drone);

        CalendarUtil.changeTimeZone(newTZ);

        ShareUser.login(drone, testUser);

        // navigate to MyCalendar
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        expectedStartDate = CalendarUtil.convertDateToNewTimeZone(expectedStartDate, defaultTZ, newTZ, allDay);
        expectedEndDate = CalendarUtil.convertDateToNewTimeZone(expectedEndDate, defaultTZ, newTZ, allDay);

        // create the detail string for new event
        eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy h:mm a", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

    }

    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_678() throws Exception
    {
        boolean allDay = true;

        ShareUser.login(drone, testUser);

        // Calendar is opened
        SiteDashboardPage siteDashBoard = ShareUser.openSiteDashboard(drone, siteName).render(maxWaitTime);

        // navigate to Calendar
        CalendarPage calendarPage = siteDashBoard.getSiteNav().selectCalendarPage();

        // get startTime and endTime
        Map<String, String> timeValues = CalendarUtil.addValuesToCurrentDate(0, 14, "6:00 AM", "10:00 AM", allDay);
        String expectedStartDate = timeValues.get("startDateValue");
        String expectedEndDate = timeValues.get("endDateValue");

        // set event name
        String event1 = "multiple_weeks_allDay_event_" + System.currentTimeMillis();

        // Create any single day event, e.g. event1
        calendarPage = calendarPage.createEvent(CalendarPage.ActionEventVia.ADD_EVENT_BUTTON, event1, event1, event1, timeValues.get("startYear"),
                timeValues.get("startMonth"), timeValues.get("startDay"), timeValues.get("startTime"), timeValues.get("endYear"), timeValues.get("endMonth"),
                timeValues.get("endDay"), timeValues.get("endTime"), null, allDay);

        // navigate to MyCalendar
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        String eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

        ShareUser.logout(drone);

        CalendarUtil.changeTimeZone(newTZ);

        ShareUser.login(drone, testUser);

        // navigate to MyCalendar
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");
    }

    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_679() throws Exception
    {
        boolean allDay = false;

        ShareUser.login(drone, testUser);

        // Calendar is opened
        SiteDashboardPage siteDashBoard = ShareUser.openSiteDashboard(drone, siteName).render(maxWaitTime);

        // navigate to Calendar
        CalendarPage calendarPage = siteDashBoard.getSiteNav().selectCalendarPage();

        // get startTime and endTime
        Map<String, String> timeValues = CalendarUtil.addValuesToCurrentDate(2, 3, "5:30 AM", "8:43 AM", allDay);
        String expectedStartDate = timeValues.get("startDateValue");
        String expectedEndDate = timeValues.get("endDateValue");

        // set event name
        String event1 = "multiple_month_event_" + System.currentTimeMillis();

        // Create any single day event, e.g. event1
        calendarPage = calendarPage.createEvent(CalendarPage.ActionEventVia.ADD_EVENT_BUTTON, event1, event1, event1, timeValues.get("startYear"),
                timeValues.get("startMonth"), timeValues.get("startDay"), timeValues.get("startTime"), timeValues.get("endYear"), timeValues.get("endMonth"),
                timeValues.get("endDay"), timeValues.get("endTime"), null, allDay);

        // navigate to MyCalendar
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        String eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy h:mm a", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

        ShareUser.logout(drone);

        CalendarUtil.changeTimeZone(newTZ);

        ShareUser.login(drone, testUser);

        // navigate to MyCalendar
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        expectedStartDate = CalendarUtil.convertDateToNewTimeZone(expectedStartDate, defaultTZ, newTZ, allDay);
        expectedEndDate = CalendarUtil.convertDateToNewTimeZone(expectedEndDate, defaultTZ, newTZ, allDay);

        // create the detail string for new event
        eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy h:mm a", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

    }

    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_680() throws Exception
    {
        boolean allDay = true;

        ShareUser.login(drone, testUser);

        // Calendar is opened
        SiteDashboardPage siteDashBoard = ShareUser.openSiteDashboard(drone, siteName).render(maxWaitTime);

        // navigate to Calendar
        CalendarPage calendarPage = siteDashBoard.getSiteNav().selectCalendarPage();

        // get startTime and endTime
        Map<String, String> timeValues = CalendarUtil.addValuesToCurrentDate(2, 0, "6:00 AM", "10:00 AM", allDay);
        String expectedStartDate = timeValues.get("startDateValue");
        String expectedEndDate = timeValues.get("endDateValue");

        // set event name
        String event1 = "multiple_months_allDay_event_" + System.currentTimeMillis();

        // Create any single day event, e.g. event1
        calendarPage = calendarPage.createEvent(CalendarPage.ActionEventVia.ADD_EVENT_BUTTON, event1, event1, event1, timeValues.get("startYear"),
                timeValues.get("startMonth"), timeValues.get("startDay"), timeValues.get("startTime"), timeValues.get("endYear"), timeValues.get("endMonth"),
                timeValues.get("endDay"), timeValues.get("endTime"), null, allDay);

        // navigate to MyCalendar
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        String eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

        ShareUser.logout(drone);

        CalendarUtil.changeTimeZone(newTZ);

        ShareUser.login(drone, testUser);

        // navigate to MyCalendar
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");
    }

    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_681() throws Exception
    {
        boolean allDay = false;

        ShareUser.login(drone, testUser);

        // Calendar is opened
        SiteDashboardPage siteDashBoard = ShareUser.openSiteDashboard(drone, siteName).render(maxWaitTime);

        // navigate to Calendar
        CalendarPage calendarPage = siteDashBoard.getSiteNav().selectCalendarPage();

        // get startTime and endTime
        Map<String, String> timeValues = CalendarUtil.addValuesToCurrentDate(0, 1, "2:30 PM", "1:00 AM", allDay);
        String expectedStartDate = timeValues.get("startDateValue");
        String expectedEndDate = timeValues.get("endDateValue");

        // set event name
        String event1 = "multiple_days_specific_event" + System.currentTimeMillis();

        // Create any single day event, e.g. event1
        calendarPage = calendarPage.createEvent(CalendarPage.ActionEventVia.ADD_EVENT_BUTTON, event1, event1, event1, timeValues.get("startYear"),
                timeValues.get("startMonth"), timeValues.get("startDay"), timeValues.get("startTime"), timeValues.get("endYear"), timeValues.get("endMonth"),
                timeValues.get("endDay"), timeValues.get("endTime"), null, allDay);

        // navigate to MyCalendar
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        String eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy h:mm a", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

        // logout
        ShareUser.logout(drone);

        // change TimeZone
        CalendarUtil.changeTimeZone(newTZ);

        // login
        ShareUser.login(drone, testUser);

        // navigate to MyCalendar
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        expectedStartDate = CalendarUtil.convertDateToNewTimeZone(expectedStartDate, defaultTZ, newTZ, allDay);
        expectedEndDate = CalendarUtil.convertDateToNewTimeZone(expectedEndDate, defaultTZ, newTZ, allDay);

        // verify the new event is displayed
        Assert.assertTrue(myCalendar.isEventsDisplayed(event1), "The " + event1 + " isn't correctly displayed on my calendar");

        // create the detail string for new event
        eventDetail = event1 + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "dd MMMM, yyyy h:mm a", allDay) + "\n" + siteName;

        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

    }

    /** AONE-682:User Dashboard. Calendar dashlet. Recurrent */
    @Test(groups = { "Calendar", "EnterpriseOnly" })
    public void AONE_682() throws Exception
    {
        boolean allDay = false;
        String location = testName + " - Room";
        String startDate = "2:30 PM";
        String endDate = "5:25 PM";

         // MS Outlook 2010 is opened;
         Ldtp l = outlook.openOfficeApplication();
        
         // create new meeting workspace
         outlook.operateOnCreateNewMeetingWorkspace(l, sharePointPath, siteName, location, testUser, DEFAULT_PASSWORD, true, false);
        
         // Step 1
         // Create any recurrent event, e.g.:
         //
         // Name: test-event;
         // Start Date: 28/06/2013 14:30;
         // End Date: 28/06/2013 17:25;
         // Recurrence: Daily, Every 1 day;
         // End after: 3 occurences.
         Ldtp l1 = outlook.getAbstractUtil().setOnWindow(siteName);
         l1.click("chkAlldayevent");
         l1 = outlook.getAbstractUtil().setOnWindow(siteName);
         l1.mouseLeftClick("btnRecurrence");
         // set the recurrence
         outlook.operateOnRecurrenceAppointment(l1, startDate, endDate, "3");
        
         Ldtp after_rec = outlook.getAbstractUtil().setOnWindow(siteName);
         after_rec.click("btnSave");
        
         // User login.
        ShareUser.login(drone, testUser);

        // Step 2
        // Open User Dashboard
        // User Dashboard is opened
        DashBoardPage userDashboard = ShareUser.selectMyDashBoard(drone);
        MyCalendarDashlet myCalendar = userDashboard.getDashlet("my-calendar").render();

        // Step 3
        // Verify Calendar dashlet.
        Map<String, String> timeValues = CalendarUtil.addValuesToCurrentDate(0, 0, startDate, endDate, allDay);
        String expectedStartDate = timeValues.get("startDateValue");
        String expectedEndDate = timeValues.get("endDateValue");

        String eventHeader = siteName + " (Repeating)";
        String eventDetail = eventHeader + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "h:mm a", allDay) + "\n" + siteName;
        //
        // // verify the event with the details was created
        // Assert.assertTrue(myCalendar.isRepeating(siteName), "The " + siteName + " is not a repeating event");
        // Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");
        //
        // ShareUser.logout(drone);
        //
        // // Step 4
        // // On a server machine, open Site Dashboard - instead of accessing a server machine, the date time of the current machine is changed
        CalendarUtil.changeTimeZone(newTZ);

        ShareUser.login(drone, testUser);

        // Verify User Dashboard is opened
        userDashboard = ShareUser.selectMyDashBoard(drone);
        myCalendar = userDashboard.getDashlet("my-calendar").render();

        expectedStartDate = CalendarUtil.convertDateToNewTimeZone(expectedStartDate, defaultTZ, newTZ, allDay);
        expectedEndDate = CalendarUtil.convertDateToNewTimeZone(expectedEndDate, defaultTZ, newTZ, allDay);

        eventDetail = eventHeader + "\n" + CalendarUtil.getDateInFormat(expectedStartDate, "dd MMMM, yyyy h:mm a", allDay) + " - "
                + CalendarUtil.getDateInFormat(expectedEndDate, "h:mm a", allDay) + "\n" + siteName;

        // Step 5
        // Verify Calendar dashlet.
        // verify the event with the details was created
        Assert.assertTrue(myCalendar.isRepeating(siteName), "The " + siteName + " is not a repeating event");
        Assert.assertTrue(myCalendar.isEventDetailsDisplayed(eventDetail), "The " + eventDetail + " isn't correctly displayed on my calendar");

    }

}
