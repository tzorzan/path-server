package controllers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.*;
import models.Path;
import models.Sample;
import models.Segment;
import models.boundaries.OverpassResponse;
import org.postgis.PGgeometry;
import play.Logger;
import play.db.jpa.JPA;
import play.mvc.Controller;
import utils.OverpassQuery;

public class MapMatching extends Controller {
    public static void step1() {
    	Sample sample = Sample.find("byLoaded", false).first();
    	Path path = sample.path;
 
    	List<Sample> samples = path.samples;

            Segment s = new Segment();
            GeometryFactory fact = new GeometryFactory();
            Coordinate[] coords  =
                    new Coordinate[] {new Coordinate(0, 2), new Coordinate(2, 0), new Coordinate(8, 6) };
            s.linestring = fact.createLineString(coords);

            //Segment s2 = Segment.findById(61l);
            //Logger.info("Segmento: " + s2.linestring.toText());
            //s.save();

        String query = "[out:json];\n" +
                "( way\n" +
                " (45.413678,11.813972,45.414751,11.817132)\n" +
                "  [highway];\n" +
                "  >; );\n" +
                "out \n" +
                "  body;";

        OverpassResponse r = new OverpassQuery(query).query();

        List<LineString> segments = new ArrayList<LineString>();

        for(OverpassResponse.Element e : r.getWays()) {
            List<Coordinate> points  = new ArrayList<Coordinate>();

            for(Long id : e.nodes) {
                OverpassResponse.Element pe = r.getElement(id);
                if (pe != null)
                    points.add(new Coordinate(pe.lat, pe.lon));
            }

            segments.add(fact.createLineString(points.toArray(new Coordinate[points.size()])));
        }

        Envelope boundingbox = new Envelope(45.414751, 45.413678, 11.813972, 11.817132);
        render(samples, boundingbox, segments);

/*
SELECT
    boxes.id as id,
    ST_YMin(boxes.box) as minlat,
    ST_XMin(boxes.box) as minlon,
    ST_YMax(boxes.box) as maxlat,
    ST_XMax(boxes.box) as maxlon
FROM
    (SELECT    
        ST_Transform(ST_Expand(ST_Transform(ST_SetSRID(ST_Point(s.longitude, s.latitude),4326), 2163), 20), 4326) as box, 
        s.id AS id 
    FROM 
        sample AS s
    WHERE
	s.loaded=false
    ) AS boxes 
ORDER BY id;         
*/
        
/*
( way
  (45.4139338116182,11.8150751555019,45.4143931983618,11.8156815378993)
  [highway];
  >; );
out 
  body;

( way
  (45.4141623996377,11.8144783476189,45.4147623996377,11.8169314805713)
  [highway];
  >; );
out
  body;

*/

    }
}
