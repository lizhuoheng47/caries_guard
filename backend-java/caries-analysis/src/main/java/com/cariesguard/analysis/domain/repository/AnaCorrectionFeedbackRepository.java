package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.domain.model.CorrectionFeedbackCreateModel;

public interface AnaCorrectionFeedbackRepository {

    void save(CorrectionFeedbackCreateModel model);
}
