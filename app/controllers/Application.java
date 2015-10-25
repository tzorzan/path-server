package controllers;

import com.google.gson.Gson;
import implementations.SPDLightRouter;
import models.boundaries.PathRoutes;
import play.db.jpa.JPA;
import play.mvc.*;
import utils.RouteResult;

import javax.persistence.Query;
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

        Map<String, Object> p = new HashMap<String, Object>();
        p.putAll(processParam("lightRatio"));
        p.putAll(processParam("noiseRatio"));
        p.putAll(processParam("threshold"));
        p.putAll(processParam("timeClass"));

        List<PathRoutes.Feature> resultList = await(RouteResult.getAllRoutes(from, to, p));

        PathRoutes routes = new PathRoutes();
        routes.features = resultList.toArray(new PathRoutes.Feature[resultList.size()]);
        routes.type = "FeatureCollection";

        String geoJson = new Gson().toJson(routes);
        render(routes, geoJson);
    }

    private static Map<String, Object> processParam(String param) {
        //If specified, add param to the map for routing pourpose
        //otherwise adds the default value to the params of the requests
        Map<String, Object> map = new HashMap<String, Object>();
        if(params._contains(param)) {
            map.put(param, params.get(param));
        } else {
            String s = "";
            switch (param) {
                case "lightRatio":
                    s = String.valueOf(SPDLightRouter.defaultRatio);
                    break;
                case "noiseRatio":
                    s = String.valueOf(SPDLightRouter.defaultRatio);
                    break;
                case "threshold":
                    s = String.valueOf(SPDLightRouter.defaultThreshold);
                    break;
                case "timeClass":
                    String sql = "SELECT time_class(localtimestamp);";
                    Query query = JPA.em().createNativeQuery(sql);
                    s = ((Integer) query.getSingleResult()).toString();
                    break;
            }
            params.put(param, s);
            map.put(param, s);
        }
        return map;
    }
}