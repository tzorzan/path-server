package implementations;

import interfaces.Routing;
import models.boundaries.PathRoutes;
import utils.MapQuestQuery;

/**
 * Routing implementation using MapQuest
 */
public class MapQuestRouting implements Routing{

  @Override
  public PathRoutes getRoute(String[] from, String[] to) {
    MapQuestQuery mqq = new MapQuestQuery(Double.valueOf(from[0]), Double.valueOf(from[1]),
        Double.valueOf(to[0]), Double.valueOf(to[1]));
    return mqq.query();
  }
}
