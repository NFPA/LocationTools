package org.nfpa.spatial;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.OrderedJSONObject;

import java.io.IOException;

public class APIVerticle extends AbstractVerticle {

    private GeocodeWrapper geocodeWrapper;
    private ReverseGeocodeWrapper reverseGeocodeWrapper;
    private JSONObject errorResponse;
    private String address;
    double ipLat, ipLon;
    float radiusKM;
    private Integer numRes;
    private OrderedJSONObject responseResult;
    private JSONArray searchResults;
    private static Logger logger = Logger.getLogger(APIVerticle.class);

    private void init() throws IOException, JSONException {
        geocodeWrapper = new GeocodeWrapper(config().getString("index.dir"));
        reverseGeocodeWrapper = new ReverseGeocodeWrapper(config().getString("index.dir"));
        errorResponse = new JSONObject();

        responseResult = new OrderedJSONObject();
        responseResult.put("version", config().getString("api.version"));
    }

    @Override
    public void start(Future<Void> fut) throws IOException, JSONException {
        init();

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        Route geocodingRoute = router.route().path("/geocoder/v1/");
        Route reverseGeocodingRoute = router.route().path("/reverse-geocoder/v1/");

        geocodingRoute.handler(routingContext -> {
            logger.info("GET geocoding");
            address = routingContext.request().getParam("address");
            numRes = Integer.parseInt(routingContext.request().getParam("n"));
            logger.info("address: " + address + " | " + "number of results: " + numRes);

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");

            try {
                searchResults = geocodeWrapper.getSearchJSONArray(address, numRes);
                responseResult.put("input", address);
                responseResult.put("results", searchResults);

                response.end(
                        responseResult.toString(4)
                );
            } catch(Exception e){
                try {
                    errorResponse.put("error", e.toString());
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                response.end(
                        errorResponse.toString()
                );
            }

        });

        reverseGeocodingRoute.handler(routingContext -> {
            logger.info("GET reverse geocoding");
            ipLat = Double.parseDouble(routingContext.request().getParam("lat"));
            ipLon = Double.parseDouble(routingContext.request().getParam("lon"));
            radiusKM = Float.parseFloat(routingContext.request().getParam("radius"));
            numRes = Integer.parseInt(routingContext.request().getParam("n"));
            logger.info("lat: " + ipLat + " | " + "lon: " + ipLon + " | " + "radiusKM: " + radiusKM);

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");

            try {
                searchResults = reverseGeocodeWrapper.getSearchJSONArray(ipLat, ipLon, radiusKM, numRes);
                responseResult.put("input", ipLat + ", " + ipLon);
                responseResult.put("results", searchResults);

                response.end(
                        responseResult.toString(4)
                );
            } catch(Exception e){
                try {
                    errorResponse.put("error", e.toString());
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                response.end(
                        errorResponse.toString()
                );
            }

        });

        server.requestHandler(router).listen(
                config().getInteger("http.port", 8080)
        );
    }
}