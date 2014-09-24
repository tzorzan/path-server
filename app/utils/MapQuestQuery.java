package utils;

import com.google.gson.Gson;
import models.boundaries.MapQuestRouteResponse;
import play.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MapQuestQuery {
    private static final String MAPQUEST_WS = "http://open.mapquestapi.com/directions/v2";
    private static final String MAPQUEST_KEY = "Fmjtd%%7Cluur2huz2h%%2Crn%%3Do5-9wa0h0";
    private static final String MAPQUEST_QUERY_ROUTE_FORMAT = MAPQUEST_WS + "/route?key=" + MAPQUEST_KEY + "&outFormat=json&routeType=pedestrian&doReverseGeocode=true&narrativeType=text&shapeFormat=raw&unit=m&manMaps=false&locale=it_IT&from=%.6f,%.6f&to=%.6f,%.6f";

    private Double fromLat;
    private Double fromLon;
    private Double toLat;
    private Double toLon;

    private String queryRoute;
    private String queryShape;

    public MapQuestQuery(Double fromLat, Double fromLon, Double toLat, Double toLon) {
        this.fromLat = toLat;
        this.fromLon = toLon;
        this.toLat = toLat;
        this.toLon = toLon;

        queryRoute = String.format(Locale.ENGLISH, MAPQUEST_QUERY_ROUTE_FORMAT, fromLat, fromLon, toLat, toLon);
        Logger.debug("Query MapQuest Route:\n" + queryRoute);
    }

    public MapQuestRouteResponse query() {
        try {
            URL mapquest = new URL(queryRoute);
            HttpURLConnection connection = (HttpURLConnection) mapquest.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            Reader reader = new InputStreamReader(connection.getInputStream());

            return new Gson().fromJson(reader, MapQuestRouteResponse.class);
        } catch (Exception e) {
            return null;
        }
    }
}
