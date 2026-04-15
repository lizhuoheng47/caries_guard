# CariesGuard —— Python 端统一实施开发文档（国一顶级整合版 v2）

> 项目代号：CariesGuard  
> 文档代号：PY-IMPL-V2  
> 文档定位：Python 端（AI 服务 + 训练体系 + 数据治理 + 模型治理 + 仓库落地 + 接口契约 + 验收标准 + 可直接落地的代码骨架）**唯一权威主文档**  
> 适用对象：Python 开发工程师、算法工程师、项目负责人、测试负责人、答辩成员  
> 版本说明：v2 相对 v1 的关键变更：
> 1. 合并并取代了原《CariesGuard_Python端统一实施开发文档》《Python 仓库初始化模板与 FastAPI 项目骨架》《requirements 与脚手架代码清单》三份文档
> 2. 目录结构、配置文件路径、模块划分统一为本文档冻结版，**任何分歧以本文档为准**
> 3. 接口契约字段与《Java ↔ Python 接口契约对照表》双向对齐，本文档不再重复字段定义，只引用
> 4. 补齐了 TPE / CVC / EDL 的伪代码与关键张量 shape
> 5. 补齐了可真正运行的 InferencePipeline、CallbackService、ModelRegistry 代码骨架
> 6. 数据前提条件全部依赖《CariesGuard 数据现状清单与采集计划》，本文档不假设数据已就绪
> 7. 所有 v1 中过于理想化或与其他文档冲突的内容已修订或删除

> **使用方式**：本文件作为 Python 端**唯一主文档**，分工、编码、联调、测试、答辩全部围绕本文档展开。v1 的三份文档标记为 **deprecated**，不再维护。

---

## 目录

### 第一部分：顶层设计
1. 文档目标与版本变更
2. Python 端总体定位
3. Python 端开发原则
4. 仓库总体结构（冻结版）

### 第二部分：在线推理服务
5. FastAPI 服务骨架
6. 核心模块与 Pipeline 设计
7. 接口契约（引用对照表）
8. 代码骨架示例（可运行版）
9. Java ↔ Python 协同协议
10. 幂等、重试、容错与可观测性

### 第三部分：算法实现细节
11. TPC-Net 实现指南（含伪代码）
12. EDL 分级实现指南（含伪代码）
13. 风险融合模块实现指南
14. 可解释性与可视化

### 第四部分：训练与数据治理
15. 训练体系总体设计
16. 数据治理流程（引用数据清单）
17. 数据集版本管理与快照
18. 模型版本治理与发布审批

### 第五部分：工程与验收
19. 环境与配置管理
20. 测试体系
21. 部署与容器化
22. 阶段里程碑与答辩资产
23. 答辩证据链清单
24. v1 三份文档的取代映射

---

# 第一部分：顶层设计

## 1. 文档目标与版本变更

### 1.1 本文档解决的核心问题

v2 集中解决 v1 版本的以下 6 个问题：

| 问题编号 | v1 存在的问题 | v2 的解决方式 |
|---|---|---|
| 问题 1 | 三份文档各自有"推荐目录结构"，且彼此不一致 | §4 给出唯一权威目录，其他文档标记为 deprecated |
| 问题 2 | 接口契约没有与 Java 端双向对齐 | §7 不再重复字段，全部引用《Java ↔ Python 接口契约对照表》 |
| 问题 3 | 训练数据来源未说明 | §15~§16 引用《数据现状清单与采集计划》，不再假设数据已就绪 |
| 问题 4 | 代码骨架只停留在 mock_pipeline | §8 给出可运行的 InferencePipeline、CallbackService、ModelRegistry |
| 问题 6 | TPE/CVC/EDL 只有建议，没有伪代码和张量 shape | §11~§12 给出伪代码、形状注释、训练 loss |
| 附加 | 与 Java 端命名规范（05 号文档）脱节 | §5~§7 统一 camelCase 对外、snake_case 对内 |

（v1 问题 5 即"每人每天做什么"的任务工单粒度，本次不在本文档覆盖范围，后续由独立的任务工单文档承担）

### 1.2 本文档的前置依赖

| 文档 | 依赖关系 |
|---|---|
| 03 号 项目总体设计文档 | 顶层架构和叙事主线 |
| 04 号 Java 后端开发说明书 | Java 端既成事实 |
| 05 号 Java 后端命名规范 | 字段命名统一标准 |
| 01 号 数据集采集与标注规范 | 数据治理方法论 |
| 06 号 数据库总体设计与数据治理 | 业务数据边界 |
| **《Java ↔ Python 接口契约对照表》** | 所有接口字段权威定义 |
| **《CariesGuard 数据现状清单与采集计划》** | 数据前提事实 |

本文档不复述这些文档的内容，只引用。

---

## 2. Python 端总体定位

### 2.1 Python 端是独立算法子系统

整体架构明确冻结为：

> **模块化单体 + 独立 AI 服务 + RabbitMQ 解耦**

Python 端不是后端里的一个小接口，而是独立的算法服务边界，承担四类职责：

**A. 在线推理职责**
- 影像预处理
- 影像质量检查
- 牙位检测与 FDI 映射
- 病灶分割（TPC-Net）
- 龋病分级（EDL）+ 不确定性输出
- 多模态风险评估
- 可解释性资产生成

**B. 训练职责**
- 消费脱敏副本数据
- 数据集版本构建
- 模型训练、验证、测试
- 消融实验与错误分析
- 候选模型导出与评估归档

**C. 数据治理职责**
- 严守业务数据与训练数据边界
- 医生修正样本的脱敏导出与准入校验
- Dataset Card、版本号、统计表管理

**D. 模型治理职责**
- 模型版本登记
- 离线评估
- 候选模型比对
- 人工审批上线
- 回滚能力保留

### 2.2 Python 端不做什么（硬边界）

- ❌ 不直接写业务数据库（`caries_guard` MySQL）
- ❌ 不维护病例状态机
- ❌ 不直接写 `ana_task_record` / `ana_result_summary` / `med_case` 等业务表
- ❌ 不承担业务权限校验
- ❌ 不生成最终 PDF 报告（PDF 由 Java 的 `caries-report` 模块生成）
- ❌ 不直接面向前端暴露接口（前端只和 Java 通信）

---

## 3. Python 端开发原则

### 3.1 四条红线

**第一条：不做"只会推理"的半成品**  
必须体现完整平台闭环：任务接收 → 推理执行 → 结果回调 → 资产落地 → 失败重试 → 修正回流 → 训练迭代。

**第二条：不把业务库直接当训练集**  
Python 训练端只能使用脱敏副本层数据、标注质控层数据、训练数据快照、经审批的回流样本。禁止直接连业务库把 `pat_patient`、`med_case`、原始业务影像当训练原料。

**第三条：所有对外接口走契约**  
Python 端所有对 Java 暴露的接口，字段名、类型、枚举值必须严格遵循《Java ↔ Python 接口契约对照表》。对内部私有实现可以使用任何命名，但接口边界必须 camelCase。

**第四条：所有模型必须版本化**  
没有版本号的模型权重不允许进入推理服务加载目录，没有 Dataset Card 的数据不允许进入训练快照。

### 3.2 明确禁止的开发方式

- ❌ 在 `app/` 下直接 import `train/` 里的训练代码
- ❌ 在 `train/` 下直接读取 MinIO 业务 bucket（只能读 `datasets/` 下的脱敏数据）
- ❌ 把模型权重文件直接 commit 到 Git（必须走 `artifacts/models/` + DVC 或外部存储）
- ❌ 在代码里硬编码 Java 端地址、MinIO 地址、数据库地址
- ❌ 用 print 代替 logger
- ❌ 把业务逻辑写在 FastAPI 路由函数里（必须走 `services/` 或 `pipelines/`）
- ❌ 回调 Java 时不带 `taskNo` 或 `traceId`

---

## 4. 仓库总体结构（冻结版）

### 4.1 唯一权威目录结构

**以下结构为 v2 冻结版，覆盖 v1 三份文档中的所有不一致。**

```text
ai-python/
├── README.md
├── requirements/
│   ├── base.txt              # 服务基础依赖
│   ├── service.txt           # FastAPI + MQ + MinIO 等
│   ├── train.txt             # Torch + timm + ultralytics 等
│   ├── dev.txt               # 开发工具
│   └── test.txt              # pytest 等
├── requirements.txt          # 总入口（include 上面几个）
├── pyproject.toml
├── .env.example
├── .gitignore
├── Makefile                  # 统一启动/测试/格式化命令
│
├── configs/
│   ├── base.yaml             # 所有环境共用
│   ├── dev.yaml              # 本地开发
│   ├── test.yaml             # 单元测试环境
│   ├── prod.yaml             # 部署演示环境
│   ├── model/
│   │   ├── tooth_detect.yaml
│   │   ├── tpc_net.yaml
│   │   ├── edl_grade.yaml
│   │   └── risk_fusion.yaml
│   └── dataset/
│       ├── split.yaml
│       └── snapshot.yaml
│
├── app/                      # 只负责在线推理服务
│   ├── __init__.py
│   ├── main.py               # FastAPI 入口
│   ├── lifespan.py           # 启动/关闭生命周期
│   ├── api/
│   │   ├── __init__.py
│   │   ├── router.py         # 聚合路由
│   │   └── v1/
│   │       ├── __init__.py
│   │       ├── health.py
│   │       ├── quality_check.py
│   │       ├── analyze.py
│   │       ├── assess_risk.py
│   │       └── model_version.py
│   ├── core/
│   │   ├── config.py         # 配置加载
│   │   ├── logging.py        # 统一日志
│   │   ├── exceptions.py     # 自定义异常
│   │   ├── middleware.py     # trace id / 日志 / 异常
│   │   ├── security.py       # token 校验
│   │   └── trace.py          # traceId 上下文
│   ├── schemas/              # Pydantic 模型
│   │   ├── common.py         # ApiResponse / ErrorCode
│   │   ├── request.py        # 入参
│   │   ├── callback.py       # 回调体
│   │   └── internal.py       # 内部 DTO
│   ├── services/             # 业务服务层
│   │   ├── preprocess_service.py
│   │   ├── quality_service.py
│   │   ├── tooth_detect_service.py
│   │   ├── segmentation_service.py
│   │   ├── grading_service.py
│   │   ├── risk_service.py
│   │   ├── explain_service.py
│   │   ├── callback_service.py
│   │   └── model_registry_service.py
│   ├── pipelines/            # 编排层
│   │   ├── __init__.py
│   │   ├── inference_pipeline.py
│   │   ├── quality_pipeline.py
│   │   └── risk_pipeline.py
│   ├── infra/                # 基础设施
│   │   ├── mq/
│   │   │   ├── consumer.py
│   │   │   └── publisher.py
│   │   ├── storage/
│   │   │   └── minio_client.py
│   │   ├── java_client/
│   │   │   └── callback_client.py
│   │   └── model_loader/
│   │       └── loader.py
│   └── utils/
│       ├── image_io.py
│       ├── geometry.py
│       ├── mask.py
│       └── hash_utils.py
│
├── train/                    # 只负责训练
│   ├── common/
│   │   ├── dataset.py
│   │   ├── transforms.py
│   │   ├── losses.py
│   │   ├── metrics.py
│   │   └── trainer.py
│   ├── tooth_detect/
│   │   ├── config.py
│   │   ├── train.py
│   │   └── export.py
│   ├── tpc_net/
│   │   ├── config.py
│   │   ├── model.py          # TPC-Net 主干 + TPE + CVC head
│   │   ├── dataset.py        # 跨视图配对 dataset
│   │   ├── train.py
│   │   └── export.py
│   ├── edl_grade/
│   │   ├── config.py
│   │   ├── model.py          # EDL 分级头
│   │   ├── dataset.py
│   │   ├── train.py
│   │   └── export.py
│   ├── risk_fusion/
│   │   ├── config.py
│   │   ├── model.py          # LightGBM / MLP
│   │   ├── train.py
│   │   └── export.py
│   └── export/
│       └── package_model.py
│
├── evaluation/               # 只负责离线评估
│   ├── offline_eval.py
│   ├── regression_eval.py
│   ├── error_analysis.py
│   └── ablation/
│       ├── run_tpe_ablation.py
│       ├── run_cvc_ablation.py
│       └── run_edl_calibration.py
│
├── tools/                    # 数据与模型治理工具
│   ├── export_desensitized_data.py
│   ├── build_snapshot.py
│   ├── validate_dataset.py
│   ├── detect_leak.py
│   ├── generate_dataset_card.py
│   ├── generate_changelog.py
│   ├── package_model.py
│   ├── register_model.py
│   └── sam_assist/           # SAM 辅助标注
│       └── run_sam_assist.py
│
├── datasets/                 # 脱敏后的数据与快照
│   ├── raw_manifest/
│   ├── desensitized/
│   ├── snapshots/
│   ├── dataset_cards/
│   └── checksums/
│
├── artifacts/                # 模型权重与评估资产
│   ├── models/
│   │   ├── tooth_detect/
│   │   ├── tpc_net/
│   │   ├── edl_grade/
│   │   └── risk_fusion/
│   ├── model_registry.json   # 模型注册表（本地维护）
│   ├── reports/              # 评估报告
│   ├── visual_assets/        # 答辩用可视化
│   └── metrics/              # 评估指标
│
├── scripts/                  # 启动与运维脚本
│   ├── start_dev.sh
│   ├── start_prod.sh
│   └── run_worker.sh
│
└── tests/
    ├── unit/
    │   ├── test_schemas.py
    │   ├── test_callback_service.py
    │   └── test_services.py
    ├── integration/
    │   ├── test_analyze_flow.py
    │   └── test_callback_flow.py
    ├── contract/              # 契约测试
    │   └── test_callback_schema.py
    └── e2e/
        └── test_e2e_mock.py
```

