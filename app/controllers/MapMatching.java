package controllers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.*;
import models.CandidatePoint;
import models.Path;
import models.RoadSegment;
import models.Sample;
import models.boundaries.OverpassResponse;
import play.Logger;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.Router;
import utils.OverpassQuery;
import utils.STMapMatching;

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
            "SELECT" +
            " * FROM RoadSegment s " +
            "WHERE" +
            " ST_DWithin(" +
            "  ST_Transform(ST_SetSRID(s.linestring, 4326),2163)," +
            "  ST_Transform(ST_SetSRID(ST_Point(:sample_latitude, :sample_longitude), 4326),2163)," +
            "  :tolerance" +
            ")";
    private static final String candidatesQuery = "" +
            "SELECT " +
            "   ST_X(candidates.c) as lat, ST_Y(candidates.c) as lon " +
            "FROM (" +
            "   SELECT " +
            "       ST_ClosestPoint(s.linestring, ST_Point(:sample_latitude, :sample_longitude)) as c " +
            "   FROM RoadSegment s " +
            "   WHERE s.id in (:near_segments_id)" +
            ") as candidates";


    public static void list() {
        List<Path> paths = Path.findAll();
        String link_step1 = Router.reverse("MapMatching.step1").url;
        String link_step2 = Router.reverse("MapMatching.step2").url;
        String link_step3 = Router.reverse("MapMatching.step3").url;
        render(paths,link_step1, link_step2, link_step3);
    }

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
            RoadSegment s = RoadSegment.find("linestring = ?", l).first();
            if(s == null) {
                Logger.debug("Aggiungo nuovo segmento.");
                s = new RoadSegment();
                s.linestring = fact.createLineString(l.getCoordinates());
                s.save();
            } else {
                Logger.trace("Segmento già presente.");
            }
        }

        render(samples, boundingbox, segments);
    }

    public static void step2(String parameter) {
        Path path = null;
        if(parameter == null) {
            Sample sample = Sample.find("byLoaded", false).first();
            path = sample.path;
        } else {
            path = Path.findById(Long.valueOf(parameter));
        }
        if(path == null)
            notFound("Path with id: " + parameter + " not found.");

        List<List<LineString>> segments = new ArrayList<List<LineString>>();
        List<List<Point>> candidates = new ArrayList<List<Point>>();

        for(Sample samp:path.samples){

            List<RoadSegment> result = JPA.em().createNativeQuery(nearSegmentQuery, RoadSegment.class)
                    .setParameter("sample_latitude", samp.latitude)
                    .setParameter("sample_longitude", samp.longitude)
                    .setParameter("tolerance", toleranceMeters)
                    .getResultList();

            List<LineString> candidateSegments = new ArrayList<LineString>();
            List<Point> candidatePoints = new ArrayList<Point>();
            List<Long> candidateSegmentsIds = new ArrayList<Long>();
            for (RoadSegment r:result) {
                candidateSegments.add(r.linestring);
                candidateSegmentsIds.add(r.id);
            }

            segments.add(candidateSegments);

            Query query = JPA.em().createNativeQuery(candidatesQuery).setParameter("sample_latitude", samp.latitude)
                    .setParameter("sample_longitude", samp.longitude).setParameter("near_segments_id", candidateSegmentsIds);

            int i = 0;
            for (Object res : query.getResultList()) {
                Object[] resArray = (Object[]) res;
                CandidatePoint c = new CandidatePoint((Double) resArray[0], (Double) resArray[1]);
                c.sample = samp;
                c.roadSegment = result.get(i);
                c.save();
                candidatePoints.add(c.getPoint());
                i++;
            }

            candidates.add(candidatePoints);
        }

        render(path, segments, candidates);
    }

    public static void step3(String parameter) {
        Path path = null;
        if(parameter == null) {
            Sample sample = Sample.find("byLoaded", false).first();
            path = sample.path;
        } else {
            path = Path.findById(Long.valueOf(parameter));
        }
        if(path == null)
            notFound("Path with id: " + parameter + " not found.");

        List<List<LineString>> segments = new ArrayList<List<LineString>>();
        List<List<Point>> candidates = new ArrayList<List<Point>>();
        GeometryFactory fact = new GeometryFactory();

        for(Sample samp:path.samples){
            List<LineString> candidateSegments = new ArrayList<LineString>();
            List<Point> candidatePoints = new ArrayList<Point>();
            List<CandidatePoint> sampleCandidates = CandidatePoint.find("bySample", samp).fetch();
            Logger.debug("Sample: " + samp.id + " (" + sampleCandidates.size() + " candidates):");

            for (CandidatePoint candidate : sampleCandidates) {
                Point samplePoint = fact.createPoint(new Coordinate(samp.latitude, samp.longitude));
                candidateSegments.add(candidate.roadSegment.linestring);
                candidatePoints.add(candidate.getPoint());
                Logger.debug("Observation probability [ "+ samp.id + " - "+ candidate.id + " ]: " + new DecimalFormat("0.00000000").format(STMapMatching.observationProbability(samplePoint, candidate.getPoint())));
            }

            segments.add(candidateSegments);
            candidates.add(candidatePoints);
        }

        render(path, segments, candidates);
   }

}