package models;

import com.vividsolutions.jts.geom.LineString;
import org.hibernate.annotations.Type;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="roadsegment_noded")
public class NodedRoadSegment extends Model{
    @ManyToOne
    @JoinColumn(name = "old_id")
    public RoadSegment roadSegment;

    @Column(name = "sub_id")
    public Long segment;

    public Long source;

    public Long target;

    @Type(type="org.hibernatespatial.GeometryUserType")
    public LineString linestring;
}