### 4.2 顶层目录职责冻结

| 目录 | 职责 | 严格约束 |
|---|---|---|
| `app/` | 只负责在线推理服务 | 禁止 import `train/` |
| `train/` | 只负责离线训练 | 禁止 import `app/`，禁止连业务库 |
| `evaluation/` | 只负责离线评估 | 只能读 `datasets/snapshots/` |
| `tools/` | 数据与模型治理工具 | 每个工具是独立 CLI |
| `datasets/` | 脱敏数据与快照 | 不 commit 数据，只 commit 脚本和 manifest |
| `artifacts/` | 模型权重与评估资产 | 权重文件通过外部存储或 DVC，不 commit |
| `configs/` | 所有配置 | 禁止硬编码，所有可变参数必须进 yaml |
| `tests/` | 测试代码 | 单元 / 集成 / 契约 / e2e 分离 |
| `scripts/` | 启动与运维脚本 | 不写业务逻辑 |

### 4.3 与 v1 旧文档的目录差异修正表

**本表是 v2 覆盖 v1 不一致的权威记录。**

| v1 旧位置 | v2 冻结位置 | 变更理由 |
|---|---|---|
| `configs/dataset/snapshot_v1.yaml` | `configs/dataset/snapshot.yaml` | 版本号不写进文件名，写进 yaml 内容 |
| `app/api/inference.py`（无 v1） | `app/api/v1/analyze.py` | 强制 API 版本前缀 |
| `requirements.txt`（单文件） | `requirements/*.txt` + 顶层 include | 支持分层依赖 |
| `schemas/task.py` + `schemas/result.py` | `schemas/request.py` + `schemas/callback.py` + `schemas/internal.py` | 按数据方向划分更清晰 |
| `tools/` 散落 | `tools/` + `tools/sam_assist/` 子目录 | 便于扩展 |
| `tests/` 无契约测试 | 新增 `tests/contract/` | 契约测试不能缺 |

---

# 第二部分：在线推理服务

## 5. FastAPI 服务骨架

### 5.1 启动入口约定

- 启动命令：`uvicorn app.main:app --host 0.0.0.0 --port 8001 --workers 2`
- 健康检查：`GET /ai/v1/health` 必须 200 ms 内返回
- 文档页：`/docs` 在 dev 环境开放，prod 环境关闭

### 5.2 中间件加载顺序

按以下顺序注册（从外到内）：

1. `CORSMiddleware` —— 允许 Java 所在的源
2. `TraceIdMiddleware` —— 解析/生成 `X-Trace-Id`
3. `RequestLoggingMiddleware` —— 入参与耗时日志
4. `ExceptionHandlerMiddleware` —— 全局异常转 ApiResponse
5. `CallbackTokenMiddleware` —— 仅对回调相关路由生效

### 5.3 lifespan 管理

启动时：
1. 加载所有模型到内存（通过 `ModelRegistryService`）
2. 初始化 MinIO 客户端
3. 初始化 RabbitMQ 消费者（可选，见 §10.3）
4. 预热一次推理 pipeline（用一张内置小图跑通）

关闭时：
1. 释放 MQ 连接
2. 释放 GPU 内存
3. 刷 pending 的日志

---

## 6. 核心模块与 Pipeline 设计

### 6.1 服务模块划分

| 模块 | 路由 | 核心 Service | 核心 Pipeline |
|---|---|---|---|
| 健康 | `/ai/v1/health` | - | - |
| 质量检查 | `/ai/v1/quality-check` | `QualityService` | `QualityPipeline` |
| 综合分析 | `/ai/v1/analyze` | `InferencePipeline` 的入口 | `InferencePipeline` |
| 风险评估 | `/ai/v1/assess-risk` | `RiskService` | `RiskPipeline` |
| 模型版本 | `/ai/v1/model-version` | `ModelRegistryService` | - |

### 6.2 InferencePipeline 主流程

```text
1. 接收请求 / 从 MQ 拉取任务
2. 下载所有影像到本地临时目录
3. 预处理（resize / normalize / 通道对齐）
4. 质量检查（任何一张不合格则整体拒收）
5. 牙位检测（YOLOv8）
6. 病灶分割（TPC-Net）
7. 分级（EDL）+ 不确定性
8. 风险融合（RiskService）
9. 可解释性资产生成（热力图 / overlay）
10. 上传 visual assets 到 MinIO caries-visual
11. 构建回调体
12. 回调 Java
13. 清理本地临时文件
14. 释放分布式锁
```

**每一步都必须**：
- 带 traceId
- 捕获异常并转为 `FAILED` 回调
- 记录耗时到 `rawResultJson.inferenceSecondsByStage`

### 6.3 Pipeline 和 Service 的边界

- **Service 层**：单一职责，无状态，接收张量/路径，返回结果对象
- **Pipeline 层**：编排多个 Service，负责上下文传递、异常捕获、日志、回调

不允许在 Service 层调用其他 Service；Service 之间的交互只通过 Pipeline。

---

## 7. 接口契约（引用对照表）

### 7.1 权威来源

**所有 Java ↔ Python 的接口字段定义、枚举值、错误码、回调体结构均以《Java ↔ Python 接口契约对照表》为唯一权威来源。**

本文档不重复字段定义，只在编码规范上做补充。

### 7.2 Python 端实现约束

- 所有请求和响应模型必须使用 **Pydantic v2** 定义
- 字段对外使用 `alias` 映射为 camelCase，内部属性使用 snake_case
- 使用 `model_config = ConfigDict(populate_by_name=True)` 允许两种命名双向解析

示例：

```python
from pydantic import BaseModel, Field, ConfigDict

class AnalyzeRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    
    trace_id: str = Field(alias="traceId")
    task_no: str = Field(alias="taskNo")
    case_id: int = Field(alias="caseId")
    patient_id: int = Field(alias="patientId")
    org_id: int = Field(alias="orgId")
    case_no: str = Field(alias="caseNo")
    image_ids: list[int] = Field(alias="imageIds")
    images: list["ImageRef"]
    patient_profile: "PatientProfile | None" = Field(default=None, alias="patientProfile")
    model_version: str | None = Field(default=None, alias="modelVersion")
    callback_url: str = Field(alias="callbackUrl")
    callback_token: str = Field(alias="callbackToken")
    requested_at: str = Field(alias="requestedAt")
```

回调体在 `schemas/callback.py` 中定义，必须通过 **契约测试**（见 §20.3）验证与对照表一致。

### 7.3 字段变更流程

- 任何字段变更必须先更新《Java ↔ Python 接口契约对照表》
- 对照表升版后，Python 端同步更新 Pydantic schema
- 跑 `pytest tests/contract/` 确认契约测试通过
- 通知 Java 端同步更新

### 7.4 最小字段集与完整字段集

- **联调初期**使用最小字段集（对照表 §14.1），只发送能让 Java 落库的必需字段
- **联调中期**补齐 `toothDetections` 和 `qualityCheckResults`
- **联调后期**补齐 `rawResultJson` 和 `riskAssessment.explanationFactors`

---

## 8. 代码骨架示例（可运行版）

**本节的代码是 v2 重点补齐的部分，v1 中所有停留在 mock 层的骨架全部升级为可运行版本。**

### 8.1 `app/main.py`

```python
from fastapi import FastAPI
from contextlib import asynccontextmanager

from app.api.router import api_router
from app.core.config import settings
from app.core.logging import setup_logging
from app.core.middleware import register_middlewares
from app.services.model_registry_service import ModelRegistryService


@asynccontextmanager
async def lifespan(app: FastAPI):
    # startup
    setup_logging()
    app.state.model_registry = ModelRegistryService()
    await app.state.model_registry.load_all()
    yield
    # shutdown
    await app.state.model_registry.unload_all()


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.app_name,
        version="2.0.0",
        docs_url="/docs" if settings.app_env != "prod" else None,
        redoc_url="/redoc" if settings.app_env != "prod" else None,
        lifespan=lifespan,
    )
    register_middlewares(app)
    app.include_router(api_router, prefix="/ai/v1")
    return app


app = create_app()
```

### 8.2 `app/core/config.py`

```python
from functools import lru_cache
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # application
    app_name: str = "CariesGuard-AI"
    app_env: str = Field(default="dev")
    app_host: str = "0.0.0.0"
    app_port: int = 8001
    
    # Java backend
    java_base_url: str = "http://127.0.0.1:8080"
    callback_path: str = "/api/v1/ai/callbacks/analysis"
    callback_timeout_seconds: int = 10
    callback_max_retries: int = 4
    
    # MinIO
    minio_endpoint: str = "127.0.0.1:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin"
    minio_secure: bool = False
    bucket_image: str = "caries-image"
    bucket_visual: str = "caries-visual"
    
    # RabbitMQ
    rabbitmq_host: str = "127.0.0.1"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "guest"
    rabbitmq_password: str = "guest"
    exchange_analysis: str = "cg.analysis.exchange"
    queue_analysis_request: str = "cg.analysis.request.queue"
    
    # Redis (用于分布式锁与幂等)
    redis_host: str = "127.0.0.1"
    redis_port: int = 6379
    redis_db: int = 0
    
    # Model
    model_root: str = "./artifacts/models"
    model_registry_path: str = "./artifacts/model_registry.json"
    
    # Inference
    inference_temp_dir: str = "/tmp/cariesguard"
    inference_stage_timeout_seconds: int = 30
    enable_mq_consumer: bool = False  # 一期默认走 HTTP 模式
    
    # GPU
    cuda_visible_devices: str = "0"
    
    model_config = SettingsConfigDict(env_file=".env", extra="ignore", env_prefix="CG_")


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
```

### 8.3 `app/schemas/common.py`

```python
from typing import Any, Generic, TypeVar
from datetime import datetime, timezone
from pydantic import BaseModel, Field, ConfigDict

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    model_config = ConfigDict(populate_by_name=True)
    
    code: str = "00000"
    message: str = "success"
    data: T | None = None
    trace_id: str | None = Field(default=None, alias="traceId")
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    
    @classmethod
    def success(cls, data: T | None = None, trace_id: str | None = None) -> "ApiResponse[T]":
        return cls(code="00000", message="success", data=data, trace_id=trace_id)
    
    @classmethod
    def error(cls, code: str, message: str, trace_id: str | None = None) -> "ApiResponse[None]":
        return cls(code=code, message=message, data=None, trace_id=trace_id)


class ErrorCode:
    SUCCESS = "00000"
    PARAM_INVALID = "A0400"
    UNAUTHORIZED = "A0401"
    NOT_FOUND = "A0404"
    BUSINESS_CONFLICT = "B0001"
    MODEL_NOT_LOADED = "B0002"
    QUALITY_REJECT = "B0003"
    AI_UNAVAILABLE = "C3001"
    INFERENCE_TIMEOUT = "C3002"
    DEPENDENCY_FAIL = "C3003"
    UNKNOWN = "C9999"
```

### 8.4 `app/core/exceptions.py`

