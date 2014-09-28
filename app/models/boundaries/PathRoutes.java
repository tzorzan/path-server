package models.boundaries;

import java.util.List;

public class PathRoutes {
    public String type;
    public Feature[] features;

    public static class Feature {
        public String type;
        public Geometry geometry;
        public Properties properties;

        public Feature() {}
    }

    public static class Geometry {
        public String type;
        public List<Double[]> coordinates;

        public Geometry() {}
    }

    public static class Properties {
        public String comment;
        public Double distance;
        public Maneuver[] maneuvers;

        public Properties() {}
    }

    public static class Maneuver {
        public String narrative;
        public String iconUrl;
        public String[] streets;

        public Maneuver() {}
    }
}
