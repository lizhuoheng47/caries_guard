package com.cariesguard.report.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.infrastructure.client.HttpRagAdminClient;
import com.cariesguard.report.interfaces.command.RagKbRebuildCommand;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RagKbAppServiceTests {

    @Mock
    private HttpRagAdminClient ragAdminClient;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void graphStatsShouldInjectOrgId() {
        setCurrentUser();
        when(ragAdminClient.get(eq("/knowledge/graph-stats"), any(), any())).thenReturn(Map.of("entityCount", 5));

        RagKbAppService appService = new RagKbAppService(ragAdminClient);
        Object response = appService.graphStats("caries-default");

        assertThat(response).isEqualTo(Map.of("entityCount", 5));
        ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(ragAdminClient).get(eq("/knowledge/graph-stats"), queryCaptor.capture(), any());
        assertThat(queryCaptor.getValue()).containsEntry("kb_code", "caries-default");
        assertThat(queryCaptor.getValue()).containsEntry("org_id", 2001L);
    }

    @Test
    void rebuildShouldInjectOperatorContext() {
        setCurrentUser();
        when(ragAdminClient.post(eq("/knowledge/rebuild"), any(), any())).thenReturn(Map.of("rebuildJobNo", "JOB-1"));

        RagKbAppService appService = new RagKbAppService(ragAdminClient);
        Object response = appService.rebuild(new RagKbRebuildCommand("caries-default", "kb", "PATIENT_GUIDE", "v1.0"));

        assertThat(response).isEqualTo(Map.of("rebuildJobNo", "JOB-1"));
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(ragAdminClient).post(eq("/knowledge/rebuild"), payloadCaptor.capture(), any());
        assertThat(payloadCaptor.getValue()).containsEntry("kbCode", "caries-default");
        assertThat(payloadCaptor.getValue()).containsEntry("orgId", 2001L);
    }

    private void setCurrentUser() {
        AuthenticatedUser user = new AuthenticatedUser(
                1001L,
                2001L,
                "doctor",
                "hash",
                "Doctor",
                true,
                List.of("DOCTOR"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
