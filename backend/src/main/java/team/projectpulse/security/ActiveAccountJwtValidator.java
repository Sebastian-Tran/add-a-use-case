package team.projectpulse.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

/**
 * Refuses a token whose user is deactivated or no longer exists (FR-SEC-active-account).
 *
 * <p>A token is self-contained and lives for two hours, and there is no revocation list, so a signature and expiry
 * check alone would let a student deactivated mid-session (UC-STU-deactivate-student) keep using the token she
 * logged in with. This validator re-reads the account on every request instead. It runs in the JWT decoder's
 * validator chain, so a refusal is an invalid token: the resource server raises {@code InvalidBearerTokenException}
 * and {@code CustomBearerTokenAuthenticationEntryPoint} answers 401, which sends the SPA back to the login page.
 *
 * <p>The cost is one primary-key read per authenticated request, accepted in {@code docs/design/stu.md}.
 */
@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INACTIVE_ACCOUNT = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
            "The account this token was issued to is deactivated or no longer exists.", null);

    private final UserRepository userRepository;


    public ActiveAccountJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Integer userId = readUserId(jwt.getClaim("userId"));
        boolean active = userId != null && this.userRepository.findById(userId)
                .map(PeerEvaluationUser::isEnabled)
                .orElse(false);
        return active ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
    }

    /**
     * The claim is written as an Integer and may decode as a Long; anything else names no account.
     */
    private static Integer readUserId(Object claim) {
        if (claim instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

}
