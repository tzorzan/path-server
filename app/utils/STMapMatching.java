package utils;

import com.vividsolutions.jts.geom.Point;
import models.CandidatePoint;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import play.Logger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class STMapMatching {

    private static Double mean = 0.0;
    private static Double standard_deviation = 20.0;

    public static Double observationProbability(CandidatePoint candidate) {
        NormalDistribution normalDistribution = new NormalDistribution(mean, standard_deviation);
        Double value = 1 - normalDistribution.cumulativeProbability(PGRouting.distance(candidate.getPoint(), candidate.sample.getPoint()));
        Logger.trace("Observation probabilty for " + candidate.sample + " - " + candidate + " : " + value);
        return value;
    }

    public static Double transmissionProbability(CandidatePoint candidateT, CandidatePoint candidateS) {
        DecimalFormat df = new DecimalFormat("#.########");

        Point p_1 = candidateT.sample.getPoint();
        Point p = candidateS.sample.getPoint();
        Double eucl_distance = PGRouting.distance(p_1, p);

        Double shortest_path = PGRouting.getCandidatesRoutingLength(candidateT, candidateS);

        Double value = eucl_distance / shortest_path;
        Logger.trace("Transmission probabilty " + candidateT + " - " + candidateS + " = " + df.format(eucl_distance) + " / " + df.format(shortest_path) + " = " + df.format(value));
        return value;
    }

    public static Double spatialAnalysis(CandidatePoint candidateT, CandidatePoint candidateS) {
        Double value = observationProbability(candidateS) * transmissionProbability(candidateT, candidateS);
        Logger.trace("Spatial Analysis " + candidateT + " - " + candidateS + " : " + value);
        return value;
    }

    public static List<CandidatePoint> findMatch(List<List<CandidatePoint>> candidatesGraph) {
        List<List<Double>> f = new ArrayList<List<Double>>();
        List<List<CandidatePoint>> pre = new ArrayList<List<CandidatePoint>>();

        // Inizializzo f[c_1_s] considerando solo Observation Probability.
        List<Double> f_1 = new ArrayList<Double>();
        for(CandidatePoint c_s : candidatesGraph.get(0)) {
            f_1.add(observationProbability(c_s));
        }
        f.add(f_1);

        // Valorizzo tutto f[] e pre[]
        for(int i=1; i < candidatesGraph.size(); i++) {
            List<Double> f_c_s = new ArrayList<Double>();
            List<CandidatePoint> pre_c_s = new ArrayList<CandidatePoint>();
            for(int s = 0; s < candidatesGraph.get(i).size(); s++) {
                CandidatePoint c_s = candidatesGraph.get(i).get(s);
                Double max = -1.0;
                pre_c_s.add(null);
                f_c_s.add(null);
                for(int t = 0; t < candidatesGraph.get(i-1).size(); t++) {
                    CandidatePoint c_t = candidatesGraph.get(i-1).get(t);
                    Double F_t_s = spatialAnalysis(c_t, c_s);
                    Double alt = f.get(i-1).get(t) + F_t_s;
                    Logger.trace("alt = f[c_" + (i-1) + "_" + t + "] + F(c_" + (i-1) + "_" + t + " -> c_" + i + "_" +
                            s + " ) = " +  f.get(i-1).get(t) + " + " + F_t_s + " = " + alt );
                    if (alt > max) {
                        max = alt;
                        pre_c_s.set(s, c_t);
                        Logger.trace("New max (" + max + "), now pre[c_" + i + "_" + s + "]=" + c_t);
                    }
                    f_c_s.set(s, max);
                }
            }
            f.add(f_c_s);
            pre.add(pre_c_s);
        }

        // Costruisco la lista con il match
        List<CandidatePoint> rList = new ArrayList<CandidatePoint>();

        Double f_max = Collections.max(f.get(f.size()-1));
        int index = f.get(f.size()-1).indexOf(f_max);
        CandidatePoint c = candidatesGraph.get(candidatesGraph.size()-1).get(index);

        for(int i=pre.size()-1; i >= 0 ; i--) {
            rList.add(c);
            c = pre.get(i).get(index);
            index = candidatesGraph.get(i).indexOf(c);
        }

        rList.add(c);

        Collections.reverse(rList);

        Logger.debug("Matched sequence: [" + StringUtils.join(rList, ", ") + "]");
    return rList;
    }
}
