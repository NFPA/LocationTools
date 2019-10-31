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

public class GeocoderAPIVerticle extends AbstractVerticle {

    private GeocodeWrapper geocodeWrapper;
    private JSONObject errorResponse;
    private String address;
    private Integer numRes;
    private OrderedJSONObject result;
    private JSONArray results;
    private static Logger logger = Logger.getLogger(GeocoderAPIVerticle.class);

    private void init() throws IOException, JSONException {
        geocodeWrapper = new GeocodeWrapper(config().getString("index.dir"));
        errorResponse = new JSONObject();

        result = new OrderedJSONObject();
        result.put("version", config().getString("api.version"));
    }

    @Override
    public void start(Future<Void> fut) throws IOException, JSONException {
        init();

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        Route route = router.route().path("/geocoder/v1/");

        route.handler(routingContext -> {
            address = routingContext.request().getParam("address");
            numRes = Integer.parseInt(routingContext.request().getParam("n"));
            logger.info("address: " + address + " | " + "number of results: " + numRes);

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");

            try {
                results = geocodeWrapper.getSearchJSONArray(address, numRes);
                result.put("input", address);
                result.put("results", results);

                response.end(
                        result.toString(4)
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