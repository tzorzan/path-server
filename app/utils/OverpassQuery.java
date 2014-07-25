package utils;

import com.google.gson.Gson;
import models.boundaries.OverpassResponse;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class OverpassQuery {
    private static final String OVERPASS_API = "http://www.overpass-api.de/api/interpreter";

    private String query;

    public OverpassQuery(String query){
        this.query = query;
    }

    public OverpassResponse query() {
        try {
            URL overpass = new URL(OVERPASS_API);
            HttpURLConnection connection = (HttpURLConnection) overpass.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
            printout.writeBytes("data=" + URLEncoder.encode(query, "utf-8"));
            printout.flush();
            printout.close();

            Reader reader = new InputStreamReader(connection.getInputStream());

            return new Gson().fromJson(reader, OverpassResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

}
