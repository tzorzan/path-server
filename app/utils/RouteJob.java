package utils;

import interfaces.Router;
import models.boundaries.PathRoutes;
import play.jobs.Job;

/**
 * Utility class to make the routing an async Job.
 */
public class RouteJob extends Job<PathRoutes.Feature> {
  private Router router;
  private String[] from;
  private String[] to;

  public RouteJob(Router router, String[] from, String[] to) {
    this.router = router;
    this.from = from;
    this.to = to;
  }

  @Override
  public PathRoutes.Feature doJobWithResult() throws Exception {
    return router.getRoute(from, to);
  }
}
