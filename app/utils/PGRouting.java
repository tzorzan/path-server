package utils;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import models.CandidatePoint;
import models.NodedRoadSegment;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.operation.TransformException;
import play.Logger;
import play.cache.Cache;
import play.db.jpa.JPA;
import javax.persistence.Query;
import java.util.*;

public class PGRouting {
    private static String edgeQuery ="" +
            "SELECT " +
            "   id, " +
            "   source::integer, " +
            "   target::integer, " +
            "   ST_Length(ST_Transform(ST_SetSRID(linestring, 4326),2163)) as cost " +
            "FROM " +
            "   roadsegment_noded";
    private static String labelCostQuery ="" +
            "SELECT " +
            "   id," +
            "   source::integer, " +
            "   target::integer, " +
            "   COST_WITH_LABEL " +
            "FROM " +
            "   roadsegment_noded";
    private static String routingQuery ="" +
            "SELECT " +
            "   seq, " +
            "   id1 AS node, " +
            "   id2 AS edge, " +
            "   cost " +
            "FROM pgr_dijkstra(" +
            "text(:edge_query), " +
            "CAST (:start_vertex as integer), " +
            "CAST (:end_vertex as integer), " +
            "false, " +
            "false);";
    private static String vertexQuery ="" +
            "SELECT " +
            "   ST_AsText(the_geom) as point_wkt " +
            "FROM " +
            "   roadsegment_noded_vertices_pgr " +
            "WHERE " +
            "   id = :id";

    private static String distanceBySegment = "" +
      "SELECT " +
      "  ST_Length(ST_Transform(ST_SetSRID(ST_Line_Substring(" +
      "    c.linestring, " +
      "    0.0, " +
      "    CASE WHEN c.p1_pos < c.p2_pos THEN c.p1_pos ELSE c.p2_pos END), 4326),2163)) as a_len," +
      "  ST_Length(ST_Transform(ST_SetSRID(ST_Line_Substring(" +
      "    c.linestring, " +
      "    CASE WHEN c.p1_pos < c.p2_pos THEN c.p1_pos ELSE c.p2_pos END, " +
      "    CASE WHEN c.p1_pos < c.p2_pos THEN c.p2_pos ELSE c.p1_pos END), 4326),2163)) as b_len," +
      "  ST_Length(ST_Transform(ST_SetSRID(ST_Line_Substring(" +
      "    c.linestring, " +
      "    CASE WHEN c.p1_pos < c.p2_pos THEN c.p2_pos ELSE c.p1_pos END, " +
      "    1.0), 4326),2163)) as c_len, " +
      "  ST_Length(ST_Transform(ST_SetSRID(c.linestring, 4326),2163)) as tot_len, " +
      "  c.p1_pos as p1, " +
      "  c.p2_pos as p2 " +
      "FROM " +
      "(" +
      "  SELECT " +
      "    e.linestring as linestring," +
      "    ST_Line_Locate_Point(e.linestring, e.p1) as p1_pos," +
      "    ST_Line_Locate_Point(e.linestring, e.p2) as p2_pos " +
      "  FROM " +
      "    ( SELECT " +
      "        ST_AsBinary(ST_GeomFromText(:linestring)) as linestring," +
      "        ST_Point(:p1_lon, :p1_lat) as p1," +
      "        ST_Point(:p2_lon, :p2_lat) as p2 " +
      "    ) as e " +
      ") as c;";