```python
class BusinessException(Exception):
    def __init__(self, code: str, message: str, cause: Exception | None = None):
        self.code = code
        self.message = message
        self.cause = cause
        super().__init__(f"[{code}] {message}")


class QualityRejectException(BusinessException):
    def __init__(self, message: str = "image quality reject"):
        super().__init__("B0003", message)


class ModelNotLoadedException(BusinessException):
    def __init__(self, model_name: str):
        super().__init__("B0002", f"model not loaded: {model_name}")


class InferenceTimeoutException(BusinessException):
    def __init__(self, stage: str, elapsed: float):
        super().__init__("C3002", f"inference timeout at {stage} after {elapsed:.1f}s")


class DependencyException(BusinessException):
    def __init__(self, dep: str, message: str):
        super().__init__("C3003", f"{dep}: {message}")
```

### 8.5 `app/core/middleware.py`

```python
import time
import uuid
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from loguru import logger

from app.core.exceptions import BusinessException
from app.schemas.common import ApiResponse, ErrorCode


def register_middlewares(app: FastAPI) -> None:
    
    @app.middleware("http")
    async def trace_middleware(request: Request, call_next):
        trace_id = request.headers.get("X-Trace-Id") or f"py-{uuid.uuid4().hex[:12]}"
        request.state.trace_id = trace_id
        start = time.time()
        
        with logger.contextualize(trace_id=trace_id):
            try:
                response = await call_next(request)
                elapsed_ms = int((time.time() - start) * 1000)
                logger.info(
                    f"HTTP {request.method} {request.url.path} -> {response.status_code} {elapsed_ms}ms"
                )
                response.headers["X-Trace-Id"] = trace_id
                return response
            except BusinessException as be:
                logger.warning(f"business exception: {be.code} {be.message}")
                return JSONResponse(
                    status_code=200,
                    content=ApiResponse.error(be.code, be.message, trace_id).model_dump(by_alias=True),
                )
            except Exception as e:
                logger.exception(f"unhandled exception: {e}")
                return JSONResponse(
                    status_code=200,
                    content=ApiResponse.error(ErrorCode.UNKNOWN, str(e), trace_id).model_dump(by_alias=True),
                )
```

### 8.6 `app/api/v1/analyze.py`

```python
from fastapi import APIRouter, BackgroundTasks, Request

from app.pipelines.inference_pipeline import InferencePipeline
from app.schemas.common import ApiResponse
from app.schemas.request import AnalyzeRequest
from app.schemas.callback import AcceptedData

router = APIRouter()


@router.post("/analyze", response_model=ApiResponse[AcceptedData])
async def analyze(
    request: Request,
    payload: AnalyzeRequest,
    background: BackgroundTasks,
) -> ApiResponse[AcceptedData]:
    trace_id = request.state.trace_id
    
    pipeline: InferencePipeline = request.app.state.inference_pipeline
    # 把真正的推理放入 background，立即返回"已受理"
    background.add_task(pipeline.run_safely, payload, trace_id)
    
    return ApiResponse.success(
        data=AcceptedData(
            task_no=payload.task_no,
            task_status_code="QUEUEING",
            estimated_seconds=12,
        ),
        trace_id=trace_id,
    )
```

### 8.7 `app/pipelines/inference_pipeline.py`（关键）

```python
import time
from loguru import logger

from app.core.exceptions import BusinessException, QualityRejectException
from app.schemas.request import AnalyzeRequest
from app.schemas.callback import (
    AnalysisCallbackPayload,
    FailureCallbackPayload,
    QualityRejectCallbackPayload,
    SummaryBlock,
)
from app.services.preprocess_service import PreprocessService
from app.services.quality_service import QualityService
from app.services.tooth_detect_service import ToothDetectService
from app.services.segmentation_service import SegmentationService
from app.services.grading_service import GradingService
from app.services.risk_service import RiskService
from app.services.explain_service import ExplainService
from app.services.callback_service import CallbackService
from app.services.model_registry_service import ModelRegistryService


class InferencePipeline:
    """
    编排整条推理流水线。
    每一步：
      - 带 traceId 日志
      - 捕获异常转 FAILED 回调
      - 记录耗时
    """

    def __init__(
        self,
        model_registry: ModelRegistryService,
        callback_service: CallbackService,
    ) -> None:
        self.registry = model_registry
        self.preprocess = PreprocessService()
        self.quality = QualityService(self.registry)
        self.tooth_detect = ToothDetectService(self.registry)
        self.segmentation = SegmentationService(self.registry)
        self.grading = GradingService(self.registry)
        self.risk = RiskService(self.registry)
        self.explain = ExplainService()
        self.callback = callback_service

    async def run_safely(self, payload: AnalyzeRequest, trace_id: str) -> None:
        """外部入口，保证任何异常都能触发回调。"""
        try:
            await self.run(payload, trace_id)
        except QualityRejectException as qe:
            await self._callback_quality_reject(payload, trace_id, qe)
        except BusinessException as be:
            await self._callback_failure(payload, trace_id, be.code, be.message, stage="UNKNOWN")
        except Exception as e:
            logger.exception(f"pipeline unhandled: {e}")
            await self._callback_failure(payload, trace_id, "C9999", str(e), stage="UNKNOWN")

    async def run(self, payload: AnalyzeRequest, trace_id: str) -> None:
        timing: dict[str, float] = {}
        started_at = _now_iso()

        # 1. 预处理
        with _timer(timing, "preprocess"):
            images = await self.preprocess.load_and_normalize(payload.images)

        # 2. 质量检查
        with _timer(timing, "qualityCheck"):
            quality_results = await self.quality.check_batch(images)
        if any(q.check_result_code == "REJECT" for q in quality_results):
            raise QualityRejectException()

        # 3. 牙位检测
        with _timer(timing, "toothDetect"):
            tooth_detections = await self.tooth_detect.detect(images)

        # 4. 分割
        with _timer(timing, "segmentation"):
            seg_results = await self.segmentation.segment(images, tooth_detections)

        # 5. 分级 + 不确定性
        with _timer(timing, "grading"):
            lesion_results = await self.grading.grade(images, seg_results, tooth_detections)

        # 6. 风险融合
        with _timer(timing, "riskFusion"):
            risk_assessment = await self.risk.assess(
                lesion_results=lesion_results,
                patient_profile=payload.patient_profile,
            )

        # 7. 可视化资产生成与上传
        with _timer(timing, "explain"):
            visual_assets = await self.explain.generate_and_upload(
                case_no=payload.case_no,
                images=images,
                seg_results=seg_results,
                lesion_results=lesion_results,
            )

        # 8. 构建并发送回调
        completed_at = _now_iso()
        callback_payload = self._build_success_payload(
            payload=payload,
            trace_id=trace_id,
            started_at=started_at,
            completed_at=completed_at,
            quality_results=quality_results,
            tooth_detections=tooth_detections,
            lesion_results=lesion_results,
            visual_assets=visual_assets,
            risk_assessment=risk_assessment,
            timing=timing,
        )
        await self.callback.send_success(payload.callback_url, payload.callback_token, callback_payload)

    def _build_success_payload(self, **kw) -> AnalysisCallbackPayload:
        payload: AnalyzeRequest = kw["payload"]
        lesion_results = kw["lesion_results"]
        summary = SummaryBlock(
            overall_highest_severity=_max_severity(lesion_results),
            suspicious_tooth_count=len({l.tooth_code for l in lesion_results if l.severity_code != "C0"}),
            overall_uncertainty_score=_avg_uncertainty(lesion_results),
            lesion_area_ratio=_total_area_ratio(lesion_results),
            review_recommended_flag="1" if _needs_review(lesion_results) else "0",
            high_risk_flag="1" if kw["risk_assessment"].risk_level_code == "HIGH" else "0",
        )
        return AnalysisCallbackPayload(
            trace_id=kw["trace_id"],
            task_no=payload.task_no,
            task_status_code="SUCCESS",
            case_id=payload.case_id,
            patient_id=payload.patient_id,
            org_id=payload.org_id,
            model_version=self.registry.release_version(),
            started_at=kw["started_at"],
            completed_at=kw["completed_at"],
            summary=summary,
            tooth_detections=kw["tooth_detections"],
            lesion_results=kw["lesion_results"],
            risk_assessment=kw["risk_assessment"],
            quality_check_results=kw["quality_results"],
            raw_result_json={
                "pipelineVersion": "p2.0",
                "inferenceSecondsByStage": kw["timing"],
            },
        )

    async def _callback_quality_reject(self, payload, trace_id, qe):
        body = QualityRejectCallbackPayload(
            trace_id=trace_id,
            task_no=payload.task_no,
            task_status_code="QUALITY_REJECT",
            case_id=payload.case_id,
            patient_id=payload.patient_id,
            org_id=payload.org_id,
            started_at=_now_iso(),
            completed_at=_now_iso(),
            quality_check_results=[],  # 实际实现中要填入
        )
        await self.callback.send_failure(payload.callback_url, payload.callback_token, body)

    async def _callback_failure(self, payload, trace_id, error_code, error_message, stage):
        body = FailureCallbackPayload(
            trace_id=trace_id,
            task_no=payload.task_no,
            task_status_code="FAILED",
            case_id=payload.case_id,
            patient_id=payload.patient_id,
            org_id=payload.org_id,
            model_version=self.registry.release_version(),
            started_at=_now_iso(),
            completed_at=_now_iso(),
            error_code=error_code,
            error_message=error_message,
            error_stage=stage,
            retryable=(error_code in ("C3001", "C3002", "C3003")),
        )
        await self.callback.send_failure(payload.callback_url, payload.callback_token, body)


# ------- 辅助函数 -------

from contextlib import contextmanager
from datetime import datetime, timezone


@contextmanager
def _timer(bucket: dict[str, float], key: str):
    t0 = time.time()
    try:
        yield
    finally:
        bucket[key] = round(time.time() - t0, 3)


def _now_iso() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat()


def _max_severity(lesions) -> str:
    order = ["C0", "C1", "C2", "C3"]
    if not lesions:
        return "C0"
    return max((l.severity_code for l in lesions), key=lambda s: order.index(s))


def _avg_uncertainty(lesions) -> float:
    if not lesions:
        return 0.0
    return round(sum(l.uncertainty_score for l in lesions) / len(lesions), 3)


def _total_area_ratio(lesions) -> float:
    return round(sum(getattr(l, "lesion_area_ratio", 0.0) or 0.0 for l in lesions), 4)


def _needs_review(lesions) -> bool:
    return any(l.uncertainty_score > 0.4 or l.severity_code in ("C2", "C3") for l in lesions)
```

### 8.8 `app/services/callback_service.py`（含 HMAC + 重试）

```python
import asyncio
import hashlib
import hmac
import json
from typing import Any

import httpx
from loguru import logger
from tenacity import (
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
)

from app.core.config import settings
from app.core.exceptions import DependencyException


class CallbackService:
    """
    Python → Java 的回调客户端。
    负责：
      - 按契约组装 JSON
      - 附带 X-Callback-Token header
      - 指数退避重试
      - 失败兜底
    """

    def __init__(self) -> None:
        self._client = httpx.AsyncClient(
            timeout=settings.callback_timeout_seconds,
        )

    async def send_success(self, callback_url: str, callback_token: str, payload: Any) -> None:
        await self._send(callback_url, callback_token, payload)

    async def send_failure(self, callback_url: str, callback_token: str, payload: Any) -> None:
        await self._send(callback_url, callback_token, payload)

    @retry(
        retry=retry_if_exception_type((httpx.HTTPError, DependencyException)),
        stop=stop_after_attempt(4),
        wait=wait_exponential(multiplier=2, min=2, max=30),
        reraise=True,
    )
    async def _send(self, callback_url: str, callback_token: str, payload: Any) -> None:
        body = payload.model_dump(by_alias=True, exclude_none=True) if hasattr(payload, "model_dump") else payload
        body_bytes = json.dumps(body, ensure_ascii=False).encode("utf-8")
        
        headers = {
            "Content-Type": "application/json; charset=utf-8",
            "X-Callback-Token": callback_token,
            "X-Trace-Id": body.get("traceId", ""),
        }
        
        try:
            resp = await self._client.post(callback_url, content=body_bytes, headers=headers)
        except httpx.HTTPError as e:
            logger.warning(f"callback http error: {e}")
            raise DependencyException("java_callback", str(e))
        
        if resp.status_code >= 500:
            raise DependencyException("java_callback", f"server error {resp.status_code}")
        
        try:
            result = resp.json()
        except Exception:
            raise DependencyException("java_callback", f"invalid json response")
        
        if result.get("code") != "00000":
            logger.warning(f"callback business error: {result}")
            raise DependencyException("java_callback", f"business {result.get('code')}")
        
        logger.info(f"callback ok taskNo={body.get('taskNo')}")

    async def close(self) -> None:
        await self._client.aclose()
```

### 8.9 `app/services/model_registry_service.py`（关键）

