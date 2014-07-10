package jobs;

import play.Logger;
import play.jobs.Every;
import play.jobs.Job;

@Every("1h")
public class CheckSamplesForTrajectories extends Job {
	public void doJob() {
        Logger.info("Check for new trajectories from samples...");
        /*
         * La ricerca della traiettoria si basa su un algoritmo di Map Matching. 
         * Dalla lista dei sample, e le informazioni sulle rotte esistenti 
         * (Road Data di OpenstreetMap e storico) mappa i corrispondenti elementi (Nodi, Segmenti).
         */
        
        
    }
}
