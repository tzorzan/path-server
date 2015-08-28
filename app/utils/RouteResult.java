package utils;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import implementations.MapQuestRouter;
import implementations.SPDLightRouter;
import implementations.SPDNoiseRouter;
import implementations.SPDRouter;
import models.NodedRoadSegment;
import models.RoadSegment;
import models.boundaries.PathRoutes;
import play.db.jpa.JPA;
import play.libs.F;
import play.mvc.Http;

import javax.persistence.Query;
import java.math.BigInteger;
import java.util.*;

/**
 * Route result container class
 */
public class RouteResult {
  public List<Double[]> coordinates;
  public Double length;
  public List<PathRoutes.Maneuver> maneuvers;
  public List<Integer> maneuverIndexes;

  public RouteResult(List<Double[]> coordinates, Double length, List<PathRoutes.Maneuver> maneuvers, List<Integer> maneuverIndexes) {
    this.coordinates = coordinates;
    this.length = length;
    this.maneuvers = maneuvers;
    this.maneuverIndexes = maneuverIndexes;
  }

  public static RouteResult getRouteResultFromQueryResult(List result) {
    List<Double[]> coordinates = new ArrayList<Double[]>();
    Double length = 0.0;
    List<PathRoutes.Maneuver> maneuvers = new ArrayList<PathRoutes.Maneuver>();
    List<Integer> maneuverIndexes = new ArrayList<Integer>();

    RoadSegment lastRoadSegment = null;
    Point lastNode = null;
    Integer counter = 0;

    for (Object route : result) {
      Object[] resArray = (Object[]) route;

      // Punto da cui parte il tratto
      Point node = PGRouting.getVertexPoint(Long.valueOf((Integer) resArray[1]));

      // percorso per questo tratto
      NodedRoadSegment segment = NodedRoadSegment.findById(Long.valueOf((Integer) resArray[2]));

      if(segment != null) {
        for(Coordinate c : getNextCoordinates(node, segment.linestring)) {
          Double[] coords = new Double[2];
          coords[0] = c.x;
          coords[1] = c.y;
          coordinates.add(coords);
        }

        length += (Double) resArray[3];
        if(lastRoadSegment==null) {
          //inizializzo lastSegment e lastNode se sono alla prima iterazione
          lastRoadSegment=segment.roadSegment;
          lastNode=node;

          //aggiungo le informazioni di partenza
          PathRoutes.Maneuver route_maneuver = new PathRoutes.Maneuver();
          route_maneuver.iconUrl = "/public/images/start.gif";
          String vs = segment.roadSegment.name!=null?" da " + segment.roadSegment.name : "";
          route_maneuver.narrative = "Parti" + vs;
          route_maneuver.streets = new String[1];
          route_maneuver.streets[0] = segment.roadSegment.name;
          maneuvers.add(route_maneuver);
          maneuverIndexes.add(counter);

        } else if(lastRoadSegment.id != segment.roadSegment.id) {
          // se il segmento è cambiato allora aggiungo le informazioni per la svolta
          PathRoutes.Maneuver route_maneuver = new PathRoutes.Maneuver();
          String via = segment.roadSegment.name!=null?segment.roadSegment.name:"";
          Integer turnDirection = getTurnDirection(segment.linestring, lastNode);
          String direzione = turnDirection == CGAlgorithms.LEFT?"sinistra":"destra";
          String azione = via.equals(lastRoadSegment.name)?"Prosegui":"Svolta a " + direzione;

          String icon = turnDirection == CGAlgorithms.LEFT?"left":"right";
          icon = azione.equals("Prosegui")?"straight":icon;
          route_maneuver.iconUrl = "/public/images/"+icon+".gif";
          route_maneuver.narrative = azione + (via.equals("")?"":" in ") + via;
          route_maneuver.streets = new String[1];
          route_maneuver.streets[0] = via;

          maneuvers.add(route_maneuver);
          maneuverIndexes.add(counter);

          counter += segment.linestring.getCoordinates().length;
        }

        lastRoadSegment = segment.roadSegment;
        lastNode = node;
      }
    }

    PathRoutes.Maneuver route_maneuver = new PathRoutes.Maneuver();
    route_maneuver.iconUrl = "/public/images/end.gif";
    route_maneuver.narrative = "Sei arrivato a destinazione";
    route_maneuver.streets = new String[1];
    route_maneuver.streets[0] = lastRoadSegment.name;
    maneuvers.add(route_maneuver);
    maneuverIndexes.add(counter);

    return new RouteResult(coordinates, length, maneuvers, maneuverIndexes);
  }

  private static String nearestVertexQuery = "" +
      "SELECT" +
      "  v.id as v_id," +
      "  r.id as r_id," +
      "  r.old_id as r_old_id," +
      "  r.source," +
      "  r.target," +
      "  ST_Distance(r.linestring, ST_Point(:lon, :lat)) as r_distance," +
      "  ST_Distance(v.the_geom, ST_Point(:lon, :lat)) as v_distance " +
      "FROM" +
      "  roadsegment_noded_vertices_pgr as v " +
      "JOIN" +
      "  roadsegment_noded as r ON r.source = v.id OR r.target = v.id " +
      "ORDER BY" +
      "  r_distance, v_distance ASC " +
      "LIMIT 1;";

  public static Long getNearestVertex(Point point){
    //Find nearest vertex from end
    Query query = JPA.em().createNativeQuery(nearestVertexQuery).setParameter("lon", point.getX()).setParameter("lat", point.getY());
    Object[] res = (Object[]) query.getSingleResult();
    return ((BigInteger) res[0]).longValue();
  }

  public static F.Promise<List<PathRoutes.Feature>> getAllRoutes(String[] from, String[] to, Map<String, Object> params) {
    F.Promise<PathRoutes.Feature> spdRouteJobResult = new RouteJob(new SPDRouter(), from, to, params).now();
    F.Promise<PathRoutes.Feature> spdLightRouteJobResult = new RouteJob(new SPDLightRouter(), from, to, params).now();
    F.Promise<PathRoutes.Feature> spdNoiseRouteJobResult = new RouteJob(new SPDNoiseRouter(), from, to, params).now();
    F.Promise<PathRoutes.Feature> mapQuestRouteJobResult = new RouteJob(new MapQuestRouter(), from, to, params).now();

    return F.Promise.waitAll(spdRouteJobResult, spdLightRouteJobResult, spdNoiseRouteJobResult, mapQuestRouteJobResult);
  }

  private static List<Coordinate> getNextCoordinates(Point node, LineString linestring) {
    List<Coordinate> coordsList= Arrays.asList(linestring.getCoordinates());
    Point sl =  new GeometryFactory().createPoint(coordsList.get(coordsList.size() - 1));

    if(node.equals(sl)) {
      Collections.reverse(coordsList);
    }

    return coordsList;
  }

  private static int getTurnDirection(LineString segment, Point node) {
    List<Coordinate> l = Arrays.asList(segment.getCoordinates());
    // confronto l'ultimo nodo con il prossimo segmento
    return CGAlgorithms.computeOrientation(l.get(l.size()-2), l.get(l.size()-1),node.getCoordinate());
  }
}
