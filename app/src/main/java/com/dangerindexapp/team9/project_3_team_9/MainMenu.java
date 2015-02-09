/*
    CSCE 315-501
    Jonathan Sandell, Vyas Sathya, Esteban Solis, Josh Orndorff
    Project 3 - Final submission
    12/10/2014

    Description: The main activity managing the swipeable screen fragments for details, main
    menu, and settings.

    Code for GPS access came from the Android developers website at this URL:
    http://developer.android.com/guide/topics/location/strategies.html

    NOTE: Much of the code and comments in the .java files (and others) were auto-generated by Android Studio

    NOTE: APIs used:
    AirNow API: http://www.airnowapi.org/
    National Weather Service Data: http://graphical.weather.gov/
    (see strings.xml for exact URLs)

*/

package com.dangerindexapp.team9.project_3_team_9;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.util.Xml;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlSerializer;

import java.io.FileWriter;
import java.util.Date;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;


public class MainMenu extends Activity implements SettingsFragment.OnFragmentInteractionListener, MainDisplayFragment.OnFragmentInteractionListener, DetailsFragment.OnFragmentInteractionListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    public Location currentLocation = null;     // stored current location
    LocationManager locationManager = null;     // location manager object
    Address currentAddress = null;              // stored current address
    HashMap<String, Float> individualValues;    // Dictionary to store individual retreived values
    boolean valuesReady = false;                // if data values are ready
    boolean missingData = false;                // if some data was not found for the current location
    float dangerIndex = 0;                      // the last read danger index
    String locationString = "";                 // the string last put in the textbox
    String addressName = "";                    // a string representation of the current address
    XMLManipulator manipulator;                 // manipulator object
    DetailsFragment detailsFragment;            // reference to details fragment
    String xmlFile;                             // the name of the data file stored
    boolean saveValue = true;                   // whether the value should be logged for a given call


    Float probTempMax  = null;
    Float probTempMin = null;

    // Internal settings
    boolean enableGPS = false;
    boolean enableLogging = false;

    boolean bullseyePressed = true;    // if a call is being made for the current location

    // Return if at least one value from the data retrieval is ready
    public boolean isDataReady() {
        return valuesReady;
    }

    // Convert boolean to "true" or "false"
    public String boolToStr(boolean b) { return b ? "true" : "false";}

    // Convert "true" to true, to false otherwise
    public boolean strToBool(String str) { return str.equals("true");}

    // Display a short toast message
    public void showToastMessage(String str) {
        Toast toast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
        toast.show();
    }

    // Checks to see if a user data file is present and, if not, creates an empty one
    public void ensureDataFilePresent() {
        File file = getApplicationContext().getFileStreamPath(xmlFile);
        XMLObj readFile = manipulator.loadXMLData(file);
        if (readFile == null) { // no file - create empty one
            XMLObj obj = new XMLObj("UserData");
            XMLObj gpsobj = new XMLObj("EnableGPS", "false");
            XMLObj logobj = new XMLObj("EnableLogging", "false");
            XMLObj lastlocal = new XMLObj("LastLocalPull");
            XMLObj records = new XMLObj("RecordsData");
            obj.addProperty("EnableGPS", gpsobj);
            obj.addProperty("EnableLogging", logobj);
            obj.addProperty("LastLocalPull", lastlocal);
            obj.addProperty("RecordsData", records);
            manipulator.writeXMLFile(obj, xmlFile);
        }
    }

    // A function to generate sample record data so the Graph functionality can be tested/demonstrated
    // without having to spend several days accumulating data
    public void generateSampleDataToTestGraph() {
        ensureDataFilePresent();
        ClearLog(null);
        XMLObj dataFile = getDataFile();
        XMLObj r1 = new XMLObj("value", "9.7");
        XMLObj r2 = new XMLObj("value", "8.6");
        XMLObj r3 = new XMLObj("value", "8.4");
        XMLObj r4 = new XMLObj("value", "8.7");
        XMLObj r5 = new XMLObj("value", "2.6");
        XMLObj r6 = new XMLObj("value", "2.3");
        XMLObj r7 = new XMLObj("value", "1.2");
        XMLObj r8 = new XMLObj("value", "1.9");
        XMLObj records = dataFile.getXMLObj("RecordsData");
        records.addProperty("value", r1);
        records.addProperty("value", r2);
        records.addProperty("value", r3);
        records.addProperty("value", r4);
        records.addProperty("value", r5);
        records.addProperty("value", r6);
        records.addProperty("value", r7);
        records.addProperty("value", r8);
        saveDataFile(dataFile);
    }

    // Load an XMLObj representation of the user data file (creating one if it doesn't exist)
    public XMLObj getDataFile() {
        ensureDataFilePresent();
        File file = getApplicationContext().getFileStreamPath(xmlFile);
        return manipulator.loadXMLData(file);
    }

    // Save the XMLObj as the user data file
    public void saveDataFile(XMLObj file) {
        manipulator.writeXMLFile(file, xmlFile);
    }

    // Update the settings in the user data file to reflect the current settings
    public void updateSettings() {
        XMLObj data = getDataFile();
        data.getXMLObj("EnableGPS").setSingletonValue(boolToStr(enableGPS));
        data.getXMLObj("EnableLogging").setSingletonValue(boolToStr(enableLogging));
        saveDataFile(data);
    }

    // Toggle GPS enabled
    public void ClickGPSEnable(View view) {
        CheckBox box = (CheckBox) view;
        enableGPS = box.isChecked();
        updateSettings();
    }

    // Toggle logging enabled
    public void ClickLoggingEnable(View view) {
        CheckBox box = (CheckBox) view;
        enableLogging = box.isChecked();
        updateSettings();
    }

    // Clear internally stored location data
    public void clearCurrentLocationData() {
        locationString = "";
        currentLocation = null;
    }

    // Set the location string
    public void setLocationString(String str) {
        locationString = str;
    }

    // No implementation
    public void onFragmentInteraction(Uri uri) {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        individualValues = new HashMap<String, Float>();    // initialize HashMap

        // Set up location management
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {}
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(1);   // jump to middle fragment at beginning

        manipulator = new XMLManipulator(getApplicationContext());  // make XMLManipulator
        xmlFile = getString(R.string.dataFileName);                 // set xmlFile name
        ensureDataFilePresent();
        //generateSampleDataToTestGraph();      // enable for testing purposes
        XMLObj file = getDataFile();                                // load settings from file
        enableGPS = strToBool(file.getXMLObj("EnableGPS").getSingletonValue());
        enableLogging = strToBool(file.getXMLObj("EnableLogging").getSingletonValue());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        TextView display = (TextView) findViewById(R.id.dangerIndexValue);  // reset danger index
        display.setText(String.valueOf(dangerIndex));                       // when re-oriented
    }

    // Options menu not currently used
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return new SettingsFragment();      // first fragment is settings
                case 1:
                    return new MainDisplayFragment();   // second is main menu
                case 2:
                    detailsFragment = new DetailsFragment();
                    return detailsFragment;             // third is details
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return "Settings";
                case 1:
                    return "Danger Index";
                case 2:
                    return "Details";
            }
            return null;
        }
    }

    // Open records view when button is pressed
    public void RecordsButton(View view){
        Intent intent = new Intent(view.getContext(), MyGraph.class );
        startActivity(intent);
    }



    // Calculate danger index when button is pressed
    public void CalculateClick(View view){
        EditText textarea = (EditText) findViewById(R.id.locationBox);
        // if textbox has been changed manually since last calculation or coord loading
        if (!locationString.equals(textarea.getText().toString()) && !textarea.getText().toString().equals("")) {
            clearCurrentLocationData();     // trigger geocoding
            bullseyePressed = false;        // from a manually entered location
            locationString = textarea.getText().toString();
        }

        Time lastTime = getLastLocalPullTime();
        if (lastTime == null) {fetchNewData(); return;}
        Time nowTime = new Time();
        nowTime.setToNow();

        saveValue = true;   // determine whether to save new value
        if(!(Time.getJulianDay(lastTime.toMillis(true), 0) - Time.getJulianDay(nowTime.toMillis(true), 0) > 0))
            saveValue = false;
            //if the julian days are the same, do not update log file

        fetchNewData();
    }

    // Load GPS coordinates
    public void LoadCoords(View view) {

        bullseyePressed = true;
        if (!enableGPS) {   // if user has locked GPS, show a toast and quit
            showToastMessage("App GPS retrieval deactivated - please activate in settings");
            return;
        }

        // get location from GPS
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        // note - any code placing values into textboxes must be commented out for testing
        if (currentLocation != null) {  // if a location was found
            EditText textarea = (EditText) findViewById(R.id.locationBox);
            double LATITUDE = currentLocation.getLatitude();    // get coords
            double LONGITUDE = currentLocation.getLongitude();
            Geocoder geocoder = new Geocoder(this,Locale.ENGLISH);
            try {   // get address from lon,lat with Geocoder

                List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);

                if (addresses != null) {    // if an address was found
                    Address returnedAddress = addresses.get(0);
                    currentAddress = returnedAddress;               // store current address
                    addressName = "";
                    for (int i = 0; i < currentAddress.getMaxAddressLineIndex(); i++) {    // build human-readable address string
                        addressName += currentAddress.getAddressLine(i) + "\n";
                    }
                    textarea.setText(addressName);
                    locationString = addressName;
                }
                else{                   // no addresses found
                    textarea.setText("" + LATITUDE + "," + LONGITUDE);
                    addressName = "" + LATITUDE + "," + LONGITUDE;
                    locationString = addressName;
                }
            } catch (IOException e) {   // couldn't retrieve reverse geocoded address
                e.printStackTrace();
                textarea.setText("Failed to retrieve address!");
            }
        }
        else {                          // couldn't retrieve location
            showToastMessage("No location retrieved!");
        }
    }

    // Remove all danger index records from the user data file
    public void ClearLog(View view) {
        ensureDataFilePresent();
        XMLObj dataFile = getDataFile();
        XMLObj newDataFile = new XMLObj("UserData");        // copy all properties by RecordsData
        newDataFile.addProperty("EnableGPS", dataFile.getXMLObj("EnableGPS"));
        newDataFile.addProperty("EnableLogging", dataFile.getXMLObj("EnableLogging"));
        newDataFile.addProperty("LastLocalPull", dataFile.getXMLObj("LastLocalPull"));
        XMLObj emptyRecords = new XMLObj("RecordsData");    // add new empty RecordsData entry
        newDataFile.addProperty("RecordsData", emptyRecords);
        saveDataFile(newDataFile);
    }

    // Returns the number of minutes until another local pull can be made (-1 means ready, 0 means less than a minute)
    public int minTilNextLocalPull() {
        Time lastTime = getLastLocalPullTime();
        if (lastTime == null) return -1;
        Time nowTime = new Time();
        nowTime.setToNow();
        float elapsedMillis = nowTime.toMillis(false) - lastTime.toMillis(false);
        if (elapsedMillis > 3600000) return -1;
        return (int) (3600000 - elapsedMillis) / 60000;
    }

    // Retrieve Time object representing last time local data was pulled (read from user data file)
    public Time getLastLocalPullTime() {
        ensureDataFilePresent();
        XMLObj records = getDataFile();
        Time lastTime = new Time();
        try {                               // read from LastLocalPull from user data file
            lastTime.parse3339(records.getXMLObj("LastLocalPull").getSingletonValue());
        } catch (Exception e) {
            lastTime = null;
        }
        return lastTime;
    }

    // Use the current location information to construct a call to the data retriever
    public void fetchNewData() {
        if (bullseyePressed && currentLocation != null) {  // if a local pull, ensure enough time has elapsed - otherwise,
            int minsLeft = minTilNextLocalPull();   // print appropriate error message and quit
            if (minsLeft != -1) {
                if (minsLeft == 0)
                    showToastMessage("It has been less than an hour since you last pulled local data - new data may be pulled again in less than a minute");
                else
                    showToastMessage("It has been less than an hour since you last pulled local data - new data may be pulled again in about " + minsLeft + " minutes");
                return;
            }
        }

        if (currentLocation == null) {  // if there's no current location, Geocode a location from the locationString
            Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
            try {
                currentLocation = new Location(LocationManager.NETWORK_PROVIDER);
                List<Address> addresses = geocoder.getFromLocationName(locationString, 1);
                Address loc = addresses.get(0);
                addressName = "";
                for (int i = 0; i < loc.getMaxAddressLineIndex(); i++) {    // build human-readable address string
                    addressName += loc.getAddressLine(i) + "\n";
                }
                currentAddress = loc;   // get location from address
                currentLocation.setLatitude(loc.getLatitude());
                currentLocation.setLongitude(loc.getLongitude());
            } catch (Exception e) { // if no location found
                showToastMessage("Couldn't find a location by that name! Apologies!");
                return;
            }
        }
        ArrayList<String> urlList = new ArrayList<String>();    // construct call to data retriever
        String w_url = fillURL(getString(R.string.weather_url));
        String aq_url = fillURL(getString(R.string.air_quality_url));
        urlList.add(w_url);
        urlList.add(aq_url);

        if (bullseyePressed) {  // log the new last local pull time for local coords
            Time time = new Time();
            time.setToNow();
            XMLObj dataFile = getDataFile();
            dataFile.getXMLObj("LastLocalPull").setSingletonValue(time.format3339(false));
            saveDataFile(dataFile);
        }
        new DataRetriever().execute(urlList);   // make data retrieval call
    }

    // Replace placeholders in URLs with meaningful location data and fix encoding
    private String fillURL(String url) {
        String r_url = url.replace("$amp;", "&").replace("INSERT_LON", String.valueOf(currentLocation.getLongitude())).replace("INSERT_LAT", String.valueOf(currentLocation.getLatitude()));
        r_url = r_url.replace("%%", "%");
        return r_url;
    }

    // Use the current individual values to construct and display the danger index
    public void calculateDangerIndex() {
        float index = 0;
        missingData = false;
        // add scaled values if present, noting absence if needed

        try {   // try to add value for air quality
            if(individualValues.get("airQuality") != -999)
                index += individualValues.get("airQuality")/7;
            else missingData = true;
        } catch (Exception e) { missingData = true; }

        try {   // try to add value for thunderstorms
            if(individualValues.get("thunderstorm") != -999)
                index += individualValues.get("thunderstorm")/100;
            else missingData = true;
        } catch (Exception e) { missingData = true; }

        try {   // try to add value for hail
            if(individualValues.get("hail") != -999)
                index += individualValues.get("hail")/100;
            else missingData = true;
        } catch (Exception e) { missingData = true; }

        try {   // try to add value for tornado
            if(individualValues.get("tornado") != -999)
                index += individualValues.get("tornado")/25;
            else missingData = true;
        } catch (Exception e) { missingData = true; }

        index += (float) NormalizedTemp(probTempMin,probTempMax);   // add values for temperatures
        if (probTempMin.equals(-999f) || probTempMax.equals(-999f)) missingData = true; // missing data if either not there

        if (index < 0) index = 0;   // ensure danger index in range
        if (index > 10) index = 10;

        dangerIndex = index;
        TextView text = (TextView) findViewById(R.id.dangerIndexValue); // display index and address
        text.setText(String.format("%.2f", dangerIndex));

        showToastMessage("Found! " + addressName);  // play ding when finished
        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(),R.raw.ding_sound);
        mediaPlayer.start();
        if (missingData) {  // notify user if some data missing
            showToastMessage("Note: some data not found - danger index may be inaccurate");
        }
        detailsFragment.ReloadListView();
        if (enableLogging && bullseyePressed && saveValue)  // save value if needed
            DataPlacement(index);
        bullseyePressed = false;
        final RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.relativeLayout);
        int dangerIndexI = (int) dangerIndex;   // update display
        TextView tv = (TextView) findViewById(R.id.dangerIndexMessage);
        tv.setText(getResources().getStringArray(R.array.messages)[dangerIndexI]);
        relativeLayout.setBackgroundColor(Color.parseColor(getResources().getStringArray(R.array.colors)[dangerIndexI]));
    }

    // Log a danger index value in the XML storage
    public void DataPlacement(float val){
        XMLObj readFile = getDataFile();
        if (readFile != null) {
            XMLObj newEntry = new XMLObj("value",String.valueOf(val));
            readFile.getXMLObj("RecordsData").addProperty("value", newEntry);
            saveDataFile(readFile);
        }
    }

    // Pressing the info button
    public void InfoButton(View view){
        Intent intent = new Intent(view.getContext(), InfoActivity.class );
        startActivity(intent);
    }
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_menu, container, false);
            return rootView;
        }

    }

    // Async Task to fetch data from the URLs
    class DataRetriever extends AsyncTask<ArrayList<String>, Integer, ArrayList<XMLObj>> {

        private ArrayList<String> requestURLs;
        private ArrayList<XMLObj> returnObjs;
        private boolean done;
        private Context context;

        protected ArrayList<XMLObj> doInBackground(ArrayList<String>...args) {
            ArrayList<String> requestURLs = args[0];
            ArrayList<XMLObj> returnObjs = new ArrayList<XMLObj>();
            XMLManipulator manipulator = new XMLManipulator(context);
            for (int i = 0; i < requestURLs.size(); i++) {  // iterate through URLs and retrieve data
                XMLObj obj = manipulator.downloadXMLData(requestURLs.get(i));
                returnObjs.add(obj);
            }
            return returnObjs;
        }

        protected void onPostExecute(ArrayList<XMLObj> result) {
            extractValues(result);
        }
    }

    // Handle temperature values, returning total contribution to danger index
    public float NormalizedTemp(Float min, Float max){
        Float normalizedValue;
        Float maxTemp = max;
        Float minTemp = min;
        int idealTemp = 72;             // ideal temperature (72 degrees ought to be good)
        int maxDiffCare = 30;           // max difference we care about
        Double contributingValue = 1.5; // amount each difference contributes to danger index

        float maxDiff = Math.abs(idealTemp - maxTemp);
        float minDiff = Math.abs(idealTemp - minTemp);

        float adjustedMinDiff = Math.min(minDiff,maxDiffCare)/maxDiffCare;
        float adjustedMaxDiff = Math.min(maxDiff,maxDiffCare)/maxDiffCare;

        individualValues.put("NormalizedMinTemp", adjustedMinDiff);
        individualValues.put("NormalizedMaxTemp", adjustedMaxDiff);
        individualValues.put("MinTemp", minTemp);
        individualValues.put("MaxTemp", maxTemp);

        return (float) (adjustedMinDiff*contributingValue + adjustedMaxDiff*contributingValue);
    }

    // Extract individual values from results and store in HashMap
    private void extractValues(ArrayList<XMLObj> results) {
        individualValues.clear();
        probTempMax = -999f;    // initially assume values not retrieved
        probTempMin = -999f;


        if (results.size() == 0) return;
        if (results.get(0) != null) {   // load in whatever values available from weather data
            XMLObj weatherObj = results.get(0);
            Float probTornado, probHail, probThunder, temp;

            Float TempValue;
            try {   // to get chance of tornados
                probTornado = new Float(weatherObj.getElement(1).getXMLObj("parameters").getElement(15).getElement(0).getFloat("value"));
                if(!weatherObj.getElement(1).getXMLObj("parameters").getElement(15).getElement(0).getString("name").equals("Probability of Tornadoes"))
                {
                    probTornado = -999f;
                    showToastMessage("Could not retrieve tornado data");
                    //show toast
                }
                individualValues.put("tornado", probTornado);
            } catch (Exception e) {}
            try {   // to get chance of hail
                probHail = new Float(weatherObj.getElement(1).getXMLObj("parameters").getElement(16).getElement(0).getFloat("value"));
                if(!weatherObj.getElement(1).getXMLObj("parameters").getElement(16).getElement(0).getString("name").equals("Probability of Hail"))
                {
                    probHail = -999f;
                    showToastMessage("Could not retrieve hail data");
                    //show toast
                }
                individualValues.put("hail", probHail);
            } catch (Exception e) {}
            try {   // to get total chance of severe thunderstorms
                probThunder = new Float(weatherObj.getElement(1).getXMLObj("parameters").getElement(21).getElement(0).getFloat("value"));
                if(!weatherObj.getElement(1).getXMLObj("parameters").getElement(21).getElement(0).getString("name").equals("Total Probability of Severe Thunderstorms"))
                {
                    probThunder = -999f;
                    showToastMessage("Could not retrieve thunderstorm data");
                    //show toast
                }
                individualValues.put("thunderstorm", probThunder);
            } catch (Exception e) {}
            try{    // temperature values
                probTempMax = new Float(weatherObj.getElement(1).getXMLObj("parameters").getElement(0).getElement(1).getSingletonValue());
            }catch(Exception e) {}
            try{
                probTempMin = new Float(weatherObj.getElement(1).getXMLObj("parameters").getElement(1).getElement(1).getSingletonValue());
            }catch(Exception e) {}

        }

        if (results.size() > 1 && results.get(1) != null) {
            XMLObj airObj = results.get(1);
            Float totalEmissions;
            try {   // to get air quality index
                totalEmissions = airObj.getElement(0).getFloat("CategoryNumber");
                individualValues.put("airQuality", totalEmissions);
            } catch (Exception e) {}
        }

        // determine if values are prepared
        if (individualValues.size() > 0) valuesReady = true;
        else valuesReady = false;
        calculateDangerIndex();
    }
}