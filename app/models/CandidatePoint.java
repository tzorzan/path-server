package models;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import play.db.jpa.Model;

import javax.persistence.*;

@Entity
public class CandidatePoint extends Model {
	
	public Double latitude;
	
	public Double longitude;

    @OneToOne(fetch= FetchType.LAZY)
    public Sample sample;

    @OneToOne(fetch= FetchType.LAZY)
    public NodedRoadSegment nodedRoadSegment;

    @Transient
    public Point getPoint() {
        return new GeometryFactory().createPoint(new Coordinate(this.longitude, this.latitude));
    }

    public CandidatePoint(Double longitude, Double latitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public CandidatePoint(Point point) {
        this.latitude = point.getY();
        this.longitude = point.getX();
    }

}
