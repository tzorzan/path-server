package implementations;

import javax.persistence.Query;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import models.boundaries.PathRoutes;
import play.Logger;
import play.db.jpa.JPA;
import interfaces.Router;
import utils.RouteResult;

import java.util.Map;

/**
 * Router implementation using Dijkstra Shortest Path algorithm.
 */
public class SPDLightRouter implements Router  {
  public static Double defaultRatio = 0.25;
  public static Double defaultThreshold = 0.5;
  public static Integer defaultTimeClass = 1;
  private static String routingQuery ="" +
      "SELECT " +
      "   seq, " +
      "   id1 AS node, " +
      "   id2 AS edge, " +
      "   cost " +
      "FROM pgr_dijkstra(" +
      "text(" +
      "  'SELECT " +
      "  r.id," +
      "  r.source::integer," +
      "  r.target::integer," +
      "  m_len(linestring) as length_cost, " +
      "  avg(COALESCE(l.value, 0)) as label_value, " +
      "  m_len(linestring) + ( :ratio * m_len(linestring) * ( ((avg(COALESCE(l.value, 0)))/100) - :threshold) ) as cost " +
      "FROM " +
      "  roadsegment_noded as r " +
      "LEFT OUTER JOIN light_sample as l ON r.id = l.id AND l.time_class = :timeClass " +
      "GROUP BY " +
      "  r.id," +
      "  r.source::integer," +
      "  r.target::integer;'), " +
      "CAST (:start_vertex as integer), " +
      "CAST (:end_vertex as integer), " +
      "false, " +
      "false);";

  @Override
  public PathRoutes.Feature getRoute(String[] from, String[] to, Map<String, Object> params) {
    GeometryFactory gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);

    //Find nearest vertex from start
    Long startId = RouteResult.getNearestVertex(gf.createPoint(new Coordinate(Double.valueOf(from[1]), Double.valueOf(from[0]))));

    //Find nearest vertex from end
    Long endId = RouteResult.getNearestVertex(gf.createPoint(new Coordinate(Double.valueOf(to[1]), Double.valueOf(to[0]))));

    //Check ratio param
    Double ratio = params.keySet().contains("lightRatio")?(Double.valueOf(params.get("lightRatio").toString())): defaultRatio;

    //Check thershold param
    Double threshold = params.keySet().contains("threshold")?(Double.valueOf(params.get("threshold").toString())): defaultThreshold;

    //Check time class param
    Integer timeClass = params.keySet().contains("timeClass")?(Integer.valueOf(params.get("timeClass").toString())): defaultTimeClass;

    //Calculate routing path using PGRouting
    Logger.debug(this.getClass() + ": Start:" + startId + " End: " + endId + " Ratio: " + ratio+ " Threshold: " + threshold + " Time class: " + timeClass);

    //NOTE: Forced to use String replace because :namedParameter doesn't work in text(...)

    Query query = JPA.em().createNativeQuery(
            routingQuery.replace(":ratio", ratio.toString())
                    .replace(":threshold", threshold.toString())
                    .replace(":timeClass", timeClass.toString())
                    .replace(":start_vertex", startId.toString())
                    .replace(":end_vertex", endId.toString()));

    RouteResult r = RouteResult.getRouteResultFromQueryResult(query.getResultList());
    PathRoutes.Feature f = new PathRoutes.Feature();
    f.type = "Feature";
    f.geometry = new PathRoutes.Geometry();
    f.geometry.type = "LineString";
    f.geometry.coordinates = r.coordinates;

    f.properties = new PathRoutes.Properties();
    f.properties.comment = "Less Light - generato con PGRouting";
    f.properties.color = "grey";
    f.properties.distance = r.length;
    f.properties.maneuvers = r.maneuvers.toArray(new PathRoutes.Maneuver[r.maneuvers.size()]);
    f.properties.maneuverIndexes = r.maneuverIndexes.toArray(new Integer[r.maneuverIndexes.size()]);

    // ritorno tutto il percorso come feature GEOJson
    return f;
  }
}
