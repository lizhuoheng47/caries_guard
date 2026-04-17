package com.cariesguard.report.domain.client;

import com.cariesguard.report.domain.model.RagAnswerModel;
import com.cariesguard.report.domain.model.RagDoctorQaRequestModel;
import com.cariesguard.report.domain.model.RagPatientExplanationRequestModel;

public interface RagClient {

    RagAnswerModel doctorQa(RagDoctorQaRequestModel request);

    RagAnswerModel patientExplanation(RagPatientExplanationRequestModel request);
}
