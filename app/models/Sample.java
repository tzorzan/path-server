package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import play.db.jpa.Model;

import java.util.Date;
import java.util.List;

@Entity
public class Sample extends Model {
	@ManyToOne
	@JoinColumn(name = "path_id")
	public Path path;
	
	public Date timestamp;
	
	public Double latitude;
	
	public Double longitude;
	
	public Boolean loaded = false;
	
	@OneToMany(mappedBy = "sample")
	public List<Label> labels;
}
