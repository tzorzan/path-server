package utils;

import play.Logger;
import play.cache.Cache;
import play.db.jpa.JPA;

import javax.persistence.Query;

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
}
