package jobs;

import controllers.MapMatching;
import models.Path;
import models.Sample;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;

@Every("60s")
public class ProcessSamples extends Job {

  public void doJob() {
    Sample s = Sample.find("roadsegment_id is null order by id").first();
    if (s != null) {
      Path path = s.path;
      Logger.debug("Elaboro percorso " + path.id + ".");

      MapMatching.doStep1(path);
      MapMatching.doStep2(path);
      MapMatching.doStep3(path);
    }
  }

}
