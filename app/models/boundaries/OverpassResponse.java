package models.boundaries;

public class OverpassResponse {
    public String version;
    public String generator;
    public Element[] elements;

    public static class Element {
        public String type;
        public Long id;
        public Double lat;
        public Double lon;

        public Element() {}
    };

}
