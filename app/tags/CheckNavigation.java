package tags;

import groovy.lang.Closure;
import play.mvc.Http;
import play.mvc.Router;
import play.templates.FastTags;
import play.templates.GroovyTemplate;
import play.templates.JavaExtensions;

import java.io.PrintWriter;
import java.util.Map;

public class CheckNavigation extends FastTags {
  public static void _checknav(Map<?, ?> args, Closure body, PrintWriter out,
                              GroovyTemplate.ExecutableTemplate template, int fromLine) {
    String request = Router.reverse(((Http.Request) args.get("arg")).action).toString();
    String action = JavaExtensions.toString(body);

    out.print(request.equals(action)?"active":"");
  }
}
