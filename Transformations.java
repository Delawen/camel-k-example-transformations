// camel-k: language=java property-file=transformation.properties dependency=camel:jacksonxml dependency=camel:http
// camel-k: source=customizers/MongoCustomizer.java source=customizers/CSVCustomizer.java source=customizers/PostgreSQLCustomizer.java

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;

public class Transformations extends RouteBuilder {

@Override
  public void configure() throws Exception {

    //The following processors store relevant info as properties
    Processor processCsv = new CSVProcessor();
    Processor processXML = new XMLProcessor();

    //Preparing properties to build a GeoJSON Feature
    Processor processDB = new DBProcessor();

    //Just collects all features in a collection for the final GeoJSON
    Processor buildGeoJSON = new GeoJSONProcessor();

    //Aggregate all messages into one message with the list of bodies
    AggregationStrategy aggregationStrategy = new CollectToListStrategy();

    //This is the actual route
    from("timer:java?period=100000")
        // Reference URL for air quality e-Reporting on EEA
        // https://www.eea.europa.eu/data-and-maps/data/aqereporting-2

        //We start by reading our data.csv file, looping on each row
        .to("{{source.csv}}")
        .unmarshal("customCSV")
        .split(body()).streaming()

        //we store on exchange properties all the data we are interested in
        .process(processCsv)

        //on each row, we query an XML API service
        .setBody().constant("")
        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
        .setHeader(Exchange.HTTP_QUERY, simple("lat=${exchangeProperty.lat}&lon=${exchangeProperty.lon}&format=xml"))
        .to("https://nominatim.openstreetmap.org/reverse")
        .unmarshal().jacksonxml()

        //we store on exchange properties all the data we are interested in
        .process(processXML)

        //now we query the postgres database for more data
        .setBody().simple("SELECT info FROM descriptions WHERE id like '${exchangeProperty.pollutant}'")
        .to("jdbc:postgresBean?readSize=1")
        
        //we store on exchange properties all the data we are interested in
        .process(processDB)

        //we collect all rows into one message
        .aggregate(constant(true), aggregationStrategy)
        .completionSize(5)
        .process(buildGeoJSON)

        //and finally store the result on mongoDB
        .to("mongodb:mongoBean?database=example&collection=mySpatialObjects&operation=insert")
        
        //Write some log to know it finishes properly
        .log("Information stored")
        .to("log:info?showBodyType=true");
  }

  private final class CollectToListStrategy 
        extends AbstractListAggregationStrategy<Object> {
	@Override
      public Object getValue(Exchange exchange) {
       return exchange.getMessage().getBody();
      }
}

private final class GeoJSONProcessor implements Processor {
	@Override
      public void process(Exchange exchange) throws Exception {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("features", exchange.getMessage().getBody());
        res.put("type", "FeatureCollection");
        exchange.getIn().setBody(res);
      }
}

private final class DBProcessor implements Processor {
		@Override
		  public void process(Exchange exchange) throws Exception {
		    @SuppressWarnings("unchecked")
		    List<Object> body = exchange.getMessage().getBody(List.class);

		    Map<String, Object> outputBody = new HashMap<String, Object>();
		    outputBody.put("unit", exchange.getProperty("unit"));
		    outputBody.put("level", exchange.getProperty("level"));
		    outputBody.put("pollutant", exchange.getProperty("pollutant"));
        outputBody.put("address", exchange.getProperty("address"));
        
        //If we got any response from the DB, add it
		    if(body.size() > 0) {
		      outputBody.put("info", body.get(0).toString());
		    }

		    List<String> coordinates = new ArrayList<String>();
		    coordinates.add(exchange.getProperty("lat", "").toString());
		    coordinates.add(exchange.getProperty("lon", "").toString());

		    Map<String, Object> geometry = new HashMap<String, Object>();
		    geometry.put("type", "Point");
		    geometry.put("coordinates", coordinates);

		    Map<String, Object> res = new HashMap<String, Object>();
		    res.put("geometry", geometry);
		    res.put("properties", outputBody);
		    res.put("type", "Feature");

		    exchange.getIn().setBody(res);
		  }
	}

private final class XMLProcessor implements Processor {
		@Override
    public void process(Exchange exchange) throws Exception {
      @SuppressWarnings("unchecked")
      Map<String, String> body = exchange.getIn().getBody(Map.class);
      exchange.setProperty("address", body.get("addressparts"));
    }
}

private final class CSVProcessor implements Processor {
		@Override
		  public void process(Exchange exchange) throws Exception {
		    @SuppressWarnings("unchecked")
		    Map<String, String> body = exchange.getIn().getBody(Map.class);

		    if (body != null) {
		      extractValue(exchange, body, "Latitude of station", "lat");
		      extractValue(exchange, body, "Longitude of station", "lon");
		      extractValue(exchange, body, "Unit", "unit");
		      extractValue(exchange, body, "Air pollution level", "level");
		      extractValue(exchange, body, "Air pollutant", "pollutant");
		    }
		  }

		private void extractValue(Exchange exchange, Map<String, String> body, 
		                                        String param, String keyName) {
		    if (body.containsKey(param)) {
		      exchange.setProperty(keyName, body.get(param));
		    }
		  }
	}
}
