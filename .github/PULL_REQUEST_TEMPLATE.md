## Description

<!-- Describe the purpose of this PR and what changes were made. -->

## Type of Change

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Refactoring (no functional changes)
- [ ] Documentation update
- [ ] Database schema change

---

## Database Schema Checklist

<!-- Complete this section if your PR involves any database schema changes. -->
<!-- If no schema changes are involved, delete this section or mark N/A. -->

- [ ] 是否涉及数据库 schema？若是，已生成 Alembic 迁移文件
- [ ] 迁移文件命名符合 `NNNN_动词_对象_细节.py` 规范
- [ ] 已在本地执行 `alembic upgrade head` 验证迁移无报错
- [ ] `downgrade()` 可执行（已在 dev 环境回归验证）
- [ ] ORM 模型 (`app/models/*.py`) 与迁移内容保持一致
- [ ] 基线文档 `Documents/02_数据库设计.md` 已同步更新
- [ ] 破坏性变更（删表/删列/改类型）已在本 PR 描述中标注

---

## Testing

- [ ] 本地单元测试通过 (`pytest tests/ -v`)
- [ ] Docker 环境启动正常 (`docker compose up backend-python`)
- [ ] 相关 E2E 场景已验证（如适用）

## Additional Notes

<!-- Any additional information, screenshots, or context. -->
