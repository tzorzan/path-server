package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import play.db.jpa.Model;

@Entity
public class Path extends Model {
	public Date sent;
	public Integer score;
	
	@OneToMany(mappedBy = "path")
	@OrderBy("timestamp ASC")
	public List<Sample> samples;
}
