import java.io.File
import java.util.Date
import scala.util.matching._
import scala.io.Source
import scala.collection.mutable._


val optReadPat = new Regex("returns option CONF-([^-]*)-[0-9]* value=(.*)");

val tracefile = args(0)
val opts = Map.empty[String,String]

def sanitize(s:String):String =
  if(s.startsWith("[") && s.endsWith("]"))
    s.substring(1, s.length -1)
	else if(s.startsWith("class "))
	  s.substring("class ".length)
	 else
	  s
	  
	  
for(line <- Source.fromFile(tracefile).getLines) {
	optReadPat.findFirstMatchIn(line) match {
		case Some(x) =>  { opts(x.group(1)) = sanitize(x.group(2))  }
		case None => ""
	}
}

for( (k,v) <- opts.elements) {
  if(v != "null")
	  Console.println(k + " = " + v)
}