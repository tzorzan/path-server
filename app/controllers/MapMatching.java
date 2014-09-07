package controllers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.*;
import models.Path;
import models.Sample;
import models.Segment;
import models.boundaries.OverpassResponse;
import org.postgis.PGgeometry;
import play.Logger;
import play.db.jpa.JPA;
import play.mvc.Controller;
import utils.OverpassQuery;

import javax.persistence.Query;

public class MapMatching extends Controller {
    public static final Double toleranceMeters = 20.0;
    private static final String bboxQuery = "" +
            "SELECT" +
            "    min(boundingbox.minlat) as minlat," +
            "    min(boundingbox.minlon) as minlon," +
            "    max(boundingbox.maxlat) as maxlat," +
            "    max(boundingbox.maxlon) as maxlon " +
            "FROM (" +
            "    SELECT" +
            "        boxes.id as id," +
            "        ST_YMin(boxes.box) as minlat," +
            "        ST_XMin(boxes.box) as minlon," +
            "        ST_YMax(boxes.box) as maxlat," +
            "        ST_XMax(boxes.box) as maxlon" +
            "    FROM" +
            "        (SELECT" +
            "            ST_Transform(ST_Expand(ST_Transform(ST_SetSRID(ST_Point(s.longitude, s.latitude),4326), 2163), :tolerance), 4326) as box," +
            "            s.id AS id" +
            "        FROM" +
            "            sample AS s" +
            "        WHERE" +
            "            s.path_id=:pathid" +
            "        ) AS boxes" +
            ") AS boundingbox;";
    private static final String nearSegmentQuery = "" +
            "SELECT id, linestring\n" +
            "  FROM segment\n" +
            "WHERE\n" +
            "  ST_DWithin(ST_Point(:lon, :lat), linestring, :tolerance);";

    public static void step1(String parameter) {
        Path path = null;

        if(parameter == null) {
            Sample sample = Sample.find("byLoaded", false).first();
            path = sample.path;
        } else {
            path = Path.findById(Long.valueOf(parameter));
        }

        if(path == null)
            notFound("Path with id: " + parameter + " not found.");

    	List<Sample> samples = path.samples;

        Query query = JPA.em().createNativeQuery(bboxQuery).setParameter("tolerance", toleranceMeters).setParameter("pathid", path.id);
        Object[] res = (Object[]) query.getResultList().get(0);

        String geoquery = "[out:json];" +
                "( way" +
                " ("+res[0]+","+res[1]+","+res[2]+","+res[3]+")" +
                "  [highway];" +
                "  >; );" +
                "out " +
                "  body;";

        OverpassResponse r = new OverpassQuery(geoquery).query();

        GeometryFactory fact = new GeometryFactory();
        List<LineString> segments = new ArrayList<LineString>();

        for(OverpassResponse.Element e : r.getWays()) {
            List<Coordinate> points  = new ArrayList<Coordinate>();

            for(Long id : e.nodes) {
                OverpassResponse.Element pe = r.getElement(id);
                if (pe != null)
                    points.add(new Coordinate(pe.lat, pe.lon));
            }

            segments.add(fact.createLineString(points.toArray(new Coordinate[points.size()])));
        }

        Envelope boundingbox = new Envelope((Double) res[0],(Double) res[2],(Double) res[3],(Double) res[1]);

        for(LineString l : segments) {
            Segment s = Segment.find("linestring = ?", l).first();
            if(s == null) {
                Logger.debug("Aggiungo segmento.");
                s = new Segment();
                s.linestring = fact.createLineString(l.getCoordinates());
                s.save();
            } else {
                Logger.debug("Segmento già presente.");
            }
        }

        render(samples, boundingbox, segments);
    }

    public static void step2(String parameter) {
        Sample sample;
        if(parameter == null) {
            sample = Sample.find("byLoaded", false).first();
        } else {
            sample = Sample.findById(Long.valueOf(parameter));
        }

        if(sample == null)
            notFound("Path with id: " + parameter + " not found.");

        List<LineString> segments = new ArrayList<LineString>();

        Query query = JPA.em().createNativeQuery(nearSegmentQuery).setParameter("tolerance", toleranceMeters).setParameter("lon", sample.longitude).setParameter("lat", sample.latitude);
        Object[] res = (Object[]) query.getResultList().get(0);

        render(sample, segments);
    }
}