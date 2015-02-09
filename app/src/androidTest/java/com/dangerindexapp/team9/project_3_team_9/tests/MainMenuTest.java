/*
    CSCE 315-501
    Jonathan Sandell, Vyas Sathya, Esteban Solis, Josh Orndorff
    Project 3 - Final submission
    12/10/2014

    Description: Test for application - runs a series of tests to determine if the app is working

    NOTE: Much of the code and comments in the .java files (and others) were auto-generated by Android Studio

*/

package com.dangerindexapp.team9.project_3_team_9.tests;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.EditText;
import com.dangerindexapp.team9.project_3_team_9.XMLManipulator;
import com.dangerindexapp.team9.project_3_team_9.MainMenu;
import com.dangerindexapp.team9.project_3_team_9.XMLObj;
import com.dangerindexapp.team9.project_3_team_9.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainMenuTest extends ActivityInstrumentationTestCase2<MainMenu> {
    public MainMenuTest() {
        super(MainMenu.class);
    }

    // The first iteration of testing will check the navigation to another activity
    // via a button click and the retrieval of GPS coordinates by pressing the button
    private MainMenu testActivity;
    private Button recordsButton;

    @Override
    protected void setUp() throws Exception {
        testActivity = getActivity();
        recordsButton = (Button) testActivity.findViewById(R.id.recordsButton);
    }

    public void testPreconditions() {

        // Wait for GPS to load
        try {
            Thread.sleep(2000, 0);
        }
        catch (Exception exc) {}

        // Test loading GPS coordinates from current location (requires disabling textbox storage)
        testActivity.LoadCoords(null);
        try {
            Thread.sleep(500, 0);
        }
        catch (Exception exc) {}
        assertNotNull(testActivity.currentLocation);

        // Test data loading when no location specified
        testActivity.clearCurrentLocationData();
        testActivity.setLocationString("Texas A&M University");
        testActivity.CalculateClick(null);
        try {
            Thread.sleep(20000, 0);
        }
        catch(Exception e) {}
        assertTrue(testActivity.isDataReady());

        testActivity.fetchNewData();

        // Test creating XML object operations
        String xmlFilePath = "testFile.xml";
        XMLManipulator manipulator = new XMLManipulator(testActivity);
        XMLObj base = new XMLObj("testCase");
        XMLObj e1 = new XMLObj("settings");
        e1.addProperty("Active", new XMLObj("Active", "true"));
        HashMap<String, String> attr = new HashMap<String, String>();
        attr.put("care", "no");
        e1.setAttributes(attr);
        XMLObj e2 = new XMLObj("entries");
        e2.addProperty("point", new XMLObj("point", "1, 2"));
        e2.addProperty("point", new XMLObj("point", "1, 4"));
        e2.addProperty("point", new XMLObj("point", "3, -1"));
        base.addProperty("settings", e1);
        base.addProperty("entries", e2);
        String st = base.toString();
        assertNotNull(st);              // test string form of object

        manipulator.writeXMLFile(base, xmlFilePath);
        File file = testActivity.getFileStreamPath(xmlFilePath);
        XMLObj reload = manipulator.loadXMLData(file);
        assertNotNull(reload);          // test file saving and reloading


        // Test AirNow API data retrieval
        String testUrl = "http://www.airnowapi.org/aq/observation/latLong/current/?format=application/xml&latitude=30.6014&longitude=-96.3144&distance=1000&API_KEY=7CBA9082-79C6-4C11-B7E8-DA8EB71D9606";
        XMLObj obj = manipulator.downloadXMLData(testUrl);
        try {
            Thread.sleep(1000, 0);
        }
        catch (Exception exc) {}
        assertNotNull(obj);                 // test retrieval of data object
        String parName;
        float catNum;
        boolean getElementFailed = false;
        try {
            parName = obj.getElement(1).getString("ParameterName");
            catNum = obj.getElement(1).getFloat("CategoryNumber");
        } catch (Exception e) { getElementFailed = true; }
        assertTrue(!getElementFailed);      // test retrieval of individual data elements

        // Test going to new activity
        ComponentName cNameMain = testActivity.getComponentName();
        testActivity.RecordsButton(recordsButton);
        try {
            Thread.sleep(500, 0);
        }
        catch (Exception exc) {}
        ActivityManager manager = (ActivityManager) testActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runTaskList = manager.getRunningTasks(1);
        ComponentName cNameGraph = runTaskList.get(0).topActivity;
    }
}