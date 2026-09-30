package sv.edu.udb.pokebattle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean RestClient pokeApiRestClient(RestClient.Builder builder, @Value("${pokeapi.base-url}") String baseUrl) { return builder.baseUrl(baseUrl).build(); }
}
