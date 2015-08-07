package utils;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import models.NodedRoadSegment;
import models.RoadSegment;
import models.boundaries.PathRoutes;
import play.mvc.Http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        //inizializzo lastSegment e lastNode se sono alla prima iterazione
        lastRoadSegment = lastRoadSegment==null?segment.roadSegment:lastRoadSegment;
        lastNode = lastNode==null?node:lastNode;

        for(Coordinate c : getNextCoordinates(node, segment.linestring)) {
          Double[] coords = new Double[2];
          coords[0] = c.x;
          coords[1] = c.y;
          coordinates.add(coords);
        }

        length += (Double) resArray[3];
        // se il segmento è cambiato allora aggiungo le informazioni per la svolta
        if(lastRoadSegment.id != segment.roadSegment.id) {
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
    return new RouteResult(coordinates, length, maneuvers, maneuverIndexes);
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
    // confronto l'ultimo nodo con il prossimo segmento, devo invertire il risultato
    return CGAlgorithms.computeOrientation(l.get(l.size()-2), l.get(l.size()-1),node.getCoordinate())*-1;
  }
}
