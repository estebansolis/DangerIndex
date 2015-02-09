/*
    CSCE 315-501
    Jonathan Sandell, Vyas Sathya, Esteban Solis, Josh Orndorff
    Project 3 - Final submission
    12/10/2014

    Description: General class for XML operations

    NOTE: Much of the code and comments in the .java files (and others) were auto-generated by Android Studio

*/


package com.dangerindexapp.team9.project_3_team_9;

import android.content.Context;
import android.util.Xml;

import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import com.dangerindexapp.team9.project_3_team_9.MainMenu;

/**
 * Created by Josh on 11/20/2014.
 */

public class XMLManipulator {
    private Context classContext;
    public XMLManipulator(Context context) {classContext = context;}

    // Download XML data from a URL
    public XMLObj downloadXMLData(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(20000);   // 20 sec timeout
            connection.setDoInput(true);
            connection.connect();
            InputStream is = connection.getInputStream();
            return loadData(is);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;    // if failed, return null
    }

    // Load XML data from a local file
    public XMLObj loadXMLData(File file) {
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            return loadData(is);
        }
        catch(Exception exc) {
            String s = exc.toString();
        }
        return null;    // if failed, return null
    }

    // Retrieve XML data from an input stream
    public XMLObj loadData(InputStream is) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(is, null);
            parser.next();
            return readXMLObj(parser);
        }
        catch(Exception exc) {
            String s = exc.getLocalizedMessage();
        }
        return null;
    }

    // Read the next XML object from the given parser
    private XMLObj readXMLObj(XmlPullParser parser) {
        XMLObj xo = null;
        HashMap<String, String> map = new HashMap<String, String>();
        String name = "";
        try {
            while (parser.getEventType() != XmlPullParser.START_TAG) parser.next();
            name = parser.getName();
            for (int i = parser.getAttributeCount() - 1; i >= 0; i--) { // add attributes
                String k = parser.getAttributeName(i);
                String v = parser.getAttributeValue(i);
                map.put(k, v);
            }
            parser.next();
            while (parser.getEventType() == XmlPullParser.TEXT && parser.isWhitespace()) {
            try {
                parser.next();
            }
            catch (Exception e) {}
            }
            if (parser.getEventType() == XmlPullParser.TEXT) {  // singleton
                String text = parser.getText();
                while (!(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals(name))) parser.next();
                return new XMLObj(name, text);
            }
            else if (parser.getEventType() == XmlPullParser.START_TAG) {    // nested
                xo = new XMLObj(name);
                while (parser.getEventType() != XmlPullParser.END_TAG) {
                    String propertyName = parser.getName();
                    xo.addProperty(propertyName, readXMLObj(parser));
                    while (parser.getEventType() != XmlPullParser.END_TAG) parser.next();
                    while (parser.next() == XmlPullParser.TEXT && parser.isWhitespace());
                }
            }
        }
        catch (Exception exc) {
            return null;
        }
        if (xo == null) {
            xo = new XMLObj(name);
        }
        xo.setAttributes(map);
        return xo;
    }

    // Write the given XML object to the specified filename
    public void writeXMLFile(XMLObj obj, String filename) {
        FileOutputStream output;
        try {
            output = classContext.openFileOutput(filename, Context.MODE_PRIVATE);
            String str = obj.toString();
            output.write(str.getBytes());
            output.close();
        }
        catch (Exception exc) {}
    }
}
