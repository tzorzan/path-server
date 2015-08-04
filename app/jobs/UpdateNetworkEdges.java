package jobs;

import play.Logger;
import play.db.jpa.JPA;
import play.jobs.Every;
import play.jobs.Job;

import javax.persistence.Query;

@Every("10m")
public class UpdateNetworkEdges extends Job {
  public static final Double tolerance = 0.000001;
	public void doJob() {
      updateNetworEdges();
    }

  public static final void updateNetworEdges() {
    /*
     *  Aggiorno le tabelle che rappresentano la Noded Network e la Network Topology
     */

    Logger.info("Updating network edges...");
    Query NodeNetworkQuery = JPA.em().createNativeQuery("SELECT pgr_nodeNetwork('roadsegment', :tolerance, 'id', 'linestring');").setParameter("tolerance", tolerance);
    Query CreateTopologyQuery = JPA.em().createNativeQuery("SELECT pgr_createTopology('roadsegment_noded', :tolerance, 'linestring');").setParameter("tolerance", tolerance);

    String result = (String) NodeNetworkQuery.getSingleResult();
    Logger.info("pgr_nodeNetwork: " + result);

    result = (String) CreateTopologyQuery.getSingleResult();
    Logger.info("pgr_createTopology: " + result);
  }
}
