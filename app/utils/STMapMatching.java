package utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import models.CandidatePoint;
import org.apache.commons.math3.distribution.NormalDistribution;

import static com.vividsolutions.jts.operation.distance.DistanceOp.distance;

public class STMapMatching {

    private static Double mean = 0.0;
    private static Double standard_deviation = 20.0;

    public static Double observationProbability(CandidatePoint candidate) {
        NormalDistribution normalDistribution = new NormalDistribution(mean, standard_deviation);
        return normalDistribution.cumulativeProbability(distance(candidate.getPoint(), candidate.sample.getPoint()));
    }

    public static Double transmissionProbability(CandidatePoint candidateT, CandidatePoint candidateS) {
        GeometryFactory fact = new GeometryFactory();

        Point p_1 = candidateT.sample.getPoint();
        Point p = candidateS.sample.getPoint();
        Point c_t = candidateT.getPoint();
        Point c_s = candidateS.getPoint();

        //TODO:
        // set minimum path c_t c_s

        return distance(p_1, p) / distance(c_t, c_s);
    }

    public static Double spatialAnalysis(CandidatePoint candidateT, CandidatePoint candidateS) {
        return observationProbability(candidateS) * transmissionProbability(candidateT, candidateS);
    }
}
