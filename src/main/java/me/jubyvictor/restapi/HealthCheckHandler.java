package me.jubyvictor.restapi;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckHandler implements Handler<RoutingContext> {

    private final Vertx vertx;

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckHandler.class);


    public HealthCheckHandler(Vertx vertx){
        this.vertx = vertx;
    }

    @Override
    public void handle(RoutingContext routingContext) {

        long start = System.currentTimeMillis();

        JsonObject healthResponse = new JsonObject();
        healthResponse.put("status", "OK");
        healthResponse.put("version", "1.0.0");

        routingContext.response().end(healthResponse.encode());

        LOG.info("Health response took {} ms",(System.currentTimeMillis() - start));
    }
}
