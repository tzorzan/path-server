package interfaces;

import models.boundaries.PathRoutes;

import java.util.List;

/**
 * Router interface
 */
public interface Router {
  public PathRoutes.Feature getRoute(String[] from, String[] to);
}
