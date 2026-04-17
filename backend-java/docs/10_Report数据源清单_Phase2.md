# Report 数据源清单（Phase 2）

> 配套《CariesGuard 下一阶段逐模块开发清单 v2》Phase 2 T1。  
> 目标：冻结报告生成的输入源，避免 report 服务层直接拼散乱 JSON 或跨边界读取 Python 内部表。

## 1. 固定输入源

报告模块只从 Java 业务库中的以下表读取报告输入：

| 表 | 用途 | 当前读取入口 |
| --- | --- | --- |
| `med_case` | 病例编号、患者锚点、病例状态、机构 | `ReportSourceQueryRepository.findCase` |
| `med_image_file` + `med_attachment` | 原始影像与对象存储引用 | `ReportSourceQueryRepository.listCaseImages` |
| `med_case_tooth_record` | 牙位级发现、严重度、建议 | `ReportSourceQueryRepository.listToothRecords` |
| `ana_result_summary` | AI 分析摘要、任务锚点、病灶数、异常牙数 | `ReportSourceQueryRepository.findLatestSummary` |
| `ana_visual_asset` + `med_attachment` | mask / overlay / heatmap 可视化资产引用 | `ReportSourceQueryRepository.listVisualAssetsByTaskId` |
| `med_risk_assessment_record` | 风险等级、复查周期、风险快照 | `ReportSourceQueryRepository.findLatestRiskAssessment` |
| `ana_correction_feedback` | 医生修正记录 | `ReportSourceQueryRepository.listCorrections` |

## 2. 禁止项

- Report 不直接读取 Python `caries_ai` 库。
- Report 不直接解析 Python 中间任务表作为业务输入。
- Report 不跳过 `med_attachment` 拼对象下载地址。
- Report 不直接把 `raw_result_json` 当作唯一事实来源。

## 3. 当前组装口径

`ReportAppService.generateReport` 按以下顺序组装：

1. 读取 `med_case` 并校验机构权限、病例状态；
2. 读取最新 `ana_result_summary`；
3. 用 `summary.taskId` 读取 `ana_visual_asset`；
4. 读取病例原始影像、牙位记录、风险评估、医生修正记录；
5. 组装 `ReportRenderDataModel`；
6. 渲染医生版或患者版内容；
7. 生成 PDF 并归档到 `med_attachment`；
8. 写入 `rpt_record`、导出时写入 `rpt_export_log`。

## 4. 验收口径

- 医生版报告至少包含：病例信息、牙位发现、uncertainty、visual asset 引用、风险评估、医生修正摘要。
- 患者版报告至少包含：通俗解释、风险等级、护理建议、复查建议。
- 同一病例生成报告时，`rpt_record.source_summary_id / source_risk_assessment_id / source_correction_id` 能追溯输入快照。
- 报告附件必须通过 `med_attachment` 管理，下载走现有附件访问链路。
