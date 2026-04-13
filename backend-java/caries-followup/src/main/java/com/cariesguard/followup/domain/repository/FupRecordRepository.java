package com.cariesguard.followup.domain.repository;

import com.cariesguard.followup.domain.model.FupRecordCreateModel;
import com.cariesguard.followup.domain.model.FupRecordModel;
import java.util.List;
import java.util.Optional;

public interface FupRecordRepository {

    void create(FupRecordCreateModel model);

    Optional<FupRecordModel> findById(Long recordId);

    List<FupRecordModel> listByTaskId(Long taskId);

    List<FupRecordModel> listByCaseId(Long caseId);

    Optional<FupRecordModel> findLatestByCaseId(Long caseId);
}
