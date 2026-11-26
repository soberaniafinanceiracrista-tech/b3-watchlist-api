package flavio.API;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("B3 Watchlist API")
                        .version("1.0")
                        .description("API pessoal para consulta de cotações e variações da B3"))
                .servers(List.of(new Server().url("/").description("Servidor Atual")));
    }
}
