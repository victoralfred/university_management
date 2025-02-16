package com.vickezi.registration.services;

import com.vickezi.globals.events.MessageProducerService;
import com.vickezi.globals.events.Status;
import com.vickezi.globals.model.RegistrationMessage;
import com.vickezi.registration.model.Users;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.InvalidKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.security.SignatureException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
@Service
@Import(MessageProducerService.class)
public class RegistrationServiceHandler {
    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceHandler.class);
    private final KeyPair keyPair;

    /**
     * Default constructor initializing the key pair for JWT signing.
     */
    public RegistrationServiceHandler() {
        this.keyPair = generateKeyPair();
    }

    /**
     * Registers a user by their email and generates a JWT token.
     *
     * @param email The email of the user.
     * @return A RegistrationMessage containing the registration token.
     * @throws RuntimeException if registration fails.
     */
    public RegistrationMessage registerUserByEmail(final String email) {
        return register(email).orElseThrow(() -> new RuntimeException("❌ Registration failed"));
    }

    /**
     * Generates an optional registration message containing a JWT token.
     *
     * @param email The email of the user.
     * @return An optional RegistrationMessage.
     */
    private Optional<RegistrationMessage> register(final String email) {
        return Optional.of(buildToken(email));
    }

    /**
     * Builds a JWT token containing email, expiry time, and status.
     *
     * @param email The email of the user.
     * @return RegistrationMessage object with the generated token.
     */
    private RegistrationMessage buildToken(final String email) throws InvalidKeyException {
        long expirationTime = 30 * 60 * 1000; // 30 minutes
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);

        String token = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.ES256)
                .compact();

        return new RegistrationMessage(UUID.randomUUID().toString(), token, Status.PENDING.getState(), email);
    }

    /**
     * Confirms if the email verification link is valid.
     *
     * @param token The JWT token received in the verification link.
     * @throws SignatureException if the token is invalid or expired.
     */
    public void confirmEmailLinkIsValid(final String token)  throws RuntimeException{
           try{
               Claims claims = parseToken(token);
               Users user = new Users();
               user.setEmail(objectToString(claims.getSubject()));
           }catch (SignatureException e){
               throw new RuntimeException(e);
           }
    }

    /**
     * Parses a JWT token and extracts claims.
     *
     * @param token The JWT token to be parsed.
     * @return Claims extracted from the token.
     * @throws RuntimeException if the token is invalid or expired.
     */
    private Claims parseToken(final String token) throws SignatureException {
            try{
                return Jwts.parserBuilder()
                        .setSigningKey(keyPair.getPublic())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            }catch (SignatureException e){
                throw new RuntimeException(e);
            }
    }

    /**
     * Generates an Elliptic Curve key pair for signing JWT tokens.
     *
     * @return A newly generated KeyPair.
     * @throws IllegalStateException if key pair generation fails.
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(256);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("❌ Failed to generate key pair", e);
        }
    }

    /**
     * Converts an object to a string representation.
     *
     * @param value The object to be converted.
     * @param <T> The type of the object.
     * @return The string representation of the object.
     */
    private <T> String objectToString(T value) {
        return String.valueOf(value).trim();
    }
}
