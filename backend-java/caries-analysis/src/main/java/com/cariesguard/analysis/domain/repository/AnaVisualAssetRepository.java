package com.cariesguard.analysis.domain.repository;

import com.cariesguard.analysis.domain.model.AnalysisVisualAssetCreateModel;
import com.cariesguard.analysis.domain.model.AnalysisVisualAssetModel;
import java.util.List;

public interface AnaVisualAssetRepository {

    void replaceByTaskId(Long taskId, List<AnalysisVisualAssetCreateModel> models);

    List<AnalysisVisualAssetModel> listByTaskId(Long taskId);
}
