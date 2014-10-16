package models;

import javax.persistence.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import play.db.jpa.Model;

import java.util.Date;
import java.util.List;

@Entity
public class Sample extends Model {
	@ManyToOne
	@JoinColumn(name = "path_id")
	public Path path;

    public String uuid;
	
	public Date timestamp;
	
	public Double latitude;
	
	public Double longitude;

    public Double accuracy;
	
	public Boolean loaded = false;
	
	@OneToMany(mappedBy = "sample")
	public List<Label> labels;

    @ManyToOne
    @JoinColumn(name = "roadSegment_id")
    public RoadSegment roadSegment;

    @Transient
    public Point getPoint() {
        return new GeometryFactory().createPoint(new Coordinate(this.latitude, this.longitude));
    }
}
