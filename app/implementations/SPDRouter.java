package implementations;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import interfaces.Router;
import models.NodedRoadSegment;
import models.RoadSegment;
import models.boundaries.PathRoutes;
import play.Logger;
import play.db.jpa.JPA;
import utils.PGRouting;
import utils.RouteResult;

import javax.persistence.Query;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Router implementation using Dijkstra Shortest Path algorithm.
 */
public class SPDRouter implements Router {
  private static String nearestPointQuery = "" +
      "SELECT" +
      "  id," +
      "  ST_Distance(the_geom, ST_Point(:lon, :lat)) as distance " +
      "FROM" +
      "  roadsegment_noded_vertices_pgr " +
      "ORDER BY" +
      "  distance ASC " +
      "LIMIT 1";

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
  public PathRoutes.Feature getRoute(String[] from, String[] to) {
    //Find nearest vertex from start
    Query query = JPA.em().createNativeQuery(nearestPointQuery).setParameter("lon", Double.valueOf(from[1])).setParameter("lat", Double.valueOf(from[0]));
    Object[] res = (Object[]) query.getSingleResult();
    Long startId = ((BigInteger) res[0]).longValue();


    //Find nearest vertex from end
    query = JPA.em().createNativeQuery(nearestPointQuery).setParameter("lon", Double.valueOf(to[1])).setParameter("lat", Double.valueOf(to[0]));
    res = (Object[]) query.getSingleResult();
    Long endId = ((BigInteger) res[0]).longValue();

    //Calculate routing path using PGRouting
    Logger.info("Start: " + startId + "  End: " + endId);
    query = JPA.em().createNativeQuery(routingQuery).setParameter("start_vertex", startId).setParameter("end_vertex", endId);

    RouteResult r = RouteResult.getRouteResultFromQueryResult(query.getResultList());
    PathRoutes.Feature f = new PathRoutes.Feature();
    f.type = "Feature";
    f.geometry = new PathRoutes.Geometry();
    f.geometry.type = "LineString";
    f.geometry.coordinates = r.coordinates;

    f.properties = new PathRoutes.Properties();
    f.properties.comment = "Shortest Path - generato con PGRouting";
    f.properties.distance = r.length;
    f.properties.maneuvers = r.maneuvers.toArray(new PathRoutes.Maneuver[r.maneuvers.size()]);
    f.properties.maneuverIndexes = r.maneuverIndexes.toArray(new Integer[r.maneuverIndexes.size()]);

    // ritorno tutto il percorso come feature GEOJson
    return f;
  }

  private List<Coordinate> getNextCoordinates(Point node, LineString linestring) {
    List<Coordinate> coordsList= Arrays.asList(linestring.getCoordinates());
    Point sl =  new GeometryFactory().createPoint(coordsList.get(coordsList.size() - 1));

    if(node.equals(sl)) {
      Collections.reverse(coordsList);
    }

    return coordsList;
  }

  private int getTurnDirection(LineString segment, Point node) {
    List<Coordinate> l = Arrays.asList(segment.getCoordinates());
    // confronto l'ultimo nodo con il prossimo segmento, devo invertire il risultato
    return CGAlgorithms.computeOrientation(l.get(l.size()-2), l.get(l.size()-1),node.getCoordinate())*-1;
  }

}
