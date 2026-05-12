package com.nexevent.nexevent.utils;

import com.nexevent.nexevent.domains.entities.Ticket;
import com.nimbusds.jose.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
public class TicketQrUtil {

    private final JwtEncoder jwtEncoder;

    public TicketQrUtil(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    @Value("${ducthien.jwt.base64-secret}")
    private String jwtSecret;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtSecret).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    public String generateTicketQr(Ticket ticket) {
        Instant now = Instant.now();
        var eventEndTime = ticket.getOrderItem().getTicketType().getEvent().getEndTime();
        Instant validity = (eventEndTime != null)
                ? eventEndTime.atZone(ZoneId.systemDefault()).toInstant()
                : now.plus(30, java.time.temporal.ChronoUnit.DAYS);
        if (validity.isBefore(now)) {
            validity = now;
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(ticket.getId())
                .claim("orderCode", ticket.getOrderItem().getOrder().getOrderCode())
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String verifyAndGetTicketId(String qrToken) {
        try {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                    .macAlgorithm(JWT_ALGORITHM)
                    .build();
            Jwt decodedJwt = jwtDecoder.decode(qrToken);
            return decodedJwt.getSubject();
        } catch (Exception e) {
            System.out.println(">>> Lỗi quét QR Code: " + e.getMessage());
            throw new RuntimeException("Mã QR không hợp lệ, đã hết hạn hoặc bị làm giả!");
        }
    }
}