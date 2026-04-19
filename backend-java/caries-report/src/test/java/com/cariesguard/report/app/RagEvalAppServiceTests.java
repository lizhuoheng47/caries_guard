package com.cariesguard.report.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.infrastructure.client.HttpRagAdminClient;
import com.cariesguard.report.interfaces.command.RagEvalRunCommand;
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
class RagEvalAppServiceTests {

    @Mock
    private HttpRagAdminClient ragAdminClient;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void datasetAndRunDetailShouldCallDedicatedEndpoints() {
        when(ragAdminClient.get(eq("/eval/datasets"), any(), any())).thenReturn(List.of(Map.of("id", 1)));
        when(ragAdminClient.get(eq("/eval/datasets/1"), any(), any())).thenReturn(Map.of("id", 1));
        when(ragAdminClient.get(eq("/eval/runs/RUN-1"), any(), any())).thenReturn(Map.of("run_no", "RUN-1"));
        when(ragAdminClient.get(eq("/eval/runs/RUN-1/results"), any(), any())).thenReturn(List.of(Map.of("question_no", "Q-1")));

        RagEvalAppService appService = new RagEvalAppService(ragAdminClient);

        assertThat(appService.datasets()).isEqualTo(List.of(Map.of("id", 1)));
        assertThat(appService.datasetDetail(1L)).isEqualTo(Map.of("id", 1));
        assertThat(appService.runDetail("RUN-1")).isEqualTo(Map.of("run_no", "RUN-1"));
        assertThat(appService.runResults("RUN-1")).isEqualTo(List.of(Map.of("question_no", "Q-1")));
    }

    @Test
    void runShouldInjectOperatorContext() {
        setCurrentUser();
        when(ragAdminClient.post(eq("/eval/run"), any(), any())).thenReturn(Map.of("runNo", "RUN-1"));

        RagEvalAppService appService = new RagEvalAppService(ragAdminClient);
        Object response = appService.run(new RagEvalRunCommand(99L));

        assertThat(response).isEqualTo(Map.of("runNo", "RUN-1"));
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(ragAdminClient).post(eq("/eval/run"), payloadCaptor.capture(), any());
        assertThat(payloadCaptor.getValue()).containsEntry("datasetId", 99L);
        assertThat(payloadCaptor.getValue()).containsEntry("orgId", 2001L);
        assertThat(payloadCaptor.getValue()).containsEntry("operatorId", 1001L);
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
