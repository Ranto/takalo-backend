package ara.project.takalo.shared.infrastructure.config;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JsonNullableConfig {

    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public JacksonModule jsonNullableModule() {
        SimpleModule module = new SimpleModule("JsonNullableModule", Version.unknownVersion());
        module.addDeserializer(JsonNullable.class, new JsonNullableDeserializer());
        module.addSerializer((Class) JsonNullable.class, new JsonNullableSerializer());
        return module;
    }

    static final class JsonNullableDeserializer extends ValueDeserializer<JsonNullable<?>> {

        private final ValueDeserializer<Object> innerDeser;

        JsonNullableDeserializer() {
            this(null);
        }

        private JsonNullableDeserializer(ValueDeserializer<Object> innerDeser) {
            this.innerDeser = innerDeser;
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            JavaType contextual = (property != null) ? property.getType() : ctxt.getContextualType();
            JavaType inner = (contextual != null && contextual.containedTypeCount() > 0)
                    ? contextual.containedType(0)
                    : ctxt.constructType(Object.class);
            return new JsonNullableDeserializer(ctxt.findContextualValueDeserializer(inner, property));
        }

        @Override
        public JsonNullable<?> deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return JsonNullable.of(null);
            }
            Object value = (innerDeser != null)
                    ? innerDeser.deserialize(p, ctxt)
                    : ctxt.readValue(p, Object.class);
            return JsonNullable.of(value);
        }

        @Override
        public Object getNullValue(DeserializationContext ctxt) {
            return JsonNullable.of(null);
        }

        @Override
        public Object getAbsentValue(DeserializationContext ctxt) {
            return JsonNullable.undefined();
        }
    }

    static final class JsonNullableSerializer extends ValueSerializer<JsonNullable<?>> {

        @Override
        public boolean isEmpty(SerializationContext ctxt, JsonNullable<?> value) {
            return value == null || !value.isPresent();
        }

        @Override
        public void serialize(JsonNullable<?> value, JsonGenerator gen, SerializationContext ctxt) {
            if (value == null || !value.isPresent() || value.get() == null) {
                gen.writeNull();
                return;
            }
            ctxt.writeValue(gen, value.get());
        }
    }
}
