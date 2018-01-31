package com.mikhaellopez.androidwebserver;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Mikhael LOPEZ on 14/12/2015.
 */
public class AndroidWebServer extends NanoHTTPD {

    public AndroidWebServer(int port) {
        super(port);
    }

    public AndroidWebServer(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";
        msg += "<script language=\"JavaScript\">\n ";
        msg += "function myrefresh()\n";
        msg += "{ \n" +
                "window.location.reload(); \n" +
                "} \n";
        msg += "setTimeout('myrefresh()',600000);\n";
        msg += "</script>\n";
        Map<String, String> parms = session.getParms();
        if (parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }

        Date currentTime = Calendar.getInstance().getTime();
        msg += "<p>"+ currentTime.toString()
                +"</p>";

        msg += "<svg xmlns=\"http://www.w3.org/2000/svg\"\n" +
                "width=\"100%\" height=\"100%\" version='1.1'>\n" +
                "   <polyline points=\"20,20 40,25 60,40 80,120 120,140 200,180\"\n" +
                "         style=\"fill:white;stroke:red;stroke-width:3\"/>\n" +
                "</svg>";

        return newFixedLengthResponse( msg + "</body></html>\n" );
    }
}
