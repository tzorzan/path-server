package controllers;

import play.*;
import play.mvc.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.io.IOUtils;

import models.*;

public class Api extends Controller {
	public static void data() {

		BufferedReader br = new BufferedReader(new InputStreamReader(
				request.body));
		SimpleDateFormat df =new SimpleDateFormat("yyyy:MM:dd'T'HH:mm:sszzz");
		String line;
		try {
			line = br.readLine();
			Path p = new Path();
			p.sent = new Date();
			p.score = Integer.parseInt(line);
			p.save();
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(" ");
				Sample s = new Sample();
				s.path = p;
				s.latitude = Double.parseDouble(tokens[0]);
				s.longitude = Double.parseDouble(tokens[1]);
				s.timestamp = df.parse(tokens[2]);
				s.save();
				
				Label ll = new Label();
				ll.type = Label.Type.LIGHT;
				ll.value = Double.parseDouble(tokens[3]);
				ll.sample = s;
				ll.save();

				Label ln = new Label();
				ln.type = Label.Type.NOISE;
				ln.value = Double.parseDouble(tokens[4]);
				ln.sample = s;
				ln.save();
				
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}