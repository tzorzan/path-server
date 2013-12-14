package controllers;

import play.*;
import play.mvc.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import org.apache.commons.io.IOUtils;

import models.*;

public class Api extends Controller {
    public static void data() {
    	StringWriter writer = new StringWriter();
    	try {
			IOUtils.copy(request.body, writer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Logger.info(writer.toString());
    }

}