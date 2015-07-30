package models;

import com.vividsolutions.jts.geom.LineString;
import org.hibernate.annotations.Type;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class RoadSegment extends Model{
    @Type(type="org.hibernatespatial.GeometryUserType")
    public LineString linestring;

    public String name;

    @OneToMany(mappedBy = "roadSegment")
    public List<Sample> samples;
}
