package models;

import javax.persistence.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
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
	
	@OneToMany(mappedBy = "sample")
	public List<Label> labels;

  @ManyToOne
  @JoinColumn(name = "roadSegment_id")
  public RoadSegment roadSegment;

  @Transient
  public Point getPoint() {
    GeometryFactory gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
      return gf.createPoint(new Coordinate(this.latitude, this.longitude));
  }
}
