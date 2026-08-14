package com.expensehub.webbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expensehub.webbackend.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.repository.UserRepository;
import com.expensehub.webbackend.service.ManagerService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ManagerControllerTest {

    @Mock private ManagerService managerService;
    @Mock private UserRepository userRepository;

    private ManagerController controller;

    @BeforeEach
    void setUp() {
        controller = new ManagerController(managerService, userRepository);
    }

    private MockedStatic<SecurityContextHolder> mockAuthenticatedAs(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class);
        mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        return mocked;
    }

    @Test
    void approve_resolvesManagerIdFromAuthenticatedEmail_notFromRequestBody() {
        User manager = User.builder().id(42L).email("manager@test.com").build();
        Approval expected = Approval.builder().id(1L).status(ApprovalStatus.APPROVED).managerId(42L).build();

        try (MockedStatic<SecurityContextHolder> ignored = mockAuthenticatedAs("manager@test.com")) {
            when(userRepository.findByEmail("manager@test.com")).thenReturn(Optional.of(manager));
            when(managerService.decide(1L, ApprovalStatus.APPROVED, "Looks good", 42L)).thenReturn(expected);

            // Even if a client tried to sneak a different managerId into the body, it must be ignored.
            Approval result = controller.approve(1L, Map.of("note", "Looks good", "managerId", "999"));

            assertThat(result.getManagerId()).isEqualTo(42L);
            verify(managerService).decide(1L, ApprovalStatus.APPROVED, "Looks good", 42L);
        }
    }

    @Test
    void reject_resolvesManagerIdFromAuthenticatedEmail() {
        User manager = User.builder().id(7L).email("finance@test.com").build();
        Approval expected = Approval.builder().id(2L).status(ApprovalStatus.REJECTED).managerId(7L).build();

        try (MockedStatic<SecurityContextHolder> ignored = mockAuthenticatedAs("finance@test.com")) {
            when(userRepository.findByEmail("finance@test.com")).thenReturn(Optional.of(manager));
            when(managerService.decide(2L, ApprovalStatus.REJECTED, null, 7L)).thenReturn(expected);

            Approval result = controller.reject(2L, Map.of());

            assertThat(result.getManagerId()).isEqualTo(7L);
        }
    }

    @Test
    void approve_emailNotFoundInUserRepository_passesNullManagerId() {
        Approval expected = Approval.builder().id(3L).status(ApprovalStatus.APPROVED).managerId(null).build();

        try (MockedStatic<SecurityContextHolder> ignored = mockAuthenticatedAs("ghost@test.com")) {
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());
            when(managerService.decide(eq(3L), eq(ApprovalStatus.APPROVED), any(), isNull())).thenReturn(expected);

            Approval result = controller.approve(3L, Map.of());

            assertThat(result.getManagerId()).isNull();
        }
    }
}
