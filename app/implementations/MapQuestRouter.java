package implementations;

import interfaces.Router;
import models.boundaries.PathRoutes;
import utils.MapQuestQuery;

import java.util.Arrays;
import java.util.List;

/**
 * Router implementation using MapQuest
 */
public class MapQuestRouter implements Router {

  @Override
  public List<PathRoutes.Feature> getRoute(String[] from, String[] to) {
    MapQuestQuery mqq = new MapQuestQuery(Double.valueOf(from[0]), Double.valueOf(from[1]),
        Double.valueOf(to[0]), Double.valueOf(to[1]));
    return Arrays.asList(mqq.query().features);
  }
}
