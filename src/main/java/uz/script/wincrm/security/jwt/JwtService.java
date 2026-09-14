package uz.script.wincrm.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String SID_CLAIM = "sid";
    private static final String TYP_CLAIM = "typ";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(String username, Long sessionId) {
        return generateToken(username, jwtProperties.getAccessExpirationMs(), sessionId, TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(String username, Long sessionId) {
        return generateToken(username, jwtProperties.getRefreshExpirationMs(), sessionId, TOKEN_TYPE_REFRESH);
    }

    private String generateToken(String username, long expiration, Long sessionId, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim(SID_CLAIM, sessionId)
                .claim(TYP_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractSid(String token) {
        Object sid = extractAllClaims(token).get(SID_CLAIM);
        return sid == null ? null : Long.valueOf(sid.toString());
    }

    public String extractTokenType(String token) {
        Object typ = extractAllClaims(token).get(TYP_CLAIM);
        return typ == null ? null : typ.toString();
    }

    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(extractTokenType(token));
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public long getRemainingTime(String token) {
        return Math.max(
                extractExpiration(token).getTime() - System.currentTimeMillis(),
                0
        );
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
