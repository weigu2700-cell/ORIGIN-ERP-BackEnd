# SmartERP — 智能制造轻量级 ERP 系统

SmartERP 是一个面向中小型制造企业的轻量级 ERP 个人项目。

项目采用 **Java 21 + Spring Boot 3.4.5 + MySQL + MyBatis-Plus** 构建后端，前端计划采用 **Vue 3 + TypeScript**。项目重点不是堆叠 CRUD 页面，而是围绕真实制造业业务逐步实现权限、主数据、库存、销售、采购、BOM、生产等模块，并在业务中实践事务、索引、乐观锁和库存并发控制。

> 🚧 项目持续开发中

## 项目目标

SmartERP 计划形成以下制造业务主链路：

```text
客户销售订单
     ↓
检查 / 预占成品库存
     ↓
库存不足产生生产需求
     ↓
BOM 物料展开
     ↓
检查原材料库存
     ↓
缺料采购
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

项目重点训练：

- ERP 业务建模
- 数据库设计与多表关系
- RBAC 权限体系
- 事务边界
- 索引设计
- 乐观锁
- 库存一致性与并发控制
- 前后端接口设计

## 技术栈

### Backend

- Java 21
- Spring Boot 3.4.5
- Spring Security
- JWT（JJWT 0.12.6）
- MyBatis-Plus 3.5.12
- MySQL
- Bean Validation
- Springdoc OpenAPI
- Maven

### Frontend（下一阶段）

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Axios
- Element Plus

## 项目架构

当前采用模块化单体架构：

```text
org.smart.erp
├── common       # 通用能力、异常、配置、工具
├── system       # 用户、部门、角色、菜单、权限
├── master       # 客户、供应商、工厂、仓库、物料等主数据
└── inventory    # 库存
```

后续逐步加入：

```text
sales
purchase
production
```

业务模块内部主要采用：

```text
Controller
    ↓
Service
    ↓
Mapper
    ↓
MySQL
```

并通过 Entity / DTO / VO / Enum 分离数据库模型、请求模型和响应模型。

## 已实现功能

### 1. 系统管理与 RBAC

已完成基础权限体系：

```text
User
 ↓
UserRole
 ↓
Role
 ├── RoleMenu → Menu
 └── RolePermission → Permission
```

主要能力：

- 用户登录
- JWT 身份认证
- BCrypt 密码加密
- Spring Security
- 用户 / 部门 / 角色管理
- 菜单管理
- 权限管理
- 用户角色关系
- 角色菜单关系
- 角色权限关系
- 当前用户菜单树
- 当前用户权限码
- `@PreAuthorize` 接口权限校验
- 统一 API 响应
- 全局异常处理

示例：

```java
@PreAuthorize("hasAuthority('master:material:create')")
```

## 2. 基础资料 Master Data

当前已完成：

- Customer 客户
- Supplier 供应商
- Factory 工厂
- Workshop 车间
- ProductionLine 生产线
- Warehouse 仓库
- Material 物料
- MaterialSupplier 物料供应商关系

### 制造资源层级

```text
Factory
├── Workshop
│   └── ProductionLine
└── Warehouse
```

工厂、车间、生产线和仓库使用业务状态控制。上游资源停用时可以进行级联停用，而不是依赖数据库物理级联删除。

### Material Supplier

物料和供应商为多对多关系：

```text
Material N : N Supplier
```

关系本身包含采购业务属性：

- 供应商物料编码
- 采购价格
- MOQ（最小采购数量）
- Lead Time（交货周期）
- 首选供应商
- 状态

首选供应商切换使用事务保证多条关系记录的一致性。

## 3. Inventory 库存

库存模块已进入核心业务开发阶段。

### 库存模型

```text
On Hand   = 实际在库库存
Reserved  = 已预占库存
Available = On Hand - Reserved
```

`Available` 不单独持久化，由业务层实时计算，避免冗余字段产生数据不一致。

库存记录使用以下维度唯一确定：

```text
warehouse_id + material_id
```

即同一种物料在不同仓库分别维护库存。

### 已实现库存动作

#### 库存预占

```java
reserveStock()
```

```text
On Hand = 100
Reserved = 20
Available = 80

预占 30

↓

On Hand = 100
Reserved = 50
Available = 50
```

#### 释放预占

```java
releaseStock()
```

订单取消等场景只减少 Reserved，不改变 On Hand。

#### 出库

```java
outboundStock()
```

对于已经预占的库存，正式出库时同时减少：

```text
On Hand
Reserved
```

#### 入库

```java
inboundStock()
```

入库只增加 On Hand，Reserved 保持不变。

## 库存并发控制

库存使用 MyBatis-Plus 乐观锁：

```java
@Version
private Integer version;
```

更新时通过版本号避免并发覆盖：

```sql
UPDATE inv_material_stock
SET reserved = ?,
    version = version + 1
WHERE id = ?
  AND version = ?;
```

并结合：

```text
Available 校验
+
Version 乐观锁
+
有限重试
```

处理多个业务请求同时竞争库存的问题，降低超卖和库存覆盖风险。

## 库存流水

库存流水 `InventoryTransaction` 正在建设中。

目标记录每一次库存变化：

- 仓库
- 物料
- 库存动作类型
- 来源业务类型
- 来源业务单号
- 变动数量
- 变动前库存
- 变动后库存
- 变动前预占
- 变动后预占
- 操作时间

库存表回答：

> 现在有多少库存？

库存流水回答：

> 为什么变成现在这个库存？

后续会保证：

```text
库存更新
+
库存流水
```

处于同一个事务中。

## 当前开发进度

```text
System / RBAC               ✅

Master Data
├── Customer                ✅
├── Supplier                ✅
├── Factory                 ✅
├── Workshop                ✅
├── ProductionLine          ✅
├── Warehouse               ✅
├── Material                ✅
└── MaterialSupplier        ✅

Inventory
├── MaterialStock           ✅
├── Reserve Stock           ✅
├── Release Stock           ✅
├── Outbound Stock          ✅
├── Inbound Stock           ✅
└── InventoryTransaction    🚧

Frontend                    ⏳
Sales                       ⏳
Purchase                    ⏳
BOM                         ⏳
Production                  ⏳
```

## 下一阶段

近期计划：

```text
InventoryTransaction
        ↓
库存第一阶段闭环
        ↓
Vue 3 前端基础架构
        ↓
登录 / JWT / RBAC / 动态菜单
        ↓
基础资料页面
        ↓
库存查询 / 库存流水页面
```

前端基础闭环后，将采用一个业务模块一个业务模块的方式继续推进：

```text
后端业务
   ↓
前端页面
   ↓
接口联调
   ↓
下一个业务模块
```

后续核心模块包括：

- Sales Order
- Purchase Order
- BOM
- Production Plan
- Production Order
- 生产领料
- 完工入库
- 销售出库

## 项目定位

SmartERP 是一个制造 ERP 业务学习与工程实践项目。

项目关注的不只是“能运行”，更关注业务规则为什么这样设计、数据库如何保持一致、并发情况下库存如何正确变化，以及多个模块之间如何形成完整业务闭环。
