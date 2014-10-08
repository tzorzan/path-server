package utils;

import com.vividsolutions.jts.geom.Point;

import static com.vividsolutions.jts.operation.distance.DistanceOp.distance;
import static java.lang.Math.*;

public class STMapMatching {

    private static Double standard_deviation = 20.0;

    public static Double observationProbability(Point sample, Point candidate) {
        Double value = (1 / sqrt(2 * PI) * sqrt(standard_deviation)) * exp( - (pow(distance(candidate, sample) - sqrt(standard_deviation), 2.0) / 2 * standard_deviation));
        return value;
    }
}
