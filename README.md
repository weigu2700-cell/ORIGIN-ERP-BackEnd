# 原点 ERP · ORIGIN 后端

> 面向制造企业的 ERP 后端服务，基于 Spring Boot 构建，覆盖系统权限、基础资料、销售、库存、BOM、生产及采购业务。

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.12-000000)](https://baomidou.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Swagger](https://img.shields.io/badge/API-Swagger%20UI-brightgreen)](http://localhost:8080/swagger-ui.html)

- 后端仓库：[ORIGIN-ERP-BackEnd](https://github.com/weigu2700-cell/ORIGIN-ERP-BackEnd)
- 前端仓库：[ORIGIN-ERP--FrontEnd](https://github.com/weigu2700-cell/ORIGIN-ERP--FrontEnd)
- 移动端仓库：[ORIGIN-ERP-Moblie](https://github.com/weigu2700-cell/ORIGIN-ERP-Moblie.git)

---

## 📚 目录

- [项目简介](#项目简介)
- [✨ 功能特性](#功能特性)
- [🛠 技术栈](#技术栈)
- [📁 项目结构](#项目结构)
- [🚀 快速开始](#快速开始)
  - [环境要求](#环境要求)
  - [数据库准备](#数据库准备)
  - [启动服务](#启动服务)
- [🔐 认证方式](#认证方式)
- [📡 常用接口](#常用接口)
- [🧪 构建与验证](#构建与验证)
- [💻 开发约定](#开发约定)
- [🤝 贡献指南](#贡献指南)
- [📄 许可证](#许可证)
- [📮 联系方式](#联系方式)

---

## 项目简介

原点 ERP 是一套面向中小型制造企业的开源 ERP 系统，采用前后端分离、模块化单体（Modular Monolith）架构。后端基于 Spring Boot 3 + MyBatis-Plus 构建，通过 JWT、RBAC、数据库事务与库存流水保证业务安全与数据一致性；前端与移动端共用同一套账号、权限与业务数据。

核心设计目标：

- **权限可配置**：菜单决定页面入口，Permission 决定接口操作权限，支持动态路由。
- **库存可追溯**：所有出入库均产生流水，可用量 = 在库量 − 预留量。
- **业务可联动**：销售、采购、生产围绕 BOM 与库存形成闭环。

## ✨ 功能特性

### 系统管理

- 用户登录、JWT 身份认证和 Spring Security 接口保护
- 用户分页查询、新增、编辑、真实姓名维护和状态修改
- 角色、部门、菜单和权限管理
- 用户分配角色、部门，角色分配菜单和操作权限
- 当前用户菜单树与前端动态路由
- 当前用户权限范围内的菜单模糊搜索
- 超级管理员自动获取全部菜单

权限关系：

```text
User ── UserRole ── Role ── RoleMenu ── Menu
                       └──── RolePermission ── Permission
```

### 基础资料

- 客户、供应商
- 工厂、车间、生产线
- 仓库、物料
- 物料与供应商关系、优选供应商
- 基础资料分页查询、详情、维护和状态管理

### 销售管理

- 销售订单及明细的创建、修改、查询和删除
- 销售订单确认、取消及库存预留联动
- 销售出库单及明细
- 出库单确认、完成和取消
- Redis 生成业务单号

### 库存管理

- 物料库存分页查询和详情
- 在库量、预留量和可用量
- 库存预留、释放及出入库联动
- 库存流水分页查询及 Excel 导入、导出
- MyBatis-Plus 乐观锁与事务控制

```text
available = onHand - reserved
```

### BOM 与生产

- BOM 创建、详情、分页查询、启用和禁用
- 多级 BOM 树形展开
- 按需求数量计算物料需求
- 生产需求分页查询和详情
- 生产订单创建、分页查询和详情
- 生产订单下达、开工、完工和取消
- 下达生产订单时计算 BOM 净需求

### 采购管理

- 采购需求（审批）驱动采购订单
- 采购订单草稿 → 补全 → 审批流转
- 审批通过后自动生成采购入库单（草稿）
- 入库单审核（DRAFT → APPROVED）
- 移动端上架（APPROVED → UPLOADED）并增加库存
- 移动端审核 / 上架接口标注 `(移动端接口)`

## 🛠 技术栈

| 分类 | 技术 |
| --- | --- |
| 运行环境 | Java 21 |
| 核心框架 | Spring Boot 3.4.5 |
| Web 与安全 | Spring Web、Spring Security、JWT |
| 数据访问 | MyBatis-Plus 3.5.12 |
| 数据库 | MySQL 8.x |
| 缓存与序号 | Spring Data Redis |
| 接口文档 | Springdoc OpenAPI |
| Excel | FastExcel |
| 构建工具 | Maven Wrapper |

## 📁 项目结构

```text
src/main/java/org/smart/erp
├── common/       # 返回体、异常、安全、配置、序号和通用工具
├── system/       # 用户、角色、部门、菜单、权限和登录
├── master/       # 客户、供应商、工厂、车间、产线、仓库和物料
├── inventory/    # 物料库存与库存流水
├── sales/        # 销售订单与销售出库
├── production/   # BOM、生产需求和生产订单
└── purchase/     # 采购需求、采购订单、采购入库

sql/smart-erp/
├── sys_*.sql     # 系统管理表
├── md_*.sql      # 基础资料表
├── inv_*.sql     # 库存表
├── sal_*.sql     # 销售表
├── prd_*.sql     # 生产表
├── pur_*.sql     # 采购表
├── migration_*.sql
└── mock_data.sql # 本地演示数据
```

各业务模块内部按 `controller`、`dto`、`entity`、`mapper`、`service`、`vo` 分层。

## 🚀 快速开始

### 环境要求

- JDK 21
- MySQL 8.x
- Redis
- Git

项目包含 Maven Wrapper，无需单独安装 Maven。

### 数据库准备

默认数据库名为 `smart-erp`。数据库连接和 Redis 地址在 `src/main/resources/application.yaml`。

首次使用时：

1. 创建 `smart-erp` 数据库。
2. 按依赖关系执行 `sql/smart-erp` 下的建表脚本。
3. 如需演示数据，执行 `mock_data.sql`。
4. 已有数据库按文件日期执行尚未应用的 `migration_*.sql`。

> ⚠️ 部署前请使用环境变量或外部配置覆盖数据库密码等敏感配置。

### 启动服务

```bash
git clone https://github.com/weigu2700-cell/ORIGIN-ERP-BackEnd.git
cd ORIGIN-ERP-BackEnd

./mvnw clean compile
./mvnw spring-boot:run
```

服务默认监听 <http://localhost:8080>，Swagger UI 地址为 <http://localhost:8080/swagger-ui.html>。

如果 8080 端口被占用，可先检查旧进程：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

## 🔐 认证方式

登录接口：

```http
POST /auth/login
```

除登录、错误页和 Swagger 外，其余接口默认需要 Bearer Token：

```http
Authorization: Bearer <token>
```

统一响应结构：

```json
{
  "code": 200,
  "msg": "请求成功",
  "data": {}
}
```

## 📡 常用接口

| 模块 | 接口 | 说明 |
| --- | --- | --- |
| 用户 | `GET /system/user/list` | 用户分页及姓名、状态筛选 |
| 用户 | `PUT /system/user/{id}/status` | 修改正常、锁定、注销状态 |
| 菜单 | `GET /system/menu/current` | 当前用户菜单树 |
| 菜单 | `GET /system/menu/search?keyword=...` | 权限范围内模糊搜索菜单 |
| 库存 | `GET /inventory/material-stock` | 物料库存分页 |
| 流水 | `GET /inventory/transaction` | 库存流水分页 |
| BOM | `GET /prd/bom/{id}/explosion` | 多级 BOM 展开 |
| BOM | `GET /prd/bom/{id}/requirement` | 物料需求计算 |
| 生产 | `PUT /production/order/{id}/release` | 下达并计算净需求 |
| 采购 | `PUT /purchase/in/stock/{id}/approve` | 审核采购入库单（移动端接口） |
| 采购 | `PUT /purchase/in/stock/{id}/upload` | 上架采购入库单（移动端接口） |

完整接口及参数以 Swagger 为准。

## 🧪 构建与验证

```bash
# 编译
./mvnw clean compile

# 测试
./mvnw test

# 打包
./mvnw clean package
```

## 💻 开发约定

- 实体主键使用 `Long`，前端按字符串接收雪花 ID，避免 JavaScript 精度丢失。
- 状态字段使用枚举，并通过 MyBatis-Plus `EnumValue` 映射数据库值。
- 数据修改放在业务服务中，跨表操作使用事务保证一致性。
- 菜单与操作权限分开管理；菜单决定页面入口，Permission 决定操作权限。
- 新增数据库字段时同步更新基础建表脚本和日期迁移脚本。
- 新增接口后同步维护 Swagger 注解和前端类型。
- 移动端专用接口（审核、上架等）在 `@Operation` 摘要中标注 `(移动端接口)`。

## 🤝 贡献指南

1. Fork 本仓库并创建特性分支：`git checkout -b feature/your-feature`
2. 提交变更：`git commit -m "feat: 描述你的改动"`
3. 推送到分支：`git push origin feature/your-feature`
4. 提交 Pull Request，描述改动目的与影响范围。

提交前请确保：

- `./mvnw clean compile` 通过
- 新增接口补充 Swagger 注解与 SQL 迁移脚本
- 涉及库存、订单等核心链路时补充事务与流水

## 📄 许可证

本项目基于 [MIT 许可证](LICENSE) 开源。

## 📮 联系方式

- 项目主页：<https://github.com/weigu2700-cell/ORIGIN-ERP-BackEnd>
- 问题反馈：<https://github.com/weigu2700-cell/ORIGIN-ERP-BackEnd/issues>

---

⭐ 如果这个项目对你有帮助，欢迎 Star 支持！
