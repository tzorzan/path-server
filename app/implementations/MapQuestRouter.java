package implementations;

import interfaces.Router;
import models.boundaries.PathRoutes;
import utils.MapQuestQuery;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Router implementation using MapQuest
 */
public class MapQuestRouter implements Router {

  @Override
  public PathRoutes.Feature getRoute(String[] from, String[] to, Map<String, Object> params) {
    MapQuestQuery mqq = new MapQuestQuery(Double.valueOf(from[0]), Double.valueOf(from[1]),
        Double.valueOf(to[0]), Double.valueOf(to[1]));
    return mqq.query().features[0];
  }
}
