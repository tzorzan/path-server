package models.boundaries;

import java.util.ArrayList;
import java.util.List;

public class OverpassResponse {
    public String version;
    public String generator;
    public Element[] elements;

    public static class Element {
        public String type;
        public Long id;
        public Double lat;
        public Double lon;
        public Long[] nodes;

        public Element() {}
    };

    private List<Element> filterByType(String type) {
        List<Element> l = new ArrayList<Element>();
        for(OverpassResponse.Element e : elements) {
            if(e.type.equals(type)) {
                l.add(e);
            }
        }
        return l;
    }

    public List<Element> getNodes() {
        return filterByType("node");
    }

    public List<Element> getWays() {
        return filterByType("way");
    }

    public Element getElement(Long id) {
        for (Element e : elements)
            if (e.id.equals(id))
                return e;
        return null;
    }
}
