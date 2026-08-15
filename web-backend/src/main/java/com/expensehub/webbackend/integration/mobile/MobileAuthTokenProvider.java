package com.expensehub.webbackend.integration.mobile;

import com.expensehub.webbackend.security.JwtUtil;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
public class MobileAuthTokenProvider {

    private static final Map<String, String> WEB_ROLE_TO_MOBILE_ROLE =
            Map.of(
                    "FINANCE_STAFF", "FINANCE",
                    "ADMIN", "ADMIN",
                    "MANAGER", "MANAGER",
                    "EMPLOYEE", "EMPLOYEE");

    // Dedicated Mobile-side account (registered via Mobile's own /api/auth/register)
    // used ONLY for calls that require Mobile's UserService.getUserById() to resolve
    // the JWT subject to a real Mobile user record. The currently-authenticated Web
    // user's email has no corresponding Mobile account, so a token minted with that
    // subject is rejected with "Not authenticated" even though the signature itself
    // is trusted - Mobile's endpoint does an extra lookup-by-subject on top of
    // signature validation. See commit message for the full diagnosis.
    private static final String MOBILE_SERVICE_ACCOUNT_USERNAME = "web-integration-service";
    // MANAGER so the service account can call Mobile's admin endpoints
    // (GET /api/admin/trips) from background jobs that have no logged-in Web
    // user context - EMPLOYEE would be rejected by the role check.
    private static final String MOBILE_SERVICE_ACCOUNT_ROLE = "MANAGER";

    private final JwtUtil jwtUtil;

    public MobileAuthTokenProvider(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String currentUserTokenForMobile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated Web user to act on behalf of");
        }
        String email = authentication.getName();
        String webRole =
                authentication.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                        .orElseThrow(() -> new IllegalStateException("Authenticated user has no role"));
        String mobileRole = WEB_ROLE_TO_MOBILE_ROLE.getOrDefault(webRole, webRole);
        return jwtUtil.generateToken(email, mobileRole);
    }

    /**
     * Token for the dedicated Mobile service account, not the current Web user.
     * Required by MobileExpenseClient.getUser(), which Mobile only accepts from
     * a subject that resolves to a real Mobile user record.
     */
    public String serviceAccountTokenForMobile() {
        return jwtUtil.generateToken(MOBILE_SERVICE_ACCOUNT_USERNAME, MOBILE_SERVICE_ACCOUNT_ROLE);
    }
}
