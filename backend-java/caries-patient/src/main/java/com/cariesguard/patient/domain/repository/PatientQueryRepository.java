package com.cariesguard.patient.domain.repository;

import com.cariesguard.patient.domain.model.PatientDetailModel;
import java.util.Optional;

public interface PatientQueryRepository {

    Optional<PatientDetailModel> findPatientDetail(Long patientId);
}
