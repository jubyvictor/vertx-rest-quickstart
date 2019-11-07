package me.jubyvictor.restapi;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Vertx vertx;
    private HttpServer server;
    private Router router;
    private EntityCreateHandler createHandler;
    private HealthCheckHandler healthCheckHandler;
    private MetricsHandler metricsHandler;

    //Metrics for the application.
    private PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    private Counter requestCounter, successCounter, errorCounter;
    private Timer processingTimer;

    private HTTPRequestValidationHandler validationHandler;

    private JsonObject defaultRootResponse;

    private static final String ROOT = "/";
    private static final String CREATE_ENTITY = "/create";
    private static final String CREATE_ENTITY_HEALTH = "/health";
    private static final String CREATE_ENTITY_METRICS = "/metrics";

    private static final int PORT = 8080;


    public Main() {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        router = Router.router(vertx);

        requestCounter = registry.counter("requests");
        successCounter = registry.counter("success");
        errorCounter = registry.counter("errors");



        //new JvmMemoryMetrics().bindTo(registry);
        //new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);


        processingTimer = Timer
                .builder("request_processing_time")
                .publishPercentiles(0.5, 0.95) // median and 95th percentile
                .publishPercentileHistogram()
                .sla(Duration.ofMillis(8))
                .description("Create entity processing time.") // optional
                .register(registry);


        Metrics.addRegistry(registry);

        defaultRootResponse = new JsonObject();
        defaultRootResponse.put("status", "OK");

        validationHandler = validationHandler();

        createHandler = new EntityCreateHandler(vertx, processingTimer, requestCounter, successCounter, errorCounter);
        healthCheckHandler = new HealthCheckHandler(vertx);
        metricsHandler = new MetricsHandler(vertx, registry);

    }

    private void start() {
        //root
        router.get(ROOT).produces("application/json").handler(ctx -> {
            ctx.response().end(defaultRootResponse.encodePrettily());
        });

        //Entity Creation Handler, with validation and failure handlers
        router.put(CREATE_ENTITY)
                .handler(BodyHandler.create())
                .produces("application/json")
                .handler(validationHandler)
                .blockingHandler(createHandler)
                .failureHandler(ctxt -> {
                    this.errorCounter.increment();
                    Throwable failure = ctxt.failure();
                    if (failure instanceof ValidationException) {
                        // Something went wrong during validation!
                        String validationErrorMessage = failure.getMessage();
                        ctxt.response().setStatusCode(400).end(validationErrorMessage);
                    }
                });

        //Health check
        router.get(CREATE_ENTITY_HEALTH).method(HttpMethod.POST).blockingHandler(healthCheckHandler);

        //Metrics
        router.get(CREATE_ENTITY_METRICS).method(HttpMethod.POST).blockingHandler(metricsHandler);

        server.requestHandler(router).listen(PORT, httpServerAsyncResult -> {
            if (httpServerAsyncResult.succeeded()) {
                LOG.info("Started HTTP server on {} ", PORT);
            } else {
                LOG.error("Could not start HTTP server on {} ", PORT);
            }
        });
    }

    HTTPRequestValidationHandler validationHandler() {
        return HTTPRequestValidationHandler
                .create()
                .addHeaderParam("Content-Type", ParameterType.GENERIC_STRING, true)
                .addHeaderParam("Accept", ParameterType.GENERIC_STRING, true)
                .addHeaderParam("Authorization", ParameterType.GENERIC_STRING, true);

    }


    public static void main(String[] args) {
        LOG.info("Starting http API server!");
        Main self = new Main();
        self.start();
    }

}
