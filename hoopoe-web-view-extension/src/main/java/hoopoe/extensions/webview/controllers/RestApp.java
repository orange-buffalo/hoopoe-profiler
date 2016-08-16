package hoopoe.extensions.webview.controllers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import hoopoe.api.HoopoeProfiler;
import java.io.IOException;
import java.time.ZonedDateTime;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ContextResolver;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/*")
public class RestApp extends ResourceConfig {

    public RestApp(HoopoeProfiler profiler) {
        register(ProfiledInvocationsController.class);
        register(JsonProcessingFeature.class);
        register(ObjectMapperProvider.class);
        register(new HoopoeProfilerBinder(profiler));
    }

    private static class HoopoeProfilerBinder extends AbstractBinder {

        private HoopoeProfiler profiler;

        private HoopoeProfilerBinder(HoopoeProfiler profiler) {
            this.profiler = profiler;
        }

        @Override
        protected void configure() {
            bind(profiler).to(HoopoeProfiler.class);
        }

    }

    private static class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

        private ObjectMapper objectMapper;

        public ObjectMapperProvider() {
            objectMapper = new ObjectMapper();

            SimpleModule hoopoeJacksonModule = new SimpleModule("HoopoeModule", new Version(1, 0, 0, null, null, null));

            hoopoeJacksonModule.addSerializer(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {
                @Override
                public void serialize(ZonedDateTime value,
                                      JsonGenerator gen,
                                      SerializerProvider serializers) throws IOException {
                    gen.writeString(String.valueOf(1000 * value.toEpochSecond()));
                }
            });

            objectMapper.registerModule(hoopoeJacksonModule);
        }

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return objectMapper;
        }

    }

}
