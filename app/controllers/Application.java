package controllers;

import com.google.gson.Gson;
import models.boundaries.PathRoutes;
import play.mvc.*;
import utils.RouteResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void route() {
        String[] from = params.get("from").split(",");
        String[] to = params.get("to").split(",");
        Map<String, Object> params = new HashMap<String, Object>();

        List<PathRoutes.Feature> resultList = await(RouteResult.getAllRoutes(from, to, params));

        PathRoutes routes = new PathRoutes();
        routes.features = resultList.toArray(new PathRoutes.Feature[resultList.size()]);
        routes.type = "FeatureCollection";

        String geoJson = new Gson().toJson(routes);
        render(routes, geoJson);
    }

}