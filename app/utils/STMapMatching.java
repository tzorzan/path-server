package utils;

import com.vividsolutions.jts.geom.Point;
import org.apache.commons.math3.distribution.NormalDistribution;

import static com.vividsolutions.jts.operation.distance.DistanceOp.distance;

public class STMapMatching {

    private static Double mean = 0.0;
    private static Double standard_deviation = 20.0;

    public static Double observationProbability(Point sample, Point candidate) {
        NormalDistribution normalDistribution = new NormalDistribution(mean, standard_deviation);
        return normalDistribution.cumulativeProbability(distance(candidate, sample));
    }
}
