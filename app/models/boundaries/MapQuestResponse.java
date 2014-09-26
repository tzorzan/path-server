package models.boundaries;

public class MapQuestResponse {
    public Route route;

    public static class Route {
        public String sessionId;
        public Double distance;
        public BoundingBox boundingBox;
        public Shape shape;
        public Leg[] legs;

        public Route() {}
    }

    public static class BoundingBox {
        public Coordinate ul;
        public Coordinate lr;

        public BoundingBox() {}
    }

    public static class Coordinate {
        public Double lng;
        public Double lat;

        public Coordinate() {}
    }

    public static class Shape {
        public Integer[] maneuverIndexes;
        public Double[] shapePoints;

        public Shape() {}
    }

    public static class Leg {
        public Maneuver[] maneuvers;

        public Leg() {}
    }

    public static class Maneuver {
        public String narrative;
        public String iconUrl;
        public String[] streets;

        public Maneuver() {}
    }
}
