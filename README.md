# 原点 ERP · ORIGIN 后端

面向制造企业的 ERP 后端服务，基于 Spring Boot 构建，覆盖系统权限、基础资料、销售、库存、BOM 与生产业务。项目采用前后端分离和模块化单体架构，通过 JWT、RBAC、事务与库存流水维护业务安全和数据一致性。

- 后端仓库：[ORIGIN-ERP-BackEnd](https://github.com/weigu2700-cell/ORIGIN-ERP-BackEnd)
- 前端仓库：[ORIGIN-ERP--FrontEnd](https://github.com/weigu2700-cell/ORIGIN-ERP--FrontEnd)
- 移动端仓库: [ORIGIN-ERP-Moblie](https://github.com/weigu2700-cell/ORIGIN-ERP-Moblie.git)

## 当前功能

### 系统管理

- 用户登录、JWT 身份认证和 Spring Security 接口保护
- 用户分页查询、新增、编辑、真实姓名维护和状态修改
- 角色、部门、菜单和权限管理
- 用户分配角色、部门，角色分配菜单和操作权限
- 当前用户菜单树与前端动态路由
- 当前用户权限范围内的菜单模糊搜索
- 超级管理员自动获取全部菜单

权限关系：

~~~text
User ── UserRole ── Role ── RoleMenu ── Menu
                       └──── RolePermission ── Permission
~~~

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

~~~text
available = onHand - reserved
~~~

### BOM 与生产

- BOM 创建、详情、分页查询、启用和禁用
- 多级 BOM 树形展开
- 按需求数量计算物料需求
- 生产需求分页查询和详情
- 生产订单创建、分页查询和详情
- 生产订单下达、开工、完工和取消
- 下达生产订单时计算 BOM 净需求

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 运行环境 | Java 21 |
| 核心框架 | Spring Boot 3.4.5 |
| Web 与安全 | Spring Web、Spring Security、JWT |
| 数据访问 | MyBatis-Plus 3.5.12 |
| 数据库 | MySQL |
| 缓存与序号 | Spring Data Redis |
| 接口文档 | Springdoc OpenAPI |
| Excel | FastExcel |
| 构建工具 | Maven Wrapper |

## 项目结构

~~~text
src/main/java/org/smart/erp
├── common/       # 返回体、异常、安全、配置、序号和通用工具
├── system/       # 用户、角色、部门、菜单、权限和登录
├── master/       # 客户、供应商、工厂、车间、产线、仓库和物料
├── inventory/    # 物料库存与库存流水
├── sales/        # 销售订单与销售出库
├── production/   # BOM、生产需求和生产订单
└── purchase/     # 采购模块目录

sql/smart-erp/
├── sys_*.sql     # 系统管理表
├── md_*.sql      # 基础资料表
├── inv_*.sql     # 库存表
├── sal_*.sql     # 销售表
├── migration_*.sql
└── mock_data.sql # 本地演示数据
~~~

各业务模块内部按 controller、dto、entity、mapper、service、vo 分层。

## 环境要求

- JDK 21
- MySQL 8.x
- Redis
- Git

项目包含 Maven Wrapper，无需单独安装 Maven。

## 数据库准备

默认数据库名为 smart-erp。数据库连接和 Redis 地址在 src/main/resources/application.yaml。

首次使用时：

1. 创建 smart-erp 数据库。
2. 按依赖关系执行 sql/smart-erp 下的建表脚本。
3. 如需演示数据，执行 mock_data.sql。
4. 已有数据库按文件日期执行尚未应用的 migration_*.sql。

最近的迁移包括用户真实姓名、销售订单明细备注和销售订单明细创建时间兼容补丁。部署前请使用环境变量或外部配置覆盖数据库密码等敏感配置。

## 启动项目

~~~bash
git clone https://github.com/weigu2700-cell/ORIGIN-ERP-BackEnd.git
cd ORIGIN-ERP-BackEnd

./mvnw clean compile
./mvnw spring-boot:run
~~~

服务默认监听 http://localhost:8080，Swagger UI 地址为 http://localhost:8080/swagger-ui.html。

如果 8080 端口被占用，可先检查旧进程：

~~~bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
~~~

## 认证方式

登录接口：

~~~http
POST /auth/login
~~~

除登录、错误页和 Swagger 外，其余接口默认需要 Bearer Token：

~~~http
Authorization: Bearer <token>
~~~

统一响应结构：

~~~json
{
  "code": 200,
  "msg": "请求成功",
  "data": {}
}
~~~

## 常用接口

| 模块 | 接口 | 说明 |
| --- | --- | --- |
| 用户 | GET /system/user/list | 用户分页及姓名、状态筛选 |
| 用户 | PUT /system/user/{id}/status | 修改正常、锁定、注销状态 |
| 菜单 | GET /system/menu/current | 当前用户菜单树 |
| 菜单 | GET /system/menu/search?keyword=... | 权限范围内模糊搜索菜单 |
| 库存 | GET /inventory/material-stock | 物料库存分页 |
| 流水 | GET /inventory/transaction | 库存流水分页 |
| BOM | GET /prd/bom/{id}/explosion | 多级 BOM 展开 |
| BOM | GET /prd/bom/{id}/requirement | 物料需求计算 |
| 生产 | PUT /production/order/{id}/release | 下达并计算净需求 |

完整接口及参数以 Swagger 为准。

## 构建与验证

~~~bash
# 编译
./mvnw clean compile

# 测试
./mvnw test

# 打包
./mvnw clean package
~~~

## 开发约定

- 实体主键使用 Long，前端按字符串接收雪花 ID，避免 JavaScript 精度丢失。
- 状态字段使用枚举，并通过 MyBatis-Plus EnumValue 映射数据库值。
- 数据修改放在业务服务中，跨表操作使用事务保证一致性。
- 菜单与操作权限分开管理；菜单决定页面入口，Permission 决定操作权限。
- 新增数据库字段时同步更新基础建表脚本和日期迁移脚本。
- 新增接口后同步维护 Swagger 注解和前端类型。
