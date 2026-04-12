package com.cariesguard.patient.domain.repository;

import com.cariesguard.patient.domain.model.PatientCreateModel;

public interface PatientCommandRepository {

    void createPatient(PatientCreateModel model);

    boolean existsPatientByIdCardHash(Long orgId, String idCardHash);
}
