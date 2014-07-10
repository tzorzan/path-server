package models;

import com.vividsolutions.jts.geom.LineString;
import org.hibernate.annotations.Type;
import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Segment extends Model{
    @Type(type="org.hibernatespatial.GeometryUserType")
    public LineString linestring;
}
