package implementations;

import java.math.BigInteger;

import javax.persistence.Query;

import models.boundaries.PathRoutes;
import play.Logger;
import play.db.jpa.JPA;
import utils.RouteResult;
import interfaces.Router;

/**
 * Router implementation using Dijkstra Shortest Path algorithm.
 */
public class SPDNoiseRouter implements Router  {
  private static String nearestPointQuery = "" +
      "SELECT" +
      "  id," +
      "  ST_Distance(the_geom, ST_Point(:lon, :lat)) as distance " +
      "FROM" +
      "  roadsegment_noded_vertices_pgr " +
      "ORDER BY" +
      "  distance ASC " +
      "LIMIT 1";

  private static Double noiseRatio = 0.25;
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
      "  m_len(linestring) + (" + noiseRatio + " * m_len(linestring) * ((avg(COALESCE(l.value, 0))))) as cost " +
      "FROM " +
      "  roadsegment_noded as r " +
      "LEFT OUTER JOIN noise_sample as l ON r.id = l.roadsegment_id AND l.time_class = time_class(localtimestamp) " +
      "GROUP BY " +
      "  r.id," +
      "  r.source::integer," +
      "  r.target::integer;'), " +
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
    f.properties.comment = "Less Noise - generato con PGRouting";
    f.properties.distance = r.length;
    f.properties.maneuvers = r.maneuvers.toArray(new PathRoutes.Maneuver[r.maneuvers.size()]);
    f.properties.maneuverIndexes = r.maneuverIndexes.toArray(new Integer[r.maneuverIndexes.size()]);

    // ritorno tutto il percorso come feature GEOJson
    return f;
  }

}
