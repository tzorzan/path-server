package jobs;

import play.Logger;
import play.jobs.Every;
import play.jobs.Job;

@Every("15s")
public class CheckSamplesForTrajectories extends Job {
	public void doJob() {
        Logger.info("Check for new trajectories from samples...");
    }
}
