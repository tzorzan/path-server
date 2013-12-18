package models;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class Label extends Model {
	@Enumerated(EnumType.STRING)
	public Type type;
	
	public Double value = 0.0;
	
	@ManyToOne
	@JoinColumn(name = "sample_id")
	public Sample sample;

	public enum Type {
		LIGHT,
		NOISE
	}
}

