package utils;

import com.google.gson.Gson;
import models.boundaries.MapQuestResponse;
import models.boundaries.PathRoutes;
import play.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapQuestQuery {
    private static final String MAPQUEST_WS = "http://open.mapquestapi.com/directions/v2";
    private static final String MAPQUEST_KEY = "Fmjtd%%7Cluur2huz2h%%2Crn%%3Do5-9wa0h0";
    private static final String MAPQUEST_QUERY_ROUTE_FORMAT = MAPQUEST_WS + "/route?key=" + MAPQUEST_KEY + "&outFormat=json&routeType=pedestrian&doReverseGeocode=true&narrativeType=text&shapeFormat=raw&unit=m&manMaps=false&locale=it_IT&from=%.6f,%.6f&to=%.6f,%.6f";
    private static final String MAPQUEST_QUERY_SHAPE_FORMAT = MAPQUEST_WS + "/routeshape?key=" + MAPQUEST_KEY + "&json={sessionId:%s,mapState:{width:1000,height:1000,zoom:16,center:{lat:%.6f,lng:%.6f}}}";

    private Double fromLat;
    private Double fromLon;
    private Double toLat;
    private Double toLon;

    private String queryRoute;
    private String queryShape;

    public MapQuestQuery(Double fromLat, Double fromLon, Double toLat, Double toLon) {
        this.fromLat = fromLat;
        this.fromLon = fromLon;
        this.toLat = toLat;
        this.toLon = toLon;
    }

    public PathRoutes query() {
        queryRoute = String.format(Locale.ENGLISH, MAPQUEST_QUERY_ROUTE_FORMAT, fromLat, fromLon, toLat, toLon);
        Logger.debug("Query MapQuest Route:\n" + queryRoute);

        MapQuestResponse routeResponse = new Gson().fromJson(doWSCall(queryRoute), MapQuestResponse.class);

        Double lat = (routeResponse.route.boundingBox.ul.lat + routeResponse.route.boundingBox.lr.lat) / 2;
        Double lng = (routeResponse.route.boundingBox.ul.lng + routeResponse.route.boundingBox.lr.lng) / 2;

        queryShape = String.format(Locale.ENGLISH, MAPQUEST_QUERY_SHAPE_FORMAT, routeResponse.route.sessionId, lat, lng);
        Logger.debug("Query MapQuest Shape:\n" + queryShape);

        MapQuestResponse shapeResponse = new Gson().fromJson(doWSCall(queryShape), MapQuestResponse.class);

        PathRoutes routes = new PathRoutes();

        routes.type = "FeatureCollection";

        PathRoutes.Feature feature = new PathRoutes.Feature();
        feature.type = "Feature";

        routes.features = new PathRoutes.Feature[1];
        routes.features[0] = new PathRoutes.Feature();
        routes.features[0].type = "Feature";
        routes.features[0].geometry = new PathRoutes.Geometry();
        routes.features[0].geometry.type = "LineString";
        routes.features[0].geometry.coordinates = new ArrayList<Double[]>();
        for(int i=0; i<shapeResponse.route.shape.shapePoints.length; i= i+2) {
            Double[] coords = new Double[2];
            coords[1] = shapeResponse.route.shape.shapePoints[i];
            coords[0] = shapeResponse.route.shape.shapePoints[i+1];
            routes.features[0].geometry.coordinates.add(coords);
        }
        routes.features[0].properties = new PathRoutes.Properties();
        routes.features[0].properties.comment = "Generato con servizio MapQuest.";
        routes.features[0].properties.distance = routeResponse.route.distance;

        List<PathRoutes.Maneuver> maneuvers = new ArrayList<PathRoutes.Maneuver>();
        for(MapQuestResponse.Maneuver mapquest_maneuver : routeResponse.route.legs[0].maneuvers) {
            PathRoutes.Maneuver route_maneuver = new PathRoutes.Maneuver();
            route_maneuver.iconUrl = mapquest_maneuver.iconUrl;
            route_maneuver.narrative = mapquest_maneuver.narrative;
            route_maneuver.streets = mapquest_maneuver.streets;
            maneuvers.add(route_maneuver);
        }
        routes.features[0].properties.maneuvers = maneuvers.toArray(new PathRoutes.Maneuver[maneuvers.size()]);

        routes.features[0].properties.maneuverIndexes = shapeResponse.route.shape.maneuverIndexes;

        return routes;
    }

    private Reader doWSCall(String querystring) {
        try {
            URL url = new URL(querystring);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            return new InputStreamReader(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