    public static Double getCandidatesRoutingLength(CandidatePoint startCandidate, CandidatePoint endCandidate) {
      Double distance = 0.0;
      // I) point A and B are on the same road segment, routing length
      //    is the portion of linestring between points

      if(startCandidate.nodedRoadSegment.equals(endCandidate.nodedRoadSegment)) {
        if(!startCandidate.getPoint().equals(endCandidate.getPoint())) {
          distance = distanceFollowingSegment(startCandidate.nodedRoadSegment.linestring, startCandidate.getPoint(), endCandidate.getPoint()).get(1);
          Logger.trace("Distance " + startCandidate + " - " + endCandidate + " - case I: " + distance + "m");
        }
        return distance;
      }

      // II) point A and B are on consecutive road segments, routing length
      //     are partials from each segment

      Long aSource = startCandidate.nodedRoadSegment.source;
      Long aTarget = startCandidate.nodedRoadSegment.target;
      Long bSource = endCandidate.nodedRoadSegment.source;
      Long bTarget = endCandidate.nodedRoadSegment.target;

      if(aSource == bSource) {
        distance = distanceFollowingSegment(endCandidate.nodedRoadSegment.linestring, endCandidate.getPoint(), endCandidate.getPoint()).get(0)
                 + distanceFollowingSegment(startCandidate.nodedRoadSegment.linestring, startCandidate.getPoint(), startCandidate.getPoint()).get(0);
        Logger.trace("Distance " + startCandidate + " - " + endCandidate + " - case II: " + distance + "m");
        return distance;
      } else if (aSource == bTarget) {
        distance = distanceFollowingSegment(endCandidate.nodedRoadSegment.linestring, endCandidate.getPoint(), endCandidate.getPoint()).get(2)
                 + distanceFollowingSegment(startCandidate.nodedRoadSegment.linestring, startCandidate.getPoint(), startCandidate.getPoint()).get(0);
        Logger.trace("Distance " + startCandidate + " - " + endCandidate + " - case II: " + distance + "m");
        return distance;
      } else if(aTarget == bSource) {
        distance = distanceFollowingSegment(startCandidate.nodedRoadSegment.linestring, startCandidate.getPoint(), startCandidate.getPoint()).get(2)
                 + distanceFollowingSegment(endCandidate.nodedRoadSegment.linestring, endCandidate.getPoint(), endCandidate.getPoint()).get(0);
        Logger.trace("Distance " + startCandidate + " - " + endCandidate + " - case II: " + distance + "m");
        return distance;
      } else if(aTarget == bTarget) {
        distance = distanceFollowingSegment(startCandidate.nodedRoadSegment.linestring, startCandidate.getPoint(), startCandidate.getPoint()).get(2)
                 + distanceFollowingSegment(endCandidate.nodedRoadSegment.linestring, endCandidate.getPoint(), endCandidate.getPoint()).get(2);
        Logger.trace("Distance " + startCandidate + " - " + endCandidate + " - case II: " + distance + "m");
        return distance;
      }

      // III) point A and B belongs to different non consecutive road segments, we
      //      need to compute the shortest path between the tho segments
      // TODO: implementation
      Set<Double> routes  = new HashSet<Double>();
      routes.add(distance(startCandidate.getPoint(), getVertexPoint(aSource)) + getRoutingLength(aSource, bSource) + distance(getVertexPoint(bSource), endCandidate.getPoint()));
      routes.add(distance(startCandidate.getPoint(), getVertexPoint(aSource)) + getRoutingLength(aSource, bTarget) + distance(getVertexPoint(bTarget), endCandidate.getPoint()));
      routes.add(distance(startCandidate.getPoint(), getVertexPoint(aTarget)) + getRoutingLength(aTarget, bSource) + distance(getVertexPoint(bSource), endCandidate.getPoint()));
      routes.add(distance(startCandidate.getPoint(), getVertexPoint(aTarget)) + getRoutingLength(aTarget, bTarget) + distance(getVertexPoint(bTarget), endCandidate.getPoint()));

      distance = Collections.min(routes);
      Logger.trace("Distance " + startCandidate + " - " + endCandidate + " - case III: " + distance + "m");
      return distance;
    }

    public static Double getRoutingLength(Long startVertex, Long endVertex) {
        Double cost = 0.0;
        if(startVertex == endVertex) {
            Logger.trace("Router " + startVertex + " - " + endVertex + " => " + cost + " (same)");
            return cost;
        }

        Double cacheCost = Cache.get("pgr_dijkstra_" + startVertex +"-"+endVertex, Double.class);
        if(cacheCost == null) {
            Query query = JPA.em().createNativeQuery(routingQuery).setParameter("edge_query", edgeQuery).setParameter("start_vertex", startVertex)
                    .setParameter("end_vertex", endVertex);
            for (Object res : query.getResultList()) {
                Object[] resArray = (Object[]) res;
                cost += (Double) resArray[3];
            }
            Cache.set("pgr_dijkstra_" + startVertex +"-"+endVertex, cost, "5mn");
            Logger.trace("Router " + startVertex + " - " + endVertex + " => " + cost);
            return  cost;
        } else {
            Logger.trace("Router " + startVertex + " - " + endVertex + " => " + cost + " (cached)");
            return cacheCost;
        }
    }

    public static Long getNearestVertex(Point point, NodedRoadSegment edge) {
        Point source = getVertexPoint(edge.source);
        Point target = getVertexPoint(edge.target);

        if(distance(point, source) < distance(point, target))
            return edge.source;
        else
            return edge.target;
    }

    public static Point getVertexPoint(Long id) {
        Point cacheVertexPoint = Cache.get("pgr_vertex_point_" + id, Point.class);
        if(cacheVertexPoint != null) {
            Logger.trace("Point for vertex " + id + " from cache.");
            return cacheVertexPoint;
        }

        Query query = JPA.em().createNativeQuery(vertexQuery).setParameter("id", id);
        String wkt = (String) query.getSingleResult();;
        WKTReader reader = new WKTReader();
        try {
            Point vertex = (Point) reader.read(wkt);
            Cache.set("pgr_vertex_point_" + id, vertex);
            return vertex;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

  public static Double distance (Point p1, Point p2) {
    try {
      return JTS.orthodromicDistance(p1.getCoordinate(), p2.getCoordinate(), DefaultGeographicCRS.WGS84);
    } catch (TransformException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static List<Double> distanceFollowingSegment (LineString linestring, Point p1, Point p2) {
    Query query = JPA.em().createNativeQuery(distanceBySegment)
        .setParameter("linestring", linestring.toString())
        .setParameter("p1_lon", p1.getX())
        .setParameter("p1_lat", p1.getY())
        .setParameter("p2_lon", p2.getX())
        .setParameter("p2_lat", p2.getY());

    Object[] result = (Object[]) query.getSingleResult();

    List<Double> l = new ArrayList<Double>();
    l.add((Double) result[0]);
    l.add((Double) result[1]);
    l.add((Double) result[2]);
    l.add((Double) result[3]);

    Logger.trace("Split Segment: " + result[0] + " - p1(" + result[4] + ") - " + result[1] + " - p2(" + result[5] + ") - " + result[2] + " = " + result[3]);

    return l;
    }

}
