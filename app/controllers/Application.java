package controllers;

import com.google.gson.Gson;
import implementations.MapQuestRouter;
import implementations.SPDLightRouter;
import implementations.SPDRouter;
import models.boundaries.PathRoutes;
import play.libs.F;
import play.mvc.*;
import utils.RouteJob;

import java.util.List;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void route() {
        String[] from = params.get("from").split(",");
        String[] to = params.get("to").split(",");

        PathRoutes routes = new PathRoutes();

        F.Promise<PathRoutes.Feature> spdRouteJobResult = new RouteJob(new SPDRouter(), from, to).now();
        F.Promise<PathRoutes.Feature> spdLightRouteJobResult = new RouteJob(new SPDLightRouter(), from, to).now();
        F.Promise<PathRoutes.Feature> mapQuestRouteJobResult = new RouteJob(new MapQuestRouter(), from, to).now();

        F.Promise<List<PathRoutes.Feature>> jobList = F.Promise.waitAll(spdRouteJobResult, spdLightRouteJobResult, mapQuestRouteJobResult);
        List<PathRoutes.Feature> resultList = await(jobList);

        routes.features = resultList.toArray(new PathRoutes.Feature[resultList.size()]);

        routes.type = "FeatureCollection";

        String geoJson = new Gson().toJson(routes);
        render(routes, geoJson);
    }

}