```python
import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from loguru import logger

from app.core.config import settings
from app.core.exceptions import ModelNotLoadedException


@dataclass
class ModelEntry:
    name: str
    version: str
    artifact_md5: str
    released_at: str
    instance: Any = None  # 加载后的模型对象


class ModelRegistryService:
    """
    管理所有推理模型的版本、加载与切换。
    
    职责：
      - 从 artifacts/model_registry.json 读取注册表
      - 按配置加载模型到内存
      - 暴露 get(name) 给各个 Service 使用
      - 支持热切换（管理员接口触发）
      - 给 model-version 接口提供当前版本信息
    """

    MODELS = ["tooth_detect", "tpc_net", "edl_grade", "risk_fusion"]

    def __init__(self) -> None:
        self._registry_path = Path(settings.model_registry_path)
        self._entries: dict[str, ModelEntry] = {}
        self._release_version: str = "unknown"

    async def load_all(self) -> None:
        """服务启动时加载所有模型。"""
        if not self._registry_path.exists():
            logger.warning(f"model registry not found: {self._registry_path}, running with mocks")
            self._load_mocks()
            return
        
        registry = json.loads(self._registry_path.read_text(encoding="utf-8"))
        self._release_version = registry.get("release_version", "dev-mock")
        
        for name in self.MODELS:
            entry_data = registry.get(name)
            if entry_data is None:
                logger.warning(f"model {name} not in registry, using mock")
                self._entries[name] = self._mock_entry(name)
                continue
            
            entry = ModelEntry(
                name=name,
                version=entry_data["version"],
                artifact_md5=entry_data["artifact_md5"],
                released_at=entry_data["released_at"],
            )
            entry.instance = await self._load_instance(name, entry_data["artifact_path"])
            self._entries[name] = entry
            logger.info(f"loaded model {name} version={entry.version}")

    async def _load_instance(self, name: str, artifact_path: str) -> Any:
        """
        真实加载模型的钩子。
        各个模型在此分支加载，保持本方法的唯一入口。
        """
        path = Path(settings.model_root) / artifact_path
        if not path.exists():
            logger.warning(f"artifact missing {path}, fallback to mock")
            return None
        
        if name == "tooth_detect":
            from ultralytics import YOLO
            return YOLO(str(path))
        elif name == "tpc_net":
            from train.tpc_net.model import load_tpc_net_for_inference
            return load_tpc_net_for_inference(str(path))
        elif name == "edl_grade":
            from train.edl_grade.model import load_edl_model_for_inference
            return load_edl_model_for_inference(str(path))
        elif name == "risk_fusion":
            import joblib
            return joblib.load(str(path))
        else:
            return None

    def _load_mocks(self) -> None:
        for name in self.MODELS:
            self._entries[name] = self._mock_entry(name)
        self._release_version = "dev-mock"

    def _mock_entry(self, name: str) -> ModelEntry:
        return ModelEntry(
            name=name,
            version="mock-v0",
            artifact_md5="0" * 32,
            released_at="2026-01-01T00:00:00+08:00",
            instance=None,
        )

    def get(self, name: str) -> Any:
        entry = self._entries.get(name)
        if entry is None:
            raise ModelNotLoadedException(name)
        return entry.instance

    def version_of(self, name: str) -> dict:
        entry = self._entries.get(name)
        if entry is None:
            return {"modelVersion": "unknown", "modelArtifactMd5": "", "releasedAt": ""}
        return {
            "modelVersion": entry.version,
            "modelArtifactMd5": entry.artifact_md5,
            "releasedAt": entry.released_at,
        }

    def release_version(self) -> str:
        return self._release_version

    def all_versions(self) -> dict[str, dict]:
        return {name: self.version_of(name) for name in self.MODELS}

    async def unload_all(self) -> None:
        self._entries.clear()
```

### 8.10 `app/infra/storage/minio_client.py`

```python
from io import BytesIO
from pathlib import Path

from minio import Minio
from minio.error import S3Error
from loguru import logger

from app.core.config import settings
from app.core.exceptions import DependencyException


class MinioStorage:
    def __init__(self) -> None:
        self._client = Minio(
            endpoint=settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure,
        )

    def download_to_path(self, bucket: str, key: str, dest: Path) -> None:
        dest.parent.mkdir(parents=True, exist_ok=True)
        try:
            self._client.fget_object(bucket, key, str(dest))
        except S3Error as e:
            raise DependencyException("minio", f"download {bucket}/{key}: {e}")

    def upload_bytes(self, bucket: str, key: str, data: bytes, content_type: str = "image/png") -> None:
        try:
            self._client.put_object(
                bucket_name=bucket,
                object_name=key,
                data=BytesIO(data),
                length=len(data),
                content_type=content_type,
            )
        except S3Error as e:
            raise DependencyException("minio", f"upload {bucket}/{key}: {e}")

    def exists(self, bucket: str, key: str) -> bool:
        try:
            self._client.stat_object(bucket, key)
            return True
        except S3Error:
            return False
```

### 8.11 `app/api/v1/health.py`

```python
from fastapi import APIRouter, Request
from app.schemas.common import ApiResponse

router = APIRouter()


@router.get("/health", response_model=ApiResponse)
async def health(request: Request) -> ApiResponse:
    registry = request.app.state.model_registry
    return ApiResponse.success(
        data={
            "status": "UP",
            "releaseVersion": registry.release_version(),
            "models": registry.all_versions(),
        },
        trace_id=getattr(request.state, "trace_id", None),
    )
```

### 8.12 `app/api/v1/model_version.py`

```python
from fastapi import APIRouter, Request
from app.schemas.common import ApiResponse

router = APIRouter()


@router.get("/model-version", response_model=ApiResponse)
async def model_version(request: Request) -> ApiResponse:
    registry = request.app.state.model_registry
    return ApiResponse.success(
        data=registry.all_versions(),
        trace_id=getattr(request.state, "trace_id", None),
    )
```

---

## 9. Java ↔ Python 协同协议

### 9.1 协同模式冻结

| 链路 | 方式 | 条件 |
|---|---|---|
| Java → Python 轻量调用 | HTTP 同步 | 质量检查、风险评估、模型版本查询 |
| Java → Python 重任务 | HTTP 同步受理 + Python 后台处理 | `analyze` 接口 |
| Python → Java 结果回调 | HTTP 同步回调 | 所有分析结果 |
| MQ 通道 | HTTP 失败时兜底 | 发布到 `cg.analysis.dlx` |
| 文件访问 | MinIO object key 引用 | 所有影像与可视化资产 |

**一期主路径：HTTP + BackgroundTasks**，MQ 路径作为可选启用项（通过 `settings.enable_mq_consumer` 控制）。

### 9.2 协议字段来源

**不在本节重复字段定义**，全部以《Java ↔ Python 接口契约对照表》为准。本节只约定协同行为：

1. Java 发送 `/ai/v1/analyze` 前，必须已生成 `taskNo` 并写入 `ana_task_record`
2. Java 生成一次性 `callbackToken` 写入 Redis，key = taskNo
3. Python 必须在 2 小时内完成处理并回调（超时 Java 侧会主动标记 TIMEOUT）
4. Python 回调失败达到最大重试次数后，发布 `analysis.failed` 到 MQ，走兜底通道
5. Java 侧 `/api/v1/ai/callbacks/analysis` 必须幂等，重复回调同一 taskNo 返回相同成功码

### 9.3 Python 端必须支持的能力

- 识别 `taskNo` / `caseId` / `patientId` / `orgId` / `caseNo`
- 识别并使用 `modelVersion`（不传则使用当前默认）
- 接收 images 数组并从 MinIO 下载
- 接收 `patientProfile`
- 输出 summary / lesionResults / toothDetections / riskAssessment / qualityCheckResults / rawResultJson
- 失败时带 errorCode / errorMessage / errorStage / retryable
- 所有回调以 `taskNo` 为幂等主键
- 回调携带 `X-Callback-Token` header

---

## 10. 幂等、重试、容错与可观测性

### 10.1 任务级幂等

Python 端使用 Redis 分布式锁防止重复处理：

```python
# app/pipelines/inference_pipeline.py 内部
lock_key = f"cg:task:{payload.task_no}"
lock_ttl = 600  # 10 分钟

if not await redis.set(lock_key, "1", nx=True, ex=lock_ttl):
    logger.warning(f"task {payload.task_no} already processing, skip")
    return
try:
    await self._run_internal(payload, trace_id)
finally:
    await redis.delete(lock_key)
```

### 10.2 回调级重试

由 `CallbackService._send` 方法通过 tenacity 实现：
- 最多 4 次（1 次首发 + 3 次重试）
- 等待：2s / 8s / 30s
- 触发条件：HTTP 错误、5xx、业务 code 非 `00000`

### 10.3 MQ 兜底

当回调达到最大重试仍失败：
1. 向 `cg.analysis.exchange` 发布 `analysis.failed` 消息
2. 消息体与失败回调 JSON 结构一致
3. Java 侧消费者处理 DLX 消息时同样走幂等落库

### 10.4 日志与可观测性

- 所有日志使用 `loguru`，统一格式，带 `trace_id` 上下文
- 关键耗时节点写入 `rawResultJson.inferenceSecondsByStage`
- 日志级别：
  - DEBUG：张量 shape、中间结果
  - INFO：阶段完成、模型加载、回调成功
  - WARNING：可重试异常、降级路径
  - ERROR：不可恢复异常

### 10.5 监控指标（预留接口）

虽然一期不强制上 Prometheus，但所有 pipeline 必须把以下指标写入 `/tmp/cariesguard/metrics.jsonl`：

```json
{"ts": "...", "taskNo": "...", "stage": "segmentation", "elapsed_ms": 4600, "status": "ok"}
```

后期升级时直接接入 `prometheus_client` 即可。

---

# 第三部分：算法实现细节

## 11. TPC-Net 实现指南（含伪代码）

### 11.1 目标与定位

TPC-Net 的定位是：

> **在成熟分割骨干 SegFormer-B2 基础上，通过 Tooth Position Embedding (TPE) 与 Cross-View Consistency (CVC) 两项增强，使分割结果更贴合口腔场景。**

这不是"发明一个新网络"，而是"在已有 backbone 上做可答辩的合理增强"。国一答辩时的卖点是**消融实验**，不是 SOTA。

### 11.2 整体网络结构

```text
Input image (B, 3, H, W)
      │
      ▼
SegFormer-B2 Encoder
  ├── Stage 1 output  (B, 64,  H/4,  W/4)
  ├── Stage 2 output  (B, 128, H/8,  W/8)
  ├── Stage 3 output  (B, 320, H/16, W/16)   ← TPE 注入点
  └── Stage 4 output  (B, 512, H/32, W/32)
      │
      ▼
SegFormer Decoder (All-MLP)
      │
      ▼
Segmentation Head
      │
      ▼
Output mask (B, num_classes, H, W)
```

**关键决策**：
- 使用 `timm` 或 HuggingFace `transformers` 中的预训练 SegFormer-B2 权重
- TPE 注入在 **Stage 3 输出后**（比中更深一点的位置），语义更丰富且仍保留空间信息
- 分类数：`num_classes = 2`（背景 / 病灶），后续分级由 EDL 模块单独承担

### 11.3 Tooth Position Embedding（TPE）实现

#### 11.3.1 数据准备

每张训练图像同时提供：
- 原图张量 `image: (3, H, W)`
- 分割 mask 标签 `mask: (H, W)`
- **牙位图** `tooth_map: (H, W)`，每个像素的值是 FDI 编号（0 表示背景，1~32 表示 32 颗牙位的位置索引）

牙位图从 YOLOv8 牙位检测结果生成：
- 对每个检测到的 bbox，把 bbox 区域填入 FDI 编号
- 若 bbox 重叠则取检测分数更高的

#### 11.3.2 TPE 模块伪代码

