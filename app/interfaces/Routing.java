package interfaces;

import models.boundaries.PathRoutes;

/**
 * Routing interface
 */
public interface Routing {
  public PathRoutes getRoute(String[] from, String[] to);
}
