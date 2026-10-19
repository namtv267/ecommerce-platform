package com.shop.auth.adapter.out.security;

import com.shop.auth.application.port.out.AccessTokenIssuerPort;
import com.shop.auth.config.JwtProperties;
import com.shop.auth.domain.model.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
class JwtAccessTokenIssuer implements AccessTokenIssuerPort {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    JwtAccessTokenIssuer(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public IssuedAccessToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.accessTokenTtl());
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(user.id().toString())
                .id(jti)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", user.email().value())
                .claim("roles", List.of(user.role().name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, jti, expiresAt);
    }
}
