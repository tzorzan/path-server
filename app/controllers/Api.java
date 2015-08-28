package controllers;

import models.boundaries.PathRoutes;
import org.geojson.Feature;
import org.geojson.Point;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.FeatureCollection;
import java.text.SimpleDateFormat;
import java.util.*;
import models.*;
import play.Logger;
import play.mvc.Controller;
import utils.RouteResult;

public class Api extends Controller {
	public static void data() {
		SimpleDateFormat df =new SimpleDateFormat("yyyy:MM:dd'T'HH:mm:sszzz");
		try {
            FeatureCollection featureCollection =
                    new ObjectMapper().readValue(request.body, FeatureCollection.class);

			Path p = new Path();
			p.sent = new Date();
			p.score = (Integer) featureCollection.getProperties().get("vote");
			p.save();

            for (Feature f : featureCollection.getFeatures()) {
                Sample s = new Sample();
                s.path = p;

                s.latitude = ((Point) f.getGeometry()).getCoordinates().getLatitude();
                s.longitude = ((Point) f.getGeometry()).getCoordinates().getLongitude();

                s.uuid = f.getId();
                s.timestamp = df.parse(f.getProperties().get("timestamp").toString());
                s.accuracy = Double.parseDouble(f.getProperties().get("accuracy").toString());
                s.save();

                Map<String, Double> labels = ((Map<String, Double>) f.getProperties().get("labels"));

                Label ll = new Label();
                ll.type = Label.Type.LIGHT;
                ll.value = labels.get("light");
                ll.sample = s;
                ll.save();

                Label ln = new Label();
                ln.type = Label.Type.NOISE;
                ln.value = labels.get("noise");
                ln.sample = s;
                ln.save();
            }
			Logger.info("Added new path, id: " + p.id);
            ok();
		} catch (Exception e) {
			e.printStackTrace();
            error(e);
		}
	}

    public static void route() {
        String[] from = params.get("from").split(",");
        String[] to = params.get("to").split(",");
        Map<String, Object> params = new HashMap<String, Object>();

        List<PathRoutes.Feature> resultList = await(RouteResult.getAllRoutes(from, to, params));

        PathRoutes routes = new PathRoutes();
        routes.features = resultList.toArray(new PathRoutes.Feature[resultList.size()]);
        routes.type = "FeatureCollection";

        renderJSON(routes);
    }

}