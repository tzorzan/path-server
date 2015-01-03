package utils;

import com.vividsolutions.jts.geom.Point;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.vividsolutions.jts.operation.distance.DistanceOp.distance;

public class PGRouting {
    private static String edgeQuery ="" +
            "SELECT " +
            "   id, " +
            "   source::integer, " +
            "   target::integer, " +
            "   ST_Length(ST_Transform(ST_SetSRID(linestring, 4326),2163)) as cost " +
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

    public static Double getCandidatesRoutingLength(CandidatePoint startCandidate, CandidatePoint endCandidate) {
      // I) point A and B are on the same road segment, routing length
      //    is the euclidean distance between points

      if(startCandidate.nodedRoadSegment.equals(endCandidate.nodedRoadSegment))
        return distance(startCandidate.getPoint(), endCandidate.getPoint());

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
        return distance(startCandidate.getPoint(), getVertexPoint(aVertex)) + distance(endCandidate.getPoint(), getVertexPoint(bVertex));
      }

      // III) point A and B belongs to different non consecutive road segments, we
      //      need to compute the shortest path between the tho segments
      // TODO: implementation
      Set<Double> routes  = new HashSet<Double>();
      routes.add(distance(startCandidate.getPoint(), getVertexPoint(aSource)) + getRoutingLength(aSource, bSource) + distance(getVertexPoint(bSource), endCandidate.getPoint()));
      routes.add(distance(startCandidate.getPoint(), getVertexPoint(aSource)) + getRoutingLength(aSource, bTarget) + distance(getVertexPoint(bTarget), endCandidate.getPoint()));
      routes.add(distance(startCandidate.getPoint(), getVertexPoint(aTarget)) + getRoutingLength(aTarget, bSource) + distance(getVertexPoint(bSource), endCandidate.getPoint()));
      routes.add(distance(startCandidate.getPoint(), getVertexPoint(aTarget)) + getRoutingLength(aTarget, bTarget) + distance(getVertexPoint(bTarget), endCandidate.getPoint()));

      return Collections.min(routes);
    }

    public static Double getRoutingLength(Long startVertex, Long endVertex) {
        Double cost = 0.0;
        if(startVertex == endVertex) {
            Logger.trace("Routing " + startVertex + " - " + endVertex + " => " + cost + " (same)");
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
            Logger.trace("Routing " + startVertex + " - " + endVertex + " => " + cost);
            return  cost;
        } else {
            Logger.trace("Routing " + startVertex + " - " + endVertex + " => " + cost + " (cached)");
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
