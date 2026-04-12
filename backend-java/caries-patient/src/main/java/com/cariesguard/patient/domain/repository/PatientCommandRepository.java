package com.cariesguard.patient.domain.repository;

import com.cariesguard.patient.domain.model.PatientCreateModel;
import com.cariesguard.patient.domain.model.PatientManagedModel;
import com.cariesguard.patient.domain.model.PatientUpdateModel;
import java.util.Optional;

public interface PatientCommandRepository {

    void createPatient(PatientCreateModel model);

    Optional<PatientManagedModel> findManagedPatient(Long patientId);

    void updatePatient(PatientUpdateModel model);

    boolean existsPatientByIdCardHash(Long orgId, String idCardHash);
}
