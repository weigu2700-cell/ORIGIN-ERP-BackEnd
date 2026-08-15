# SmartERP — 智能制造轻量级 ERP 系统

SmartERP 是一个面向中小型制造企业的轻量级 ERP 个人项目，围绕制造企业中的销售、采购、库存、生产、BOM、组织权限等核心业务进行设计与实现。

项目采用 **Vue 3 + TypeScript + Spring Boot + MySQL** 前后端分离架构，目标不是单纯实现后台 CRUD，而是通过真实制造业业务场景，完成从销售订单、库存需求、物料采购、生产工单到成品入库与销售出库的完整业务闭环。

## 项目目标

SmartERP 主要用于模拟制造企业日常经营管理流程，并重点解决以下问题：

* 企业用户、部门、角色与权限管理
* 客户、供应商、产品、物料等基础资料管理
* 销售订单及订单状态流转
* 原材料与成品库存管理
* 库存预占、入库、出库及库存流水追踪
* BOM 多版本管理
* 生产计划与生产工单管理
* 采购订单及缺料采购
* 工厂、车间、生产线等制造资源管理
* 生产领料、完工入库及实际物料消耗记录

## 核心业务流程

系统围绕制造企业订单驱动生产的业务模式进行设计：

```text
客户销售订单
     ↓
检查成品库存
     ↓
库存不足
     ↓
生成生产需求
     ↓
BOM 物料展开
     ↓
检查原材料库存
     ↓
缺料生成采购需求
     ↓
采购入库
     ↓
生产领料
     ↓
生产工单执行
     ↓
成品入库
     ↓
销售出库
     ↓
订单完成
```

库存设计区分：

* 实际库存（On Hand）
* 预占库存（Reserved）
* 可用库存（Available）
* 库存流水（Inventory Transaction）

避免销售订单重复占用库存导致超卖，并保证库存变化具有可追溯性。

## 技术栈

### Backend

* Java
* Spring Boot
* Spring Security
* JWT
* MyBatis-Plus
* MySQL
* Bean Validation
* Maven

### Frontend

* Vue 3
* TypeScript
* Vite
* Pinia
* Vue Router
* Element Plus
* Axios

## 后端架构

项目当前采用模块化单体架构，根据业务领域划分模块：

```text
com.smart.erp

├── common          # 通用基础设施
├── system          # 用户、部门、角色、权限
├── master          # 客户、供应商、产品、物料等基础资料
├── sales           # 销售管理
├── purchase        # 采购管理
├── inventory       # 库存管理
└── production      # BOM、生产计划、生产工单
```

每个业务模块按照：

```text
Controller
    ↓
Service
    ↓
Mapper
    ↓
MySQL
```

进行分层。

同时通过 DTO / VO 与 Entity 分离，避免数据库模型直接暴露给前端。

## 已实现基础能力

项目目前正在持续开发，已搭建包括：

* Spring Boot 后端基础架构
* MySQL + MyBatis-Plus 持久层
* 统一 API 响应
* 全局异常处理
* 参数校验
* 逻辑删除
* 创建时间 / 更新时间自动填充
* Spring Security + JWT 登录认证
* 用户、部门等系统管理基础功能
* 用户、角色、权限 RBAC 模型设计

后续将逐步完成制造 ERP 核心业务模块。

## 项目特点

SmartERP 不以简单的 CRUD 页面数量作为主要目标，而是更加关注：

* 制造业真实业务关系
* 数据库表关系设计
* 业务状态流转
* 数据历史可追溯
* BOM 版本管理
* 库存一致性
* 数据事务
* 并发库存控制
* 模块之间的业务协作

例如，已经发布的 BOM 不直接覆盖修改，而通过 BOM Version 保存不同生产时期的标准数据；生产工单绑定实际使用的 BOM 版本，从而保证历史生产数据不会被后续 BOM 修改污染。

## 项目状态

> 🚧 Developing

SmartERP 当前仍处于持续开发阶段，将按照：

**系统管理 → 基础资料 → 库存 → 销售 / 采购 → BOM / 生产 → 数据看板与智能制造增强**

的顺序逐步完善。
