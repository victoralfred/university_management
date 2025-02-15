package com.vickezi.registration.services;

import com.vickezi.globals.events.Status;
import com.vickezi.registration.model.RegistrationMessage;
import com.vickezi.registration.model.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
@Service
public class RegistrationServiceHandler {
    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceHandler.class);
    private final KeyPair keyPair = generateKeyPair();

    public RegistrationServiceHandler() throws NoSuchAlgorithmException {
    }
    public RegistrationMessage registerUserByEmail(final String email) throws NoSuchAlgorithmException {
        return register(email).orElseThrow();
    }
    private Optional<RegistrationMessage> register(final String email) throws NoSuchAlgorithmException {
        return Optional.of(buildToken(email));
    }
    /**
     * Build a JWT token of email, expiry and created timeline, and {@link Status}
     * @param email Email
     * @return an object of Registration containing the email token
     */
    private RegistrationMessage buildToken(final String email) {
        long expirationTime = 30 * 40 * 1000; // Expires in 30 minutes
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);
        String token = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(),
                        SignatureAlgorithm.ES256)
                .compact();
        return new RegistrationMessage( UUID.randomUUID().toString(), token,
                Status.PENDING.getState(), email);
    }
    public void confirmEmailLinkIsValid(final String token) throws RuntimeException {
        try{
            Claims claims = parseToken(token);
            // Check if token expired
            Users user = new Users();
            user.setEmail(objectTOsString(claims.get("sub")));
            log.info("User is {}", user);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    private Claims parseToken(final String token) throws RuntimeException{
        try {
            return (Claims) Jwts.parserBuilder()
                    .setSigningKey(keyPair.getPublic())
                    .build()
                    .parse(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e); // used for checking message type is jwt by queue, and endure its removed by ack the message
        }
    }
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256); // Use a 256-bit key size
        return keyPairGenerator.generateKeyPair();
    }
    private <T>String objectTOsString(T t){
        return String.valueOf(t).trim();
    }
}
