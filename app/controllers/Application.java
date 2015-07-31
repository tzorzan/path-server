package controllers;

import com.google.gson.Gson;
import implementations.SPDRouter;
import models.boundaries.PathRoutes;
import play.mvc.*;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void route() {
        String[] from = params.get("from").split(",");
        String[] to = params.get("to").split(",");

        PathRoutes routes = new PathRoutes();
        PathRoutes.Feature spdRoute = new SPDRouter().getRoute(from, to);

        String geoJson = new Gson().toJson(spdRoute);

        render(geoJson);
    }

}