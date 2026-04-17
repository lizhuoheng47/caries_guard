-- CariesGuard MySQL 双库基线
-- 由 docker-entrypoint-initdb.d 在 MySQL 容器首次启动时执行。
-- caries_biz：Java 业务平台库（系统/患者/病例/影像/analysis 业务快照/报告/随访/通知）
-- caries_ai ：Python AI / RAG / 模型治理库（推理运行、知识库、检索日志、模型版本）
CREATE DATABASE IF NOT EXISTS `caries_biz`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS `caries_ai`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
