package registry;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.SimpleDateFormat;

//helper functions
public class HelperFunctions {

    //Gets date in the specified format
    public static String getFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        // Apply format to current date/time and return
        return dateFormat.format(date);
    }

    //Check if a peer has timed out
    public static boolean checkTimeout(String curr, String ts) {
        int currMin = Integer.parseInt(curr.split(" ")[1].split(":")[1]);
        int currSec = Integer.parseInt(curr.split(" ")[1].split(":")[2]);
        int tsMin = Integer.parseInt(ts.split(" ")[1].split(":")[1]);
        int tsSec = Integer.parseInt(ts.split(" ")[1].split(":")[2]);

        double currTotal = (double) currMin + (double) (currSec / 60);
        double tsTotal = (double) tsMin + (double) (tsSec / 60);

        return currTotal >= tsTotal + 3.0;
    }
	
	//Gets string from byte buffer
    public static String data(byte[] a) {
        if (a == null) return null;
        StringBuilder returnString = new StringBuilder();
        int i = 0;
        while (a[i] != 0) {
          returnString.append((char) a[i]);
          i++;
        }
        return returnString.toString();
    }


    //Get public IP for udp server
    public static String getIP() {
        String address = "";
        try {
            URL url = new URL("http://checkip.amazonaws.com");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            address = br.readLine().trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

}
