# Architecture

## 1. 当前架构定位

当前后端应被定义为“多模块单体”，不是微服务系统。

原因不是抽象偏好，而是代码现实：

- 各业务域已经分模块
- 但患者、病例、影像、分析、报告、随访之间仍强耦合
- 一个可部署单元更适合当前测试、答辩和交付阶段

## 2. 模块划分

### 2.1 基础模块

- `caries-common`
- `caries-framework`
- `caries-boot`

### 2.2 业务模块

- `caries-system`
- `caries-patient`
- `caries-image`
- `caries-analysis`
- `caries-report`
- `caries-followup`
- `caries-dashboard`
- `caries-integration`

## 3. 分层结构

大部分业务模块采用：

- `controller`
- `app`
- `domain`
- `infrastructure`
- `interfaces`

这说明当前仓库已经不是简单 CRUD 工程，而是有明确应用层、领域层和基础设施层分工。

## 4. 请求处理链路

典型请求路径：

1. 请求进入 Spring MVC
2. `JwtAuthenticationFilter` 解析 Token
3. `SecurityContext` 装载当前用户
4. `@RequirePermission` 做权限校验
5. Controller 调用 AppService
6. AppService 驱动领域规则与仓储
7. 数据落到 MySQL
8. 统一返回 `ApiResponse`

## 5. 数据架构

当前持久层组合：

- MyBatis-Plus：DO/Mapper/Repository
- Flyway：schema 迁移
- MySQL：事务型主库
- JdbcTemplate：dashboard 聚合 SQL

当前通用设计：

- `org_id` 机构隔离
- `deleted_flag` 逻辑删除
- `created_* / updated_*` 审计字段
- 状态码 + 状态日志并存

## 6. 外部依赖架构

当前外部依赖的真实状态：

- MySQL：已实际使用
- RabbitMQ：analysis 事件可真实发布
- Redis：配置存在但尚未成为业务核心依赖
- 对象存储：抽象已存在，当前实现为本地文件系统
- Python AI：通过消息/回调契约协同，不在仓库内

## 7. 当前核心业务链路

目前架构真正支撑的主链路是：

- auth -> patient -> visit -> case -> image -> analysis -> report -> followup -> dashboard

这条链路既体现在控制器和服务里，也体现在 boot 真库测试里。

## 8. 当前架构缺口

- 权限菜单种子数据不足，导致普通角色演示不完整
- 数据权限规则没有横向下沉到所有模块
- dashboard 直接查业务表，未来需要缓存/快照/宽表
- PDF 生成与对象存储还未产品化

## 9. 结论

当前最准确的架构描述是：

- 一个以 MySQL 为核心的多模块单体后端
- 已具备可演示的业务闭环
- 仍保留若干外部集成和工程化收尾点
