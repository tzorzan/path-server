package interfaces;

import models.boundaries.PathRoutes;

import java.util.List;
import java.util.Map;

/**
 * Router interface
 */
public interface Router {
  PathRoutes.Feature getRoute(String[] from, String[] to, Map<String, Object> params);
}