```python
# train/tpc_net/model.py 节选

import torch
import torch.nn as nn
import torch.nn.functional as F


class ToothPositionEmbedding(nn.Module):
    """
    Tooth Position Embedding（TPE）
    
    输入：
      feat: (B, C_in, h, w)    —— SegFormer Stage 3 输出
      tooth_map: (B, H, W)     —— 原图尺寸的牙位图，整数张量 [0, 32]
    
    输出：
      feat_out: (B, C_in, h, w) —— 注入了牙位先验的特征
    """

    def __init__(self, in_channels: int = 320, embed_dim: int = 32, num_tooth: int = 33):
        super().__init__()
        # num_tooth = 32 颗牙 + 1 个背景
        self.embedding = nn.Embedding(num_embeddings=num_tooth, embedding_dim=embed_dim)
        self.proj = nn.Conv2d(in_channels + embed_dim, in_channels, kernel_size=1, bias=False)
        self.bn = nn.BatchNorm2d(in_channels)

    def forward(self, feat: torch.Tensor, tooth_map: torch.Tensor) -> torch.Tensor:
        B, C, h, w = feat.shape

        # 1. 把 tooth_map 从 (B, H, W) 下采样到 (B, h, w)
        #    使用 nearest 保证类别整数不被插值破坏
        tooth_map_small = F.interpolate(
            tooth_map.unsqueeze(1).float(),
            size=(h, w),
            mode="nearest",
        ).squeeze(1).long()  # (B, h, w)

        # 2. 查 embedding → (B, h, w, embed_dim) → (B, embed_dim, h, w)
        pos_embed = self.embedding(tooth_map_small)      # (B, h, w, E)
        pos_embed = pos_embed.permute(0, 3, 1, 2).contiguous()  # (B, E, h, w)

        # 3. concat 到原特征
        fused = torch.cat([feat, pos_embed], dim=1)     # (B, C+E, h, w)

        # 4. 1x1 conv 投影回原维度 + BN
        out = self.proj(fused)                           # (B, C, h, w)
        out = self.bn(out)

        # 5. 残差连接，保证没有 TPE 时退化为原 backbone
        return feat + F.relu(out)
```

#### 11.3.3 关键张量形状对照表

| 变量 | 形状 | dtype | 说明 |
|---|---|---|---|
| `image` | `(B, 3, 512, 512)` | float32 | 原图输入 |
| `tooth_map` | `(B, 512, 512)` | int64 | 牙位图，值域 [0,32] |
| `stage3_feat` | `(B, 320, 32, 32)` | float32 | SegFormer Stage 3 输出 |
| `tooth_map_small` | `(B, 32, 32)` | int64 | nearest 下采样 |
| `pos_embed` | `(B, 32, 32, 32)` | float32 | embed_dim=32 |
| `fused` | `(B, 352, 32, 32)` | float32 | 320+32 |
| `out` | `(B, 320, 32, 32)` | float32 | 投影回原维度 |

#### 11.3.4 在 SegFormer 中的插入方式

```python
class TPCNet(nn.Module):
    def __init__(self, num_classes: int = 2, embed_dim: int = 32):
        super().__init__()
        # 1. SegFormer 主干 + Decoder，从 timm 或 transformers 加载
        from transformers import SegformerForSemanticSegmentation
        self.backbone = SegformerForSemanticSegmentation.from_pretrained(
            "nvidia/mit-b2",
            num_labels=num_classes,
            ignore_mismatched_sizes=True,
        )
        # 2. TPE 模块
        self.tpe = ToothPositionEmbedding(in_channels=320, embed_dim=embed_dim)

    def forward(self, image, tooth_map):
        # 手动跑 encoder，拦截 stage 3
        encoder = self.backbone.segformer.encoder
        hidden_states = encoder(image, output_hidden_states=True).hidden_states
        # hidden_states: tuple of 4 tensors, [stage1, stage2, stage3, stage4]
        
        s1, s2, s3, s4 = hidden_states
        s3_tpe = self.tpe(s3, tooth_map)
        
        # 替换 stage3 后，手动跑 decoder
        all_hidden = (s1, s2, s3_tpe, s4)
        logits = self.backbone.decode_head(all_hidden)  # (B, num_classes, h, w)
        
        # 上采样回原图尺寸
        logits = F.interpolate(logits, size=image.shape[-2:], mode="bilinear", align_corners=False)
        return logits
```

> **注意**：HuggingFace SegFormer 的 decoder 内部封装较深，实际实现时可能需要继承并 override。这里给出伪代码表达意图，实际代码请参照 `transformers` 源码。

### 11.4 Cross-View Consistency（CVC）实现

#### 11.4.1 数据准备

CVC 需要 **配对 dataset**，每个样本是同一颗牙的多视图：

```python
{
    "pairId": "PAIR000001",
    "toothCode": "16",
    "views": [
        {"image_path": "pan_01.jpg",   "modality": "PANORAMIC",  "mask_path": "mask_pan_01.png"},
        {"image_path": "intra_01.jpg", "modality": "INTRAORAL",  "mask_path": "mask_intra_01.png"},
    ],
    "severityCode": "C2",
}
```

#### 11.4.2 训练数据加载策略

```python
class PairedMultiViewDataset(Dataset):
    """
    每个 sample 返回一对（同一颗牙两个视图的张量）
    若某 tooth 只有单视图，跳过或作为普通样本进入 baseline 分支
    """
    def __init__(self, manifest_path: str, transform):
        self.pairs = load_jsonl(manifest_path)
        self.transform = transform

    def __len__(self):
        return len(self.pairs)

    def __getitem__(self, idx):
        pair = self.pairs[idx]
        views = pair["views"]
        # 只取前两个视图（pan + intra）
        view_a = self._load_view(views[0])
        view_b = self._load_view(views[1])
        return {
            "image_a": view_a["image"],      # (3, H, W)
            "mask_a":  view_a["mask"],       # (H, W)
            "tooth_map_a": view_a["tooth_map"],  # (H, W)
            "image_b": view_b["image"],
            "mask_b":  view_b["mask"],
            "tooth_map_b": view_b["tooth_map"],
            "tooth_code": pair["toothCode"],
        }
```

#### 11.4.3 CVC Loss 伪代码

CVC loss 的核心思想：同一颗牙在两个视图下，经过骨干网络提取的 **ROI 级特征向量** 应该在 embedding 空间接近。

```python
# train/tpc_net/model.py 节选

def roi_pool_feature(feat_map: torch.Tensor, tooth_map: torch.Tensor, target_tooth: int) -> torch.Tensor:
    """
    从特征图中用 mask pooling 提取特定牙位的特征向量。
    
    Args:
      feat_map: (B, C, h, w)  骨干网络 encoder 最终层输出（Stage 4）
      tooth_map: (B, H, W)    原图尺寸的牙位图
      target_tooth: int       目标 FDI 牙位编号
    
    Returns:
      roi_feat: (B, C) 每个 sample 该牙位的平均特征
    """
    B, C, h, w = feat_map.shape
    tooth_small = F.interpolate(
        tooth_map.unsqueeze(1).float(), size=(h, w), mode="nearest"
    ).squeeze(1).long()  # (B, h, w)
    
    # 构造该牙位的二值 mask
    tooth_mask = (tooth_small == target_tooth).float().unsqueeze(1)  # (B, 1, h, w)
    tooth_mask_sum = tooth_mask.sum(dim=[2, 3]).clamp(min=1.0)        # (B, 1)
    
    # masked average pooling
    pooled = (feat_map * tooth_mask).sum(dim=[2, 3]) / tooth_mask_sum  # (B, C)
    return pooled


def cvc_loss(
    feat_map_a: torch.Tensor,
    feat_map_b: torch.Tensor,
    tooth_map_a: torch.Tensor,
    tooth_map_b: torch.Tensor,
    target_tooth: int,
    temperature: float = 0.1,
) -> torch.Tensor:
    """
    Cross-View Consistency Loss
    
    使用 cosine similarity 约束：
      loss = 1 - cos(feat_a, feat_b)
    
    也可以换成 InfoNCE 风格（batch 内其他 pair 作为负样本），
    一期推荐先用简单版，调通后再考虑加负样本。
    """
    roi_a = roi_pool_feature(feat_map_a, tooth_map_a, target_tooth)  # (B, C)
    roi_b = roi_pool_feature(feat_map_b, tooth_map_b, target_tooth)  # (B, C)
    
    roi_a = F.normalize(roi_a, dim=-1)
    roi_b = F.normalize(roi_b, dim=-1)
    
    cos_sim = (roi_a * roi_b).sum(dim=-1)   # (B,)
    loss = (1.0 - cos_sim).mean()
    return loss
```

#### 11.4.4 总损失函数

```python
def compute_total_loss(
    logits_a, logits_b,
    mask_a, mask_b,
    feat_a, feat_b,
    tooth_map_a, tooth_map_b,
    target_tooth_codes: list[int],
    lambda_dice: float = 1.0,
    lambda_cvc: float = 0.3,
):
    # 1. 分割主损失（两个视图各自）
    seg_ce  = F.cross_entropy(logits_a, mask_a) + F.cross_entropy(logits_b, mask_b)
    seg_dice = dice_loss(logits_a, mask_a) + dice_loss(logits_b, mask_b)
    
    # 2. CVC loss —— 在 batch 内按每个 pair 的 target_tooth 计算
    cvc = 0.0
    for i, t in enumerate(target_tooth_codes):
        cvc = cvc + cvc_loss(
            feat_a[i:i+1], feat_b[i:i+1],
            tooth_map_a[i:i+1], tooth_map_b[i:i+1],
            target_tooth=t,
        )
    cvc = cvc / max(len(target_tooth_codes), 1)
    
    total = seg_ce + lambda_dice * seg_dice + lambda_cvc * cvc
    return total, {"seg_ce": seg_ce.item(), "seg_dice": seg_dice.item(), "cvc": float(cvc)}
```

### 11.5 训练配置建议

```yaml
# configs/model/tpc_net.yaml
model:
  name: TPCNet
  backbone: segformer-b2
  pretrained: nvidia/mit-b2
  num_classes: 2
  tpe_embed_dim: 32
  use_tpe: true
  use_cvc: true

training:
  batch_size: 8         # 注意 CVC 每个 sample 是一对，显存占用加倍
  num_epochs: 80
  learning_rate: 6e-5
  weight_decay: 1e-4
  optimizer: AdamW
  lr_scheduler: cosine
  warmup_epochs: 3
  lambda_dice: 1.0
  lambda_cvc: 0.3
  mixed_precision: true
  early_stopping_patience: 10

data:
  image_size: 512
  augmentation:
    - HorizontalFlip
    - RandomBrightnessContrast
    - GridDistortion
    - CoarseDropout
  normalize:
    mean: [0.485, 0.456, 0.406]
    std:  [0.229, 0.224, 0.225]

evaluation:
  metrics: [dice, iou, boundary_iou, precision, recall]
  save_best_by: dice
```

### 11.6 消融实验设计（答辩关键）

答辩时**必须**展示以下对比表：

| 配置 | Dice | IoU | Boundary IoU | 备注 |
|---|---|---|---|---|
| SegFormer-B2 baseline | - | - | - | 无 TPE 无 CVC |
| + TPE | - | - | - | 只加 TPE |
| + CVC | - | - | - | 只加 CVC |
| + TPE + CVC（TPC-Net 完整版） | - | - | - | 本项目方案 |

- 至少在保底 500 张数据上跑完这四组
- 每组用相同的 seed、相同的数据划分、相同的训练 epochs
- 报告格式参考 MMSegmentation 消融表风格

### 11.7 评估指标要求

- Dice（主指标）
- IoU（辅）
- Boundary IoU（小病灶敏感度）
- Precision / Recall（临床可信度）
- 按 FDI 分组的分牙位 Dice（展示 TPE 效果）
- 困难样本 case study（展示 CVC 效果）

---

## 12. EDL 分级实现指南（含伪代码）

### 12.1 为什么用 EDL

EDL（Evidential Deep Learning）能同时输出分类概率和 **不确定性** u，避免 softmax 的"过度自信"。

不确定性的业务价值：
- u 高 → 医生必须复核
- u 高 → 优先进入回流池
- u 高 → 报告文本使用保守措辞

### 12.2 EDL 的数学本质（简版）

把 softmax 分类头换成 Dirichlet 参数化：

```text
net output α_k = exp(f_k)        (k=0..K-1)
S = Σ α_k
p_k = α_k / S     （预测概率）
u = K / S         （不确定性，范围 [0, 1]）
```

- 当所有 α_k 都很大 → S 大 → u 小 → 模型很确定
- 当所有 α_k 都接近 1 → S ≈ K → u ≈ 1 → 模型完全不确定

### 12.3 网络结构

```text
Input: ROI image (B, 3, 112, 112)
        ↓
Backbone: ResNet-18 / MobileNetV3 / tiny ViT
        ↓
Global Avg Pool  → (B, feat_dim)
        ↓
FC + ReLU        → (B, 128)
        ↓
EDL Head         → (B, K) = α (必须 > 0)
```

**K = 4**，对应 C0/C1/C2/C3。

### 12.4 EDL Head 伪代码

