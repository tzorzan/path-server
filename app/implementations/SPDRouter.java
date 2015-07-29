package implementations;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import interfaces.Router;
import models.NodedRoadSegment;
import models.boundaries.PathRoutes;
import play.Logger;
import play.db.jpa.JPA;
import utils.PGRouting;

import javax.persistence.Query;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Router implementation using Dijkstra Shortest Path algorithm.
 */
public class SPDRouter implements Router {
  private static String nearestPointQuery = "" +
      "SELECT" +
      "  id," +
      "  ST_Distance(the_geom, ST_Point(:lat, :lon)) as distance " +
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
    PathRoutes.Feature f = new PathRoutes.Feature();
    f.type = "Feature";
    f.geometry = new PathRoutes.Geometry();
    f.geometry.type = "LineString";
    f.geometry.coordinates = new ArrayList<Double[]>();
    List<PathRoutes.Maneuver> maneuvers = new ArrayList<PathRoutes.Maneuver>();
    List<Integer> maneuverIndexes = new ArrayList<Integer>();

    f.properties = new PathRoutes.Properties();
    f.properties.comment = "Shortest Path - PGRouting.";

    Double length = 0.0;
    Long lastSegmentId = null;
    Integer counter = 0;

    //Find nearest vertex from start
    Query query = JPA.em().createNativeQuery(nearestPointQuery).setParameter("lat", Double.valueOf(from[0])).setParameter("lon", Double.valueOf(from[1]));
    Object[] res = (Object[]) query.getSingleResult();
    Long startId = ((BigInteger) res[0]).longValue();


    //Find nearest vertex from end
    query = JPA.em().createNativeQuery(nearestPointQuery).setParameter("lat", Double.valueOf(to[0])).setParameter("lon", Double.valueOf(to[1]));
    res = (Object[]) query.getSingleResult();
    Long endId = ((BigInteger) res[0]).longValue();

    //Calculate routing path using PGRouting
    Logger.info("Start: " + startId + "  End: " + endId);
    query = JPA.em().createNativeQuery(routingQuery).setParameter("start_vertex", startId).setParameter("end_vertex", endId);

    for (Object route : query.getResultList()) {
      Object[] resArray = (Object[]) route;

      // Punto da cui parte il tratto
      Point node = PGRouting.getVertexPoint(Long.valueOf((Integer) resArray[1]));

      // percorso per questo tratto
      NodedRoadSegment segment = NodedRoadSegment.findById(Long.valueOf((Integer) resArray[2]));
      if(segment != null) {
        // aggiungo tutti i punti del tratto al percorso
        for(Coordinate c : segment.linestring.getCoordinates()) {
          Double[] coords = new Double[2];
          coords[0] = c.x;
          coords[1] = c.y;
          f.geometry.coordinates.add(coords);
        }

        length += (Double) resArray[3];
        if(lastSegmentId != segment.roadSegment.id) {
          // se il segmento è cambiato allora aggiungo le informazioni per la svolta

          PathRoutes.Maneuver route_maneuver = new PathRoutes.Maneuver();
          route_maneuver.iconUrl = "icona";
          route_maneuver.narrative = "descrizione narrativa";
          route_maneuver.streets = new String[1];
          route_maneuver.streets[0] = "Street id: " + segment.roadSegment.id;
          maneuvers.add(route_maneuver);

          maneuverIndexes.add(counter);

          lastSegmentId = segment.roadSegment.id;
          counter += segment.linestring.getCoordinates().length;
        }

        f.properties.maneuvers = maneuvers.toArray(new PathRoutes.Maneuver[maneuvers.size()]);
        f.properties.maneuverIndexes = maneuverIndexes.toArray(new Integer[maneuverIndexes.size()]);

      }
    }

    f.properties.distance = length;
    f.properties.maneuvers = maneuvers.toArray(new PathRoutes.Maneuver[maneuvers.size()]);
    f.properties.maneuverIndexes = maneuverIndexes.toArray(new Integer[maneuverIndexes.size()]);

    // ritorno tutto il percorso come feature GEOJson
    return f;
  }

}
