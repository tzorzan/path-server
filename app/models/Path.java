package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

import play.db.jpa.Model;

@Entity
public class Path extends Model {
	public Date sent;
	public Integer score;
	
	@OneToMany(mappedBy = "path")
	public List<Sample> samples;
}