```python
# train/edl_grade/model.py

import torch
import torch.nn as nn
import torch.nn.functional as F


class EDLHead(nn.Module):
    """
    输出 Dirichlet alpha 参数。
    
    实现方式：
      raw = Linear(feat)            # (B, K)
      evidence = softplus(raw)      # 确保 >= 0
      alpha = evidence + 1          # 确保 >= 1
    """

    def __init__(self, in_dim: int = 128, num_classes: int = 4):
        super().__init__()
        self.fc = nn.Linear(in_dim, num_classes)
        self.num_classes = num_classes

    def forward(self, feat: torch.Tensor) -> torch.Tensor:
        raw = self.fc(feat)                  # (B, K)
        evidence = F.softplus(raw)           # (B, K)
        alpha = evidence + 1.0               # (B, K)
        return alpha

    @staticmethod
    def compute_probs_and_uncertainty(alpha: torch.Tensor) -> tuple[torch.Tensor, torch.Tensor]:
        """
        从 alpha 计算 p 和 u
        
        Returns:
          probs: (B, K)
          uncertainty: (B,) in [0, 1]
        """
        K = alpha.shape[-1]
        S = alpha.sum(dim=-1, keepdim=True)   # (B, 1)
        probs = alpha / S                     # (B, K)
        u = K / S.squeeze(-1)                 # (B,)
        return probs, u


class EDLGradingModel(nn.Module):
    def __init__(self, num_classes: int = 4):
        super().__init__()
        from torchvision.models import resnet18
        backbone = resnet18(weights="IMAGENET1K_V1")
        self.backbone = nn.Sequential(*list(backbone.children())[:-1])  # drop fc
        self.flatten = nn.Flatten()
        self.proj = nn.Sequential(
            nn.Linear(512, 128),
            nn.ReLU(inplace=True),
            nn.Dropout(0.1),
        )
        self.edl_head = EDLHead(in_dim=128, num_classes=num_classes)

    def forward(self, image: torch.Tensor) -> torch.Tensor:
        feat = self.backbone(image)    # (B, 512, 1, 1)
        feat = self.flatten(feat)      # (B, 512)
        feat = self.proj(feat)         # (B, 128)
        alpha = self.edl_head(feat)    # (B, K)
        return alpha
```

### 12.5 EDL Loss 伪代码

EDL 标准损失由两部分构成：**期望均方误差** + **KL 散度正则化**。

```python
def edl_loss(alpha: torch.Tensor, target: torch.Tensor, num_classes: int, 
             kl_annealing_coef: float = 1.0) -> torch.Tensor:
    """
    Args:
      alpha: (B, K)   —— Dirichlet 参数
      target: (B,)    —— 类别标签 [0, K-1]
      kl_annealing_coef: KL 项权重，前 10 个 epoch 线性从 0 升到 1
    """
    B, K = alpha.shape
    S = alpha.sum(dim=-1, keepdim=True)                 # (B, 1)
    y_onehot = F.one_hot(target, num_classes=K).float() # (B, K)
    
    # 1. 期望均方误差
    prob = alpha / S
    mse = (y_onehot - prob).pow(2).sum(dim=-1)
    var = (prob * (1.0 - prob) / (S + 1.0).squeeze(-1)).sum(dim=-1)
    mse_term = mse + var
    
    # 2. KL 散度正则化（只惩罚错误类别的 evidence）
    alpha_tilde = y_onehot + (1 - y_onehot) * alpha      # 将正确类的 alpha 置 1
    kl = kl_dirichlet_uniform(alpha_tilde, K)
    
    loss = mse_term.mean() + kl_annealing_coef * kl.mean()
    return loss


def kl_dirichlet_uniform(alpha: torch.Tensor, K: int) -> torch.Tensor:
    """
    KL( Dir(alpha) || Dir(1,1,...,1) )
    """
    S = alpha.sum(dim=-1, keepdim=True)
    lnB = torch.lgamma(S).squeeze(-1) - torch.lgamma(alpha).sum(dim=-1)
    lnB_uni = torch.lgamma(torch.tensor(float(K))) - K * torch.lgamma(torch.tensor(1.0))
    
    dg0 = torch.digamma(S)
    dg1 = torch.digamma(alpha)
    
    kl = lnB + lnB_uni.to(alpha.device) + ((alpha - 1) * (dg1 - dg0)).sum(dim=-1)
    return kl
```

### 12.6 关键张量形状对照表

| 变量 | 形状 | 说明 |
|---|---|---|
| `roi_image` | `(B, 3, 112, 112)` | ROI crop |
| `feat_after_backbone` | `(B, 512, 1, 1)` | ResNet-18 output |
| `feat_flatten` | `(B, 512)` | |
| `feat_proj` | `(B, 128)` | |
| `alpha` | `(B, 4)` | Dirichlet 参数 |
| `probs` | `(B, 4)` | 类别概率 |
| `uncertainty` | `(B,)` | 不确定性 [0, 1] |

### 12.7 训练与评估要求

**训练配置**：
- Optimizer: AdamW, lr=1e-4
- Batch size: 32
- Epochs: 60
- KL annealing: 前 10 个 epoch 线性从 0 升到 1
- 数据增强：水平翻转、随机亮度/对比度、颜色抖动

**评估指标**（除常规 Accuracy / Macro-F1 外）：
1. **Expected Calibration Error (ECE)** —— 校准性
2. **高不确定性阈值 → 拒识率 / 错误率曲线**（核心答辩材料）
3. **高置信错误率**（越低越好，体现"不会错得很自信"）

### 12.8 不确定性的业务联动

训练完成后，EDL 输出的 `uncertainty` 值必须在推理 pipeline 中产生以下动作：

```python
# app/services/grading_service.py 节选
if uncertainty > 0.4:
    lesion.review_recommended_flag = "1"
if uncertainty > 0.6:
    lesion.text_template_variant = "CONSERVATIVE"  # 报告使用保守措辞
```

阈值 0.4 / 0.6 是**初始建议**，训练完成后根据验证集上的 ECE 曲线重新校准。

---

## 13. 风险融合模块实现指南

### 13.1 定位

风险融合 **不是** 核心创新，定位为"平台能力补全"。方案选择优先级：

1. **LightGBM** （首选，一期默认）—— 训练快、SHAP 解释原生、数据量需求低
2. XGBoost（备选）
3. Logistic Regression（兜底，可解释性最强）
4. 小型 MLP（只有在数据量 > 2000 时才考虑）

**不用深度模型**的原因：数据量不足、可解释性差、答辩不讨好。

### 13.2 特征工程

**输入特征清单**（全部来自回调 payload 中的 summary + patientProfile）：

| 特征 | 类型 | 取值 |
|---|---|---|
| `overall_highest_severity_ord` | int | C0=0, C1=1, C2=2, C3=3 |
| `suspicious_tooth_count` | int | ≥0 |
| `overall_uncertainty_score` | float | [0,1] |
| `lesion_area_ratio` | float | [0,1] |
| `age` | int | |
| `gender_m` | int | one-hot |
| `gender_f` | int | one-hot |
| `brushing_frequency_ord` | int | NONE=0, ONCE=1, TWICE=2, MORE=3 |
| `sugar_diet_level_ord` | int | LOW=0, MEDIUM=1, HIGH=2 |
| `fluoride_use` | int | 0/1 |
| `previous_caries_count` | int | |
| `last_dental_check_months` | int | |

**训练标签**：`risk_level_code ∈ {LOW, MEDIUM, HIGH}`

### 13.3 训练伪代码

```python
# train/risk_fusion/train.py

import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score
import lightgbm as lgb
import joblib

from train.risk_fusion.config import RiskFusionConfig


def train(cfg: RiskFusionConfig) -> None:
    df = pd.read_csv(cfg.dataset_path)
    X = df[cfg.feature_cols]
    y = df["risk_level_code"].map({"LOW": 0, "MEDIUM": 1, "HIGH": 2})
    
    X_train, X_val, y_train, y_val = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    
    model = lgb.LGBMClassifier(
        n_estimators=300,
        learning_rate=0.05,
        num_leaves=31,
        max_depth=-1,
        class_weight="balanced",
        random_state=42,
    )
    model.fit(
        X_train, y_train,
        eval_set=[(X_val, y_val)],
        callbacks=[lgb.early_stopping(stopping_rounds=30)],
    )
    
    # 评估
    y_pred = model.predict(X_val)
    y_proba = model.predict_proba(X_val)
    print(classification_report(y_val, y_pred, target_names=["LOW", "MEDIUM", "HIGH"]))
    print("Macro AUC:", roc_auc_score(y_val, y_proba, multi_class="ovr", average="macro"))
    
    # 保存
    joblib.dump(model, cfg.output_path)


def explain(model_path: str, sample: dict) -> list[dict]:
    """
    推理时用，返回 top-k SHAP 贡献因子
    """
    import shap
    model = joblib.load(model_path)
    explainer = shap.TreeExplainer(model)
    X = pd.DataFrame([sample])
    shap_values = explainer.shap_values(X)
    
    contributions = []
    # shap_values: list of (1, n_features) for each class
    # 简化：只看预测类别的贡献
    pred_class = model.predict(X)[0]
    for i, feat in enumerate(X.columns):
        contributions.append({
            "featureCode": feat,
            "contribution": round(float(shap_values[pred_class][0, i]), 4),
            "direction": "POSITIVE" if shap_values[pred_class][0, i] > 0 else "NEGATIVE",
        })
    contributions.sort(key=lambda c: abs(c["contribution"]), reverse=True)
    return contributions[:5]
```

### 13.4 推理时的 RiskService

```python
# app/services/risk_service.py

from train.risk_fusion.train import explain

class RiskService:
    def __init__(self, registry):
        self.model = registry.get("risk_fusion")

    async def assess(self, lesion_results, patient_profile) -> "RiskAssessment":
        features = self._build_features(lesion_results, patient_profile)
        if self.model is None:
            return self._mock_assessment(features)
        
        X = pd.DataFrame([features])
        pred_class_idx = int(self.model.predict(X)[0])
        pred_proba = self.model.predict_proba(X)[0]
        
        level_map = {0: "LOW", 1: "MEDIUM", 2: "HIGH"}
        risk_level = level_map[pred_class_idx]
        risk_score = int(pred_proba[pred_class_idx] * 100)
        
        explanation = explain(settings.model_root + "/risk_fusion/latest.pkl", features)
        
        return RiskAssessment(
            risk_level_code=risk_level,
            risk_score=risk_score,
            recommended_cycle_days=self._recommend_cycle(risk_level),
            explanation_factors=explanation,
            model_version=self._model_version(),
        )
    
    def _recommend_cycle(self, risk_level: str) -> int:
        return {"LOW": 365, "MEDIUM": 180, "HIGH": 90}[risk_level]
```

### 13.5 数据量不足时的降级方案

当带完整结构化特征的病例 < 200 例时，降级为 **规则引擎**：

```python
def rule_based_risk(features: dict) -> dict:
    score = 0
    # 影像侧
    if features["overall_highest_severity_ord"] >= 2:
        score += 30
    if features["suspicious_tooth_count"] >= 3:
        score += 15
    # 行为
    if features["brushing_frequency_ord"] < 2:
        score += 15
    if features["sugar_diet_level_ord"] >= 2:
        score += 10
    if features["fluoride_use"] == 0:
        score += 5
    # 既往
    if features["previous_caries_count"] >= 2:
        score += 15
    if features["last_dental_check_months"] > 24:
        score += 10
    
    if score >= 60:
        return {"risk_level_code": "HIGH", "risk_score": min(score, 100)}
    if score >= 30:
        return {"risk_level_code": "MEDIUM", "risk_score": score}
    return {"risk_level_code": "LOW", "risk_score": score}
```

规则引擎的答辩口径：**"我们选择可解释的规则引擎作为一期方案，保留后续数据到位后升级到 LightGBM 的路径"**，不吃亏。

---

## 14. 可解释性与可视化

### 14.1 三层解释策略

| 层次 | 面向对象 | 输出 |
|---|---|---|
| 像素级 | 医生 | mask 二值图 + overlay 叠加图 |
| 特征级 | 医生 / 科研 | 风险融合的 SHAP 贡献因子 |
| 文本级 | 患者 | 通俗化结果说明（由 Java 报告模块生成） |

### 14.2 可视化资产的 MinIO 命名

```text
visual/{yyyy}/{MM}/{dd}/{caseNo}/mask_{imageId}_{toothCode}.png
visual/{yyyy}/{MM}/{dd}/{caseNo}/overlay_{imageId}_{toothCode}.png
visual/{yyyy}/{MM}/{dd}/{caseNo}/heatmap_{imageId}.png
```

