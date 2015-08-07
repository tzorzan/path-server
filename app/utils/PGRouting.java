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

    private static String candidatesLengthQuery = "" +
        "SELECT " +
        "  ST_Length(ST_Transform(ST_SetSRID(ST_Line_Substring(c.linestring, " +
        "    CASE WHEN c.s_pos < c.e_pos THEN c.s_pos ELSE c.e_pos END, " +
        "    CASE WHEN c.s_pos < c.e_pos THEN c.e_pos ELSE c.s_pos END), 4326),2163)) as par_length " +
        "FROM ( " +
        "  SELECT" +
        "    r.linestring as linestring, " +
        "    ST_Line_Locate_Point(r.linestring, ST_Point(s.longitude, s.latitude)) as s_pos,  " +
        "    ST_Line_Locate_Point(r.linestring, ST_Point(e.longitude, e.latitude)) as e_pos " +
        "FROM (select * from candidatepoint where id = :s_id) as s " +
        "JOIN (select * from candidatepoint where id = :e_id) as e on 1=1 " +
        "JOIN roadsegment_noded as r on s.nodedroadsegment_id = r.id " +
        ") as c;";

    public static Double getCandidatesRoutingLength(CandidatePoint startCandidate, CandidatePoint endCandidate) {
      Double distance = 0.0;
      // I) point A and B are on the same road segment, routing length
      //    is the portion of linestring between points

      if(startCandidate.nodedRoadSegment.equals(endCandidate.nodedRoadSegment)) {
        Query query = JPA.em().createNativeQuery(candidatesLengthQuery).setParameter("s_id", startCandidate.id).setParameter("e_id", endCandidate.id);
        if(!startCandidate.getPoint().equals(endCandidate.getPoint())) {
          distance = (Double) query.getSingleResult();
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
      Long aVertex, bVertex;

      if(aSource == bSource) {
        aVertex = aSource;
        bVertex = bSource;
      } else if (aSource == bTarget) {
        aVertex = aSource;
        bVertex = bTarget;
      } else if(aTarget == bSource) {
        aVertex = aTarget;
        bVertex = bSource;
      } else if(aTarget == bTarget) {
        aVertex = aTarget;
        bVertex = bTarget;
      } else {
        aVertex = null;
        bVertex = null;
      }

      if(aVertex != null && bVertex != null){
        distance = distance(startCandidate.getPoint(), getVertexPoint(aVertex)) + distance(endCandidate.getPoint(), getVertexPoint(bVertex));
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

}
