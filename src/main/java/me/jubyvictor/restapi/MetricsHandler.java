package me.jubyvictor.restapi;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public class MetricsHandler  implements Handler<RoutingContext> {

    private Vertx vertx;
    private PrometheusMeterRegistry meterRegistry;

    public MetricsHandler(Vertx vertx, PrometheusMeterRegistry meterRegistry){
        this.vertx = vertx;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.response().end(meterRegistry.scrape());
    }
}