### 14.3 Overlay 绘制伪代码

```python
# app/services/explain_service.py

import cv2
import numpy as np

def render_overlay(image_bgr: np.ndarray, mask_bin: np.ndarray, severity_code: str) -> bytes:
    """
    在原图上叠加病灶轮廓与颜色。
    
    Args:
      image_bgr: (H, W, 3) uint8
      mask_bin: (H, W) uint8, 0/1
      severity_code: C0~C3，决定叠加色
    
    Returns:
      PNG 字节流
    """
    color_map = {
        "C0": (0, 255, 0),    # 绿
        "C1": (0, 255, 255),  # 黄
        "C2": (0, 165, 255),  # 橙
        "C3": (0, 0, 255),    # 红
    }
    color = color_map.get(severity_code, (0, 0, 255))
    
    overlay = image_bgr.copy()
    overlay[mask_bin > 0] = color
    blended = cv2.addWeighted(overlay, 0.4, image_bgr, 0.6, 0)
    
    # 画轮廓
    contours, _ = cv2.findContours(mask_bin, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    cv2.drawContours(blended, contours, -1, color, 2)
    
    ok, buf = cv2.imencode(".png", blended)
    if not ok:
        raise RuntimeError("failed to encode overlay png")
    return buf.tobytes()
```

### 14.4 不生成什么

- ❌ 不生成 Grad-CAM 等 CAM 系列图（分割模型无此需求）
- ❌ 不生成反事实解释图（v1 曾提到"健康行为模拟"，已降级为患者教育文字，不绘图）
- ❌ 不把模型内部 attention 图暴露给用户（只做工程侧 debug 用）

---


# 第四部分：训练与数据治理

## 15. 训练体系总体设计

### 15.1 训练子系统边界

训练子系统位于 `train/` 目录下，**严格禁止**与 `app/` 目录互相 import。训练代码只能从以下路径读取数据：

- `datasets/desensitized/` —— 脱敏副本层
- `datasets/snapshots/` —— 版本化数据快照

**禁止**连接任何业务数据库，**禁止**直接读取 MinIO 的 `caries-image` bucket。

### 15.2 四条训练线

| 训练线 | 目录 | 依赖数据 | 产出 |
|---|---|---|---|
| 牙位检测 | `train/tooth_detect/` | 全景片 + bbox 标注 | `tooth_detect_{version}.pt` |
| TPC-Net 分割 | `train/tpc_net/` | 全景片 + 口内照 + mask + FDI 映射 + 跨视图配对 | `tpc_net_{version}.pt` |
| EDL 分级 | `train/edl_grade/` | 病灶 ROI + C0~C3 标签 | `edl_grade_{version}.pt` |
| 风险融合 | `train/risk_fusion/` | 病例级结构化特征 | `risk_fusion_{version}.pkl` |

四条训练线彼此独立，但存在**执行顺序依赖**：

```text
W4: 牙位检测（独立可先跑，为 TPE 提供牙位图）
  ↓
W5~W6: TPC-Net 分割（baseline → +TPE → +CVC → 完整版）
  ↓
W6~W7: EDL 分级（依赖 TPC-Net 产生的 ROI）
  ↓
W7: 风险融合（依赖影像侧结果 + 结构化特征）
```

### 15.3 训练产物归档

每次正式训练（非探索性实验）必须产出以下归档文件，全部放在 `artifacts/models/{model_name}/{version}/` 下：

```text
artifacts/models/tpc_net/cg_tpc_net_2026_04_v1/
├── model.pt                  # 权重
├── config.yaml               # 训练时使用的完整 yaml
├── metrics.json              # 验证集与测试集指标
├── confusion_matrix.png      # 混淆矩阵
├── ablation_table.md         # 消融实验对比
├── dataset_snapshot_ref.txt  # 使用的数据快照版本号
├── git_commit.txt            # 训练时的 git commit hash
├── requirements_freeze.txt   # pip freeze 结果
└── training_log.txt          # 训练日志片段
```

**缺任何一个文件都不得注册到 model_registry.json**，这是硬规则，答辩时每一个文件都是证据。

---

## 16. 数据治理流程（引用数据清单）

### 16.1 权威前置依赖

**本节所有"数据规模""数据来源""标注进度"等事实性描述，均以《CariesGuard 数据现状清单与采集计划》为唯一权威来源。本文档不复述数据事实，只说工程实现。**

### 16.2 数据流转冻结流程

```text
业务运行数据（MySQL + MinIO caries-image）
        │
        │  tools/export_desensitized_data.py
        ▼
脱敏副本层（datasets/desensitized/）
        │
        │  tools/validate_dataset.py + 临床顾问复核
        ▼
标注质控层（datasets/desensitized/annotations/）
        │
        │  tools/build_snapshot.py
        ▼
训练数据快照（datasets/snapshots/v{n}/）
        │
        ▼
    训练代码读取
```

### 16.3 脱敏工具约束

`tools/export_desensitized_data.py` 必须做到：

1. 剥离 DICOM 头部所有 PII 字段
2. 文件名哈希化
3. 剥离 EXIF
4. 将姓名、电话、身份证号映射为匿名 ID
5. 日期模糊化到月份
6. 输出的 manifest.jsonl 中**禁止**包含任何原始业务 ID

### 16.4 医生修正回流的数据准入

Java 端的 `ana_correction_feedback` 表是医生修正的业务记录，但**不能**直接进入训练集。准入流程：

```text
Java 端 ana_correction_feedback 表
        │
        │  tools/export_corrections.py（只读业务库，只导出经审批的记录）
        ▼
datasets/desensitized/corrections/pending/
        │
        │  临床顾问人工复核 + 签字审批
        ▼
datasets/desensitized/corrections/approved/
        │
        │  纳入下一次 build_snapshot
        ▼
datasets/snapshots/v{n+1}/
```

**禁止**：
- 不经审批就把修正记录加入训练
- 训练代码直接访问 Java 业务库
- 修正回流触发"自动重训"——必须经人工审批才能进入下一次训练

---

## 17. 数据集版本管理与快照

### 17.1 快照命名

```text
datasets/snapshots/
├── v0_public_only/         # 只含公开数据集（Tufts + DENTEX）
├── v1_baseline/            # 公开 + 合作医院首批
├── v1_1_enhanced/          # 补充自采 + 跨视图配对
└── v1_2_final/             # 答辩冻结版
```

每个快照目录下必须有：

```text
v1_baseline/
├── manifest.jsonl          # 每条记录一个样本，含路径、标注、划分
├── splits/
│   ├── train.txt
│   ├── val.txt
│   └── test.txt
├── stats.json              # 样本分布统计
├── checksums.md5           # 所有文件 MD5
└── README.md               # 即 Dataset Card
```

### 17.2 快照冻结规则

一旦快照被用于**正式训练**（非探索实验），就必须满足：

- [ ] manifest.jsonl 不得修改（任何变更都要升版号）
- [ ] splits 按患者级划分，同一患者必在同一划分
- [ ] checksums.md5 已生成且每次训练前校验一次
- [ ] Dataset Card 已填写完整
- [ ] 有 tools/detect_leak.py 的泄漏检测通过报告

### 17.3 构建快照的工具

`tools/build_snapshot.py` 输入参数：

```bash
python tools/build_snapshot.py \
  --version v1_baseline \
  --include-sources public,partner_hospital_a \
  --split-by patient \
  --train-ratio 0.7 --val-ratio 0.15 --test-ratio 0.15 \
  --random-seed 42 \
  --output datasets/snapshots/v1_baseline
```

工具必须：
1. 按患者级划分，检测并拒绝同一患者跨划分的情况
2. 生成 checksums.md5
3. 生成 stats.json（按模态、按标注类型、按划分）
4. 生成 README.md 模板让人工填写剩余 Dataset Card 内容
5. 一次 build 失败整体回滚，不留半成品

---

## 18. 模型版本治理与发布审批

### 18.1 模型注册表

`artifacts/model_registry.json` 是本地的"生产模型目录"，结构：

```json
{
  "release_version": "cg_release_2026_04_v1",
  "released_at": "2026-04-10T10:00:00+08:00",
  "approved_by": "xxx",
  "approval_doc": "artifacts/reports/release_2026_04_v1.md",
  "tooth_detect": {
    "version": "cg_tooth_detect_2026_03_v2",
    "artifact_path": "tooth_detect/cg_tooth_detect_2026_03_v2/model.pt",
    "artifact_md5": "a3f5...",
    "released_at": "2026-03-15T10:00:00+08:00"
  },
  "tpc_net": { ... },
  "edl_grade": { ... },
  "risk_fusion": { ... }
}
```

### 18.2 发布审批流程（硬规则）

**禁止**任何模型未经以下流程就被写入 `model_registry.json`：

```text
1. 候选模型训练完成
   └─ 产物归档到 artifacts/models/{name}/{version}/

2. 离线评估
   └─ evaluation/offline_eval.py 产出 metrics.json
   └─ evaluation/regression_eval.py 对比当前线上版本

3. 人工审批
   └─ 填写 artifacts/reports/release_{version}.md
   └─ 项目负责人 + 算法负责人签字

4. 注册
   └─ tools/register_model.py 更新 model_registry.json
   └─ 同步更新 release_version 字段

5. 灰度验证
   └─ dev 环境跑 e2e 测试
   └─ 运行 24 小时稳定性观察

6. 正式启用
```

**硬规则**：禁止在推理服务里加"自动切换最新模型"逻辑。任何模型上线都必须经过人工审批。03 号总体设计文档已明确这一条。

### 18.3 回滚能力

`tools/rollback_model.py` 可以把 `model_registry.json` 回滚到上一个版本，并触发推理服务热重载。

**每次发布新模型前，必须能说出"如果出问题我怎么回滚"**，不能说清就不许发布。

---

# 第五部分：工程与验收

## 19. 环境与配置管理

### 19.1 Python 版本与依赖

