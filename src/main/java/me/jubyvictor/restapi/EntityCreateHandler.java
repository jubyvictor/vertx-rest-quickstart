package me.jubyvictor.restapi;

//import com.intuit.cto.idp.services.rest.model.Entity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class EntityCreateHandler implements Handler<RoutingContext> {

    private final Vertx vertx;
    private static final Logger LOG = LoggerFactory.getLogger(EntityCreateHandler.class);
    private Timer processingTimer;
    private Counter requestCounter;
    private Counter successCounter;
    private Counter errorCounter;

    public EntityCreateHandler(Vertx vertx, Timer timer, Counter requestCounter, Counter successCounter, Counter errorCounter){
        this.vertx = vertx;
        this.processingTimer = timer;
        this.requestCounter = requestCounter;
        this.successCounter = successCounter;
        this.errorCounter = errorCounter;
    }


    @Override
    public void handle(RoutingContext routingContext) {
        long start = System.currentTimeMillis();
        try {
            this.requestCounter.increment();
            JsonObject entityAsJson = routingContext.getBodyAsJson();
            LOG.debug("Reading JSON entity took {} ms", (System.currentTimeMillis() - start));
            long decodeStart = System.currentTimeMillis();
            ThrowAwayEntity entity = Json.decodeValue(entityAsJson.encode(), ThrowAwayEntity.class);
            LOG.debug("Decoding JSON entity took {} ms", (System.currentTimeMillis() - decodeStart));
            LOG.debug("Attempting to create entity {}...", entity);
            JsonObject res = new JsonObject();
            res.put("status", "OK");
            routingContext.response().setStatusCode(201).end(res.encode());
            this.successCounter.increment();
        } catch (Exception e){
            this.errorCounter.increment();
            LOG.error("Unexpected error",e);
        }

        processingTimer.record(Duration.of((System.currentTimeMillis() - start), ChronoUnit.MILLIS));


    }
}
