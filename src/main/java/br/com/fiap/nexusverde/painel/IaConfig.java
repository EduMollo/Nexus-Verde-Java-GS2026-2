package br.com.fiap.nexusverde.painel;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration(proxyBeanMethods = false)
class IaConfig {
    @Bean
    RestClientCustomizer tempoLimiteIa() {
        var transporte = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
        var requisicoes = new JdkClientHttpRequestFactory(transporte);
        requisicoes.setReadTimeout(Duration.ofSeconds(12));
        return builder -> builder.requestFactory(requisicoes);
    }
}
