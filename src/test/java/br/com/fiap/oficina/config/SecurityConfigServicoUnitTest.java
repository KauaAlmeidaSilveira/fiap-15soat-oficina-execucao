package br.com.fiap.oficina.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigServicoUnitTest {

    private static JwtEncoder encoder;
    private static JwtDecoder decoder;

    @BeforeAll
    static void chaves() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair par = generator.generateKeyPair();
        RSAPublicKey publica = (RSAPublicKey) par.getPublic();
        JWK jwk = new RSAKey.Builder(publica).privateKey((RSAPrivateKey) par.getPrivate()).build();
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        decoder = SecurityConfig.servicoJwtDecoder(publica, "oficina-os");
    }

    private String token(String emissor, String audiencia) {
        Instant agora = Instant.now();
        return encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer(emissor).audience(List.of(audiencia)).subject("oficina-os")
                .issuedAt(agora).expiresAt(agora.plusSeconds(60)).build())).getTokenValue();
    }

    @Test
    @DisplayName("Aceita token do OS Service destinado à Execução")
    void aceitaTokenDeServico() {
        assertThat(decoder.decode(token("oficina-os", SecurityConfig.AUDIENCIA_SERVICO)).getSubject()).isEqualTo("oficina-os");
    }

    @Test
    @DisplayName("Rejeita token de sessão e token de outro emissor nas rotas internas")
    void rejeitaOutrosTokens() {
        String sessao = token("oficina-os", SecurityConfig.AUDIENCIA_SESSAO);
        String outroEmissor = token("oficina-billing", SecurityConfig.AUDIENCIA_SERVICO);

        assertThatThrownBy(() -> decoder.decode(sessao)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(outroEmissor)).isInstanceOf(JwtException.class);
    }
}
