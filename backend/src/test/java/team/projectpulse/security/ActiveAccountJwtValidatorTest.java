package team.projectpulse.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtValidatorTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ActiveAccountJwtValidator validator;


    private static Jwt tokenFor(Object userId) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject("someone")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.build();
    }

    @Test
    void testActiveAccountIsAccepted() {
        given(this.userRepository.findById(1)).willReturn(Optional.of(new Student("john", "John", "Smith", "j.smith@abc.edu", "123456", true, "student")));

        assertThat(this.validator.validate(tokenFor(1L)).hasErrors()).isFalse();
    }

    @Test
    void testDeactivatedAccountIsRefused() {
        given(this.userRepository.findById(1)).willReturn(Optional.of(new Student("john", "John", "Smith", "j.smith@abc.edu", "123456", false, "student")));

        assertThat(this.validator.validate(tokenFor(1)).hasErrors()).isTrue();
    }

    @Test
    void testAccountThatNoLongerExistsIsRefused() {
        given(this.userRepository.findById(1)).willReturn(Optional.empty());

        assertThat(this.validator.validate(tokenFor(1)).hasErrors()).isTrue();
    }

    @Test
    void testTokenWithoutUserIdIsRefused() {
        // Any lookup would find an active account, so only the missing-userId check can refuse this token. Without this
        // stub the mock's default answer (Optional.empty()) refused it anyway, and the test passed with the check deleted.
        // lenient(): the validator under test never reaches the repository, which strict stubbing would otherwise reject.
        lenient().when(this.userRepository.findById(any())).thenReturn(Optional.of(new Student("john", "John", "Smith", "j.smith@abc.edu", "123456", true, "student")));

        assertThat(this.validator.validate(tokenFor(null)).hasErrors()).isTrue();
    }

}
