package com.controlm.iam.application;

import com.controlm.iam.domain.AuthenticatedUser;

/**
 * Application interface that the auth module calls to verify a username/password pair. Keeping it
 * in iam means the auth module never touches the {@code app_user} entity or its repository
 * directly.
 */
public interface AuthenticateCredentialsUseCase {

    /**
     * Verifies the credentials and returns the identity on success.
     *
     * @throws com.controlm.shared.error.ApiException with {@code UNAUTHENTICATED} for any failure —
     *     unknown user, wrong password, inactive or locked account — so the caller cannot tell them
     *     apart.
     */
    AuthenticatedUser authenticate(String username, String rawPassword);
}
