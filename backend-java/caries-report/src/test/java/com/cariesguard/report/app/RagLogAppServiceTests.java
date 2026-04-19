package com.cariesguard.report.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.infrastructure.client.HttpRagAdminClient;
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
class RagLogAppServiceTests {

    @Mock
    private HttpRagAdminClient ragAdminClient;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listRequestsShouldInjectOrgId() {
        setCurrentUser();
        when(ragAdminClient.get(eq("/logs/requests"), any(), any())).thenReturn(List.of(Map.of("request_no", "REQ-1")));

        RagLogAppService appService = new RagLogAppService(ragAdminClient);
        Object response = appService.listRequests();

        assertThat(response).isEqualTo(List.of(Map.of("request_no", "REQ-1")));
        ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(ragAdminClient).get(eq("/logs/requests"), queryCaptor.capture(), any());
        assertThat(queryCaptor.getValue()).containsEntry("org_id", 2001L);
    }

    @Test
    void fusionRerankAndLlmLogsShouldCallDedicatedEndpoints() {
        when(ragAdminClient.get(eq("/logs/fusion/REQ-1"), any(), any())).thenReturn(List.of(Map.of("final_rank", 1)));
        when(ragAdminClient.get(eq("/logs/rerank/REQ-1"), any(), any())).thenReturn(List.of(Map.of("final_rank", 1)));
        when(ragAdminClient.get(eq("/logs/llm/REQ-1"), any(), any())).thenReturn(List.of(Map.of("model_name", "gpt-4o-mini")));

        RagLogAppService appService = new RagLogAppService(ragAdminClient);

        assertThat(appService.fusionLogs("REQ-1")).isEqualTo(List.of(Map.of("final_rank", 1)));
        assertThat(appService.rerankLogs("REQ-1")).isEqualTo(List.of(Map.of("final_rank", 1)));
        assertThat(appService.llmLogs("REQ-1")).isEqualTo(List.of(Map.of("model_name", "gpt-4o-mini")));
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
