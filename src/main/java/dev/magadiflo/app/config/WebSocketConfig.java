package dev.magadiflo.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/web-socket") // WebSocket endpoint al que se conectarán los clientes
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper());
        converter.setContentTypeResolver(resolver);

        messageConverters.add(converter);
        return false; // No queremos usar los valores predeterminados sino el que acabamos de configurar
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        /**
         * Habilitar un simple broker (intermediario) de mensajes en memoria para llevar mensajes de vuelta al cliente
         * en destinos prefijados (por ejemplo, destinos con el prefijo "/topic"). En nuestro caso definimos
         * el prefijo /user.
         */
        registry.enableSimpleBroker("/user");

        // Designa el prefijo "/app" para mensajes vinculados a métodos anotados con @MessageMapping en el controlador.
        registry.setApplicationDestinationPrefixes("/app");

        /**
         * Prefijo utilizado para identificar los destinos de usuario. Los destinos de usuario permiten que un usuario
         * se suscriba a nombres de colas exclusivos de su sesión y que otros usuarios envíen mensajes a esas colas
         * exclusivas y específicas del usuario.
         */
        registry.setUserDestinationPrefix("/user");
    }
}