- Python：**3.11**（固定，不用 3.12）
- 包管理：pip + requirements/*.txt 分层
- 虚拟环境：venv 或 conda，不强制

### 19.2 分层 requirements

```text
requirements/base.txt
  fastapi==0.115.0
  uvicorn[standard]==0.30.6
  pydantic==2.9.2
  pydantic-settings==2.5.2
  loguru==0.7.2
  httpx==0.27.2
  tenacity==9.0.0
  orjson==3.10.7

requirements/service.txt
  -r base.txt
  minio==7.2.8
  pika==1.3.2
  redis==5.0.8

requirements/train.txt
  torch==2.4.1
  torchvision==0.19.1
  timm==1.0.9
  transformers==4.45.1
  ultralytics==8.3.0
  albumentations==1.4.15
  lightgbm==4.5.0
  shap==0.46.0
  scikit-learn==1.5.2
  joblib==1.4.2
  pandas==2.2.3
  numpy==1.26.4
  opencv-python==4.10.0.84

requirements/dev.txt
  -r service.txt
  black==24.8.0
  ruff==0.6.9
  mypy==1.11.2
  ipython==8.27.0

requirements/test.txt
  -r service.txt
  pytest==8.3.3
  pytest-asyncio==0.24.0
  pytest-cov==5.0.0
  httpx==0.27.2
  respx==0.21.1
```

**版本冻结原则**：训练栈与服务栈分离，可以在不同机器部署。torch 版本固定是 2.4.1，不跟随 nightly。

### 19.3 配置加载原则

- 所有配置通过 `app/core/config.py` 的 `Settings` 加载
- 环境变量前缀 `CG_`
- 本地开发使用 `.env` 文件
- 生产部署使用环境变量注入
- **禁止**在代码里硬编码 URL、Secret、路径

### 19.4 Makefile 统一命令

```makefile
.PHONY: install dev test lint format train-tpc eval

install:
	pip install -r requirements/dev.txt

dev:
	uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload

test:
	pytest tests/ -v --cov=app --cov-report=term-missing

lint:
	ruff check app/ train/ tests/
	mypy app/

format:
	black app/ train/ tests/
	ruff check --fix app/ train/ tests/

train-tpc:
	python -m train.tpc_net.train --config configs/model/tpc_net.yaml

eval:
	python -m evaluation.offline_eval --config configs/model/tpc_net.yaml
```

---

## 20. 测试体系

### 20.1 测试分层

| 层 | 目录 | 目标 | 覆盖率要求 |
|---|---|---|---|
| 单元测试 | `tests/unit/` | 单个函数/类 | 核心业务模块 ≥ 70% |
| 集成测试 | `tests/integration/` | 多模块组合，mock 外部依赖 | 关键路径全覆盖 |
| **契约测试** | `tests/contract/` | 验证回调 schema 与对照表一致 | 100% |
| e2e 测试 | `tests/e2e/` | 全链路 mock 跑通 | 至少 1 条 happy path |

### 20.2 关键单元测试清单

- `test_schemas.py`：Pydantic 模型 camelCase ↔ snake_case 双向转换
- `test_callback_service.py`：重试策略、签名、异常分支
- `test_model_registry.py`：模型加载、版本查询、热切换
- `test_inference_pipeline.py`：各阶段成功/失败/超时分支
- `test_explain_service.py`：overlay 图像生成

### 20.3 契约测试（重点）

**契约测试是保证 Python 端不跑偏的核心机制。**

```python
# tests/contract/test_callback_schema.py
import json
from pathlib import Path

from app.schemas.callback import AnalysisCallbackPayload, FailureCallbackPayload


EXPECTED_FIELDS_SUCCESS = {
    "traceId", "taskNo", "taskStatusCode", "caseId", "patientId", "orgId",
    "modelVersion", "startedAt", "completedAt",
    "summary", "toothDetections", "lesionResults",
    "riskAssessment", "qualityCheckResults", "rawResultJson",
}

EXPECTED_FIELDS_SUMMARY = {
    "overallHighestSeverity", "suspiciousToothCount", "overallUncertaintyScore",
    "lesionAreaRatio", "reviewRecommendedFlag", "highRiskFlag",
}


def test_success_callback_has_all_required_fields():
    """确保成功回调体的字段集合与《Java ↔ Python 接口契约对照表》§10.2 完全一致"""
    sample = _build_sample_success_payload()
    body = sample.model_dump(by_alias=True, exclude_none=False)
    missing = EXPECTED_FIELDS_SUCCESS - set(body.keys())
    assert not missing, f"missing fields: {missing}"


def test_summary_block_fields():
    sample = _build_sample_success_payload()
    summary = sample.model_dump(by_alias=True)["summary"]
    missing = EXPECTED_FIELDS_SUMMARY - set(summary.keys())
    assert not missing, f"summary missing: {missing}"


def test_enum_values_in_allowed_set():
    sample = _build_sample_success_payload()
    assert sample.task_status_code in {"QUEUEING", "PROCESSING", "SUCCESS", "FAILED", "QUALITY_REJECT", "TIMEOUT", "CANCELLED"}
    assert sample.summary.overall_highest_severity in {"C0", "C1", "C2", "C3"}
    assert sample.summary.review_recommended_flag in {"0", "1"}
    assert sample.summary.high_risk_flag in {"0", "1"}


def test_no_snake_case_leak_in_output():
    """保证对外 JSON 里没有残留的 snake_case 字段"""
    sample = _build_sample_success_payload()
    body_str = json.dumps(sample.model_dump(by_alias=True))
    forbidden = ["task_no", "case_id", "patient_id", "trace_id", "model_version"]
    for f in forbidden:
        assert f not in body_str, f"snake_case leaked: {f}"


def test_contract_doc_sync():
    """把对照表 JSON 样例当 golden 文件进行对比"""
    golden_path = Path("tests/contract/fixtures/success_callback_golden.json")
    if not golden_path.exists():
        return  # 首次运行时生成
    expected = json.loads(golden_path.read_text())
    sample = _build_sample_success_payload()
    actual = sample.model_dump(by_alias=True, exclude_none=True)
    # 对比 key 结构，不对比值
    _assert_same_structure(actual, expected)
```

### 20.4 e2e mock 测试

`tests/e2e/test_e2e_mock.py`：

1. 启动内置 `httpx.MockTransport` 替代真实 Java 回调端点
2. 启动内置 fake MinIO（用本地文件系统）
3. 使用内置 mock 模型权重
4. 构造一个完整的 AnalyzeRequest
5. 调用 `POST /ai/v1/analyze`
6. 等待 BackgroundTasks 完成
7. 断言 mock Java 端收到了完整合规的回调体
8. 断言 fake MinIO 中生成了 overlay PNG

---

## 21. 部署与容器化

### 21.1 Dockerfile（推理服务）

```dockerfile
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    CG_APP_ENV=prod

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
        libgl1 libglib2.0-0 curl && \
    rm -rf /var/lib/apt/lists/*

COPY requirements/ /app/requirements/
RUN pip install --no-cache-dir -r requirements/service.txt && \
    pip install --no-cache-dir torch==2.4.1 torchvision==0.19.1 --index-url https://download.pytorch.org/whl/cu121

COPY app/ /app/app/
COPY train/ /app/train/
COPY configs/ /app/configs/
COPY artifacts/model_registry.json /app/artifacts/model_registry.json
COPY artifacts/models/ /app/artifacts/models/

EXPOSE 8001
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8001/ai/v1/health || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001", "--workers", "2"]
```

### 21.2 docker-compose 片段

```yaml
services:
  ai-python:
    build: ./ai-python
    image: cariesguard/ai-python:v2
    ports:
      - "8001:8001"
    environment:
      CG_APP_ENV: prod
      CG_JAVA_BASE_URL: http://caries-backend:8080
      CG_MINIO_ENDPOINT: minio:9000
      CG_RABBITMQ_HOST: rabbitmq
      CG_REDIS_HOST: redis
    depends_on:
      - minio
      - rabbitmq
      - redis
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
```

### 21.3 训练环境

训练环境**不进**生产 Docker 镜像，单独维护：

- 开发机：本地 venv + train.txt
- 训练机：Docker + `requirements/train.txt` + 显卡驱动
- 训练产物通过 `tools/package_model.py` 打包后手动放入 `artifacts/models/`

---

## 22. 阶段里程碑与答辩资产

### 22.1 与数据计划的里程碑对齐

本表与《数据现状清单与采集计划》§8 的 D0~D4 里程碑完全对齐。

| 周次 | 数据里程碑 | Python 里程碑 |
|---|---|---|
| W1~W2 | 数据规划、工具搭建 | 仓库脚手架、FastAPI 骨架、Mock Pipeline 跑通 |
| W3 | D0：公开数据集 snapshot | InferencePipeline 集成 mock 模型，与 Java 联调 L0~L1 |
| W4 | **D1：TPC-Net baseline 可训** | YOLOv8 牙位检测训练完成 + TPC-Net baseline 训练启动 |
| W5 | - | TPC-Net +TPE 实验 |
| W6 | **D2：保底数据到位** | TPC-Net +TPE+CVC 完整版训练 |
| W7 | D3：可用于正式训练 | EDL 分级训练 + 风险融合训练 |
| W8 | **D4：v1.0 snapshot 冻结** | 全链路 e2e 打通 + 候选模型注册 |
| W9~W10 | - | 消融实验 + 错误分析 + 答辩材料准备 |
| W11~W12 | - | 联调 L4 端到端 + 压测 + 回滚演练 |
| W13~W14 | - | 答辩预演 + 材料定稿 |
| W15~W16 | - | 最终冲刺 + 提交 |

### 22.2 关键 Go/No-Go 节点

- **W4 末**：如果 TPC-Net baseline 还没跑通 Dice > 0.7，触发 Plan B 预案
- **W6 末**：如果跨视图配对数据 < 80 对，触发 Plan C（砍 CVC）
- **W8 末**：如果 v1.0 snapshot 未能冻结，必须停止训练调整，优先保数据

---

## 23. 答辩证据链清单

**答辩现场必须能当场拿出以下证据**，缺一不可。

### 23.1 数据证据

- [ ] Dataset Card v1.0（完整版，含所有统计表）
- [ ] 数据来源清单（公开 / 合作医院 / 自采的数量分布）
- [ ] 患者级划分证明（detect_leak.py 输出）
- [ ] 标注一致性评估（双人 Dice + kappa）
- [ ] 伦理审批文件或说明

### 23.2 模型证据

- [ ] 四组消融实验对比表（baseline / +TPE / +CVC / +TPE+CVC）
- [ ] EDL 校准曲线（ECE、Reliability Diagram）
- [ ] EDL 高不确定性 → 拒识率 / 错误率曲线
- [ ] 风险融合的 SHAP 贡献图
- [ ] 困难样本 case study（至少 5 例）

### 23.3 工程证据

- [ ] `model_registry.json` 当前发布版本
- [ ] 发布审批文档 `artifacts/reports/release_*.md`
- [ ] Java ↔ Python 接口契约对照表（已双方签字）
- [ ] L0~L4 联调通过记录
- [ ] 回滚演练记录

### 23.4 可信性证据（加分项）

- [ ] 业务库 vs 训练库边界的代码级证明（训练代码不含 pymysql/sqlalchemy 连接业务库的字符串）
- [ ] 医生修正回流的审批流程截图
- [ ] 失败回调与 DLX 兜底的日志证据
- [ ] 不确定性驱动复核的真实例子

### 23.5 答辩现场可演示内容

- [ ] 实时上传一张全景片 → 看到 overlay 结果与分级
- [ ] 上传一张故意模糊的图片 → 看到质量拒收
- [ ] 上传一个不确定病例 → 看到"建议复核"标记
- [ ] 进入医生修正界面 → 修正 → 展示修正被写入待审批池
- [ ] 打开 `/ai/v1/model-version` → 展示当前版本栈

---

## 24. v1 三份文档取代映射

**以下三份 v1 文档自 v2 签发日起标记为 deprecated，不再维护。任何开发活动以本 v2 文档为准。**

| v1 文档 | 状态 | v2 继承位置 |
|---|---|---|
| CariesGuard_Python端统一实施开发文档.md | **deprecated** | §1~§24 整合 |
| CariesGuard_Python仓库初始化模板与FastAPI项目骨架文档.md | **deprecated** | §4 目录结构 + §8 代码骨架 |
| CariesGuard_requirements与Python仓库脚手架代码清单.md | **deprecated** | §4 目录结构 + §8 代码骨架 + §19 依赖管理 |

### 24.1 v1 内容保留与删除

- **保留并增强**：架构分层、四条红线、数据治理流程、模型治理、Dataset Card 模板
- **修订后保留**：接口契约（改为引用对照表）、代码骨架（升级为可运行版）
- **新增**：TPE/CVC/EDL 伪代码、张量 shape 对照、契约测试、数据计划对齐
- **删除**：v1 中未明确边界的"先进 MLOps 平台"相关描述（不做承诺外的事）
- **删除**：v1 中过度理想化的数据规模假设

### 24.2 冲突仲裁规则

一旦 v1 与 v2 存在任何冲突，**以 v2 为准**。开发过程中若发现 v2 本身有歧义或错误，按以下流程处理：

1. 在项目 issue 中开 `[PY-IMPL-V2]` 标签的 issue
2. Python 负责人 + 项目负责人 review
3. 通过后修订 v2 并升小版号（v2.0 → v2.1）
4. 同步更新相关联的《接口契约对照表》和《数据清单》

---

## 附录 A：与其他文档的关系图

```text
《项目总体设计文档》(03 号)
    │
    ├──→《Java 后端开发说明书》(04 号)
    │       │
    │       └──→《Java 后端命名规范》(05 号)
    │
    ├──→《数据库总体设计》(06 号)
    │       │
    │       └──→《完整数据字典》(07 号)
    │
    ├──→《数据集采集与标注规范》(01 号)
    │       │
    │       └──→《CariesGuard 数据现状清单与采集计划》【B 文档】
    │
    └──→《CariesGuard Python 端统一实施开发文档 v2》【本文档 C】
            │
            ├──→《Java ↔ Python 接口契约对照表》【A 文档】
            │
            └──→《CariesGuard 数据现状清单与采集计划》【B 文档】
```

## 附录 B：术语表

| 术语 | 全称 | 说明 |
|---|---|---|
| TPE | Tooth Position Embedding | 牙位位置嵌入 |
| CVC | Cross-View Consistency | 跨视图一致性约束 |
| EDL | Evidential Deep Learning | 证据学习（Dirichlet 参数化不确定性） |
| FDI | Fédération Dentaire Internationale | 国际通用牙位编号系统 |
| ROI | Region of Interest | 感兴趣区域（分级用病灶 crop） |
| ECE | Expected Calibration Error | 期望校准误差 |
| DLX | Dead Letter Exchange | 死信交换机 |
| SHAP | SHapley Additive exPlanations | 特征贡献解释方法 |

---

**签发**

| 角色 | 姓名 | 签字 | 日期 |
|---|---|---|---|
| Python 负责人 | | | |
| 算法负责人 | | | |
| Java 负责人 | | | |
| 项目总负责人 | | | |
