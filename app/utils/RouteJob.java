package utils;

import interfaces.Router;
import models.boundaries.PathRoutes;
import play.jobs.Job;

import java.util.Map;

/**
 * Utility class to make the routing an async Job.
 */
public class RouteJob extends Job<PathRoutes.Feature> {
  private Router router;
  private String[] from;
  private String[] to;
  private Map<String, Object> params;

  public RouteJob(Router router, String[] from, String[] to, Map<String, Object> params) {
    this.router = router;
    this.from = from;
    this.to = to;
    this.params = params;
  }

  @Override
  public PathRoutes.Feature doJobWithResult() throws Exception {
    return router.getRoute(from, to, params);
  }
}
