package com.nexevent.nexevent.utils;

import com.nexevent.nexevent.domains.dto.request.ResLoginDTO;
import com.nimbusds.jose.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SecurityUtil {
    private final JwtEncoder jwtEncoder;
    public SecurityUtil(final JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    @Value("${ducthien.jwt.base64-secret}")
    private String jwtKey;

    @Value("${ducthien.jwt.access-token-validity-in-seconds}")
    private long accessTokenExpiration;

    @Value("${ducthien.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }
    public String createToken(Authentication auth, ResLoginDTO dto){

        ResLoginDTO.UserInsideToken userInsideToken=new ResLoginDTO.UserInsideToken();
        userInsideToken.setId(dto.getUserLogin().getId());
        userInsideToken.setEmail(dto.getUserLogin().getEmail());
        userInsideToken.setName(dto.getUserLogin().getName());
        Instant now = Instant.now();
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);

        // @formatter:off
        List<String> listAuthority = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(auth.getName())
                .claim("permission", listAuthority)
                .claim("user", userInsideToken)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String createRefreshToken(String email,ResLoginDTO dto)
    {
        Instant now = Instant.now();
        Instant validity = now.plus(this.refreshTokenExpiration, ChronoUnit.SECONDS);

        ResLoginDTO.UserInsideToken userInsideToken=new ResLoginDTO.UserInsideToken();
        userInsideToken.setId(dto.getUserLogin().getId());
        userInsideToken.setEmail(dto.getUserLogin().getEmail());
        userInsideToken.setName(dto.getUserLogin().getName());
//Body
// @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user", userInsideToken)
                .build();
        //header
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,
                claims)).getTokenValue();
    }

    public Jwt checkValidRefreshToken(String token)
    {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();
        try{
            return jwtDecoder.decode(token);
        }catch (Exception e)
        {
            System.out.println(">>> Refresh Token error: "+e.getMessage());
            throw e;
        }
    }

}
