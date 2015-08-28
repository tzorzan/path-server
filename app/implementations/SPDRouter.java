package implementations;

import com.vividsolutions.jts.geom.*;
import interfaces.Router;
import models.boundaries.PathRoutes;
import play.Logger;
import play.db.jpa.JPA;
import utils.RouteResult;
import javax.persistence.Query;
import java.util.Map;

/**
 * Router implementation using Dijkstra Shortest Path algorithm.
 */
public class SPDRouter implements Router {
  private static String routingQuery ="" +
      "SELECT " +
      "   seq, " +
      "   id1 AS node, " +
      "   id2 AS edge, " +
      "   cost " +
      "FROM pgr_dijkstra(" +
      "text(" +
      "  'SELECT " +
      "    id," +
      "    source::integer," +
      "    target::integer," +
      "    ST_Length(ST_Transform(ST_SetSRID(linestring, 4326),2163)) as cost" +
      "  FROM roadsegment_noded;'), " +
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

    //Calculate routing path using PGRouting
    Logger.info("Start: " + startId + "  End: " + endId);
    Query query = JPA.em().createNativeQuery(routingQuery).setParameter("start_vertex", startId).setParameter("end_vertex", endId);

    RouteResult r = RouteResult.getRouteResultFromQueryResult(query.getResultList());
    PathRoutes.Feature f = new PathRoutes.Feature();
    f.type = "Feature";
    f.geometry = new PathRoutes.Geometry();
    f.geometry.type = "LineString";
    f.geometry.coordinates = r.coordinates;

    f.properties = new PathRoutes.Properties();
    f.properties.comment = "Shortest Path - generato con PGRouting";
    f.properties.color = "red";
    f.properties.distance = r.length;
    f.properties.maneuvers = r.maneuvers.toArray(new PathRoutes.Maneuver[r.maneuvers.size()]);
    f.properties.maneuverIndexes = r.maneuverIndexes.toArray(new Integer[r.maneuverIndexes.size()]);

    // ritorno tutto il percorso come feature GEOJson
    return f;
  }

}
