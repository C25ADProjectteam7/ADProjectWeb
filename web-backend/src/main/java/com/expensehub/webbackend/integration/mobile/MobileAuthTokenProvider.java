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
}
