# Smart Travel and Expense Hub — Web (Frontend + Backend)

Smart Travel & Expense Hub的Web端服务，前后端都在其中：

```
web-frontend/   React + Vite 前端
web-backend/    Spring Boot 后端
gateway/        统一对外访问的 Nginx 网关配置
docker-compose.yml   本地/服务器整体编排（MySQL + 后端 + 前端 + 网关）
.github/workflows/
  ci-frontend.yml   前端 CI/CD
  ci-backend.yml    后端 CI/CD
```

### 项目状态概览（截至 2026-08-16）

- 全栈基础设施整合已完成：React 前端 + Spring Boot 后端 + MySQL + Nginx API Gateway + Docker Compose 编排。
- GitHub Actions 的前后端 CI/CD 流水线已上线，并能在 `main` 分支上自动执行构建、测试、安全扫描和部署。
- 云端部署已验证可用：已在 DigitalOcean Ubuntu Droplet 上完成部署验证，应用栈可正常运行。
- 业务功能层面仍处于迭代中：截至当前，尚无单个业务功能达到“集成测试通过 / 已部署到云端”的正式交付状态；当前仓库主要体现的是基础骨架和全栈集成基础设施。

### Project Status Report

| ID | Features | Implementation Technologies | Requirements | Development | Integration Testing | Deployed to Cloud |
|---|---|---|---|---|---|---|
| 1 | Secure Account Login | Spring Boot, Spring Security, JWT, RBAC, MySQL, React.js (login form) | I | I |  |  |
| 2 | Admin Account Management (create/edit/disable) | React.js, Spring Boot, MySQL | I | I |  |  |
| 3 | Forgotten Password Reset via Email | Spring Boot, Email service (SMTP/SES), JWT reset token |  |  |  |  |
| 15 | Budget vs. Actual Spending Dashboard | React.js, Chart.js, Spring Boot REST API | I |  |  |  |
| 16 | Department Budget Configuration | React.js, Spring Boot, MySQL | I |  |  |  |
| 17 | Reimbursement Review with Policy Flags | React.js, Spring Boot, rule engine | I |  |  |  |
| 18 | Export Expense Data to Excel | React.js, Apache POI | I |  |  |  |
| 19 | Edit/Correct Submitted Claims | React.js, Spring Boot REST API | I |  |  |  |
| 20 | Manager Approval Notifications | Spring Boot, Email/Push (SES/FCM), React.js, MySQL | I |  |  |  |
| 23 | Departmental Expense Comparison Chart | React.js, Chart.js | I |  |  |  |
| 24 | Employee Travel Frequency Statistics | React.js, Chart.js | I |  |  |  |
| 25 | Budget Overspending Alert Dashboard | React.js, Chart.js, anomaly detection rules | I |  |  |  |
| 26 | Data Encryption at Rest and in Transit | AES-256, TLS 1.2+/HTTPS, Spring Boot field-level encryption |  |  |  |  |
| 27 | RBAC and Sensitive-Data Access Audit Log | Spring Security RBAC, audit log table, MySQL | C | I |  |  |
| 28 | CI/CD Pipeline with Security Scanning | GitHub Actions, Docker, SonarQube / Snyk | C | C | C | C |
| 29 | Unified Cloud Gateway Infrastructure | DigitalOcean Ubuntu Droplet (backlog spec: AWS EC2/RDS), Docker Compose, Nginx (API Gateway) | C | C | C | C |

说明：`C = Completed`，`I = In Progress`。当前项目已完成基础设施层和部署层的集成与上线；后续业务功能项仍需继续按需求、设计、开发和集成测试节奏推进。

### 整体架构

```
                        ┌─────────────┐
   外部请求  ───────▶   │   gateway    │  (Nginx, 对外唯一入口, :80)
                        └──────┬──────┘
                    /api/*     │      其余路径
                 ┌─────────────┴─────────────┐
                 ▼                           ▼
         ┌───────────────┐           ┌───────────────┐
         │  web-backend  │           │  web-frontend │
         │ Spring Boot   │           │ React 静态资源 │
         │   :8081       │           │  (自带nginx:80)│
         └───────┬───────┘           └───────────────┘
                 │
                 ▼
         ┌───────────────┐
         │     mysql     │
         └───────────────┘
```

前端、后端、MySQL 都不直接对外暴露端口，只有 `gateway` 监听 80。这对应 backlog Item 29 里
"Nginx 作为统一 API 网关"的要求，也是为什么两个 CI 流水线各自构建镜像、但只有 gateway 层
需要在服务器上开放端口。

### 本地运行

```bash
cp .env.example .env   # 按需修改 DB 密码、JWT_SECRET
docker compose up --build
```

- 浏览器打开 `http://localhost` → 走 gateway → 前端
- `http://localhost/api/health` → 走 gateway → 后端健康检查

### 两条独立的 CI/CD 流水线

前后端各自一条流水线，按各自目录或 `docker-compose.yml` 的变更触发。部署 job 会串行执行，
避免两条流水线同时在同一台服务器执行 `git pull` 和 Docker Compose：

| | 前端 `ci-frontend.yml` | 后端 `ci-backend.yml` |
|---|---|---|
| build-and-test | npm ci → lint → format → vitest → vite build | mvn test（H2） → mvn package |
| security-scan | SonarQube（JS/TS）+ Snyk（npm 依赖） | SonarQube（Java，含 JaCoCo 覆盖率）+ Snyk（Maven 依赖） |
| docker-build | 验证 `expense-hub-web` Docker 镜像可构建 | 验证 `expense-hub-backend` Docker 镜像可构建 |
| deploy | SSH 到服务器，拉取代码后执行 `docker compose up -d --build web-frontend` | SSH 到服务器，拉取代码后执行 `docker compose up -d --build web-backend` |

此项目采用“服务器本地构建”部署方式，不推送镜像到 Docker Registry，因此**不需要**
`REGISTRY_URL`、`REGISTRY_USERNAME` 或 `REGISTRY_PASSWORD`。服务器上的 `/opt/expense-hub`
必须是仓库的 `main` 分支工作副本且具备 `git pull` 权限；生产环境变量应在服务器本地的 `.env`
中配置。GitHub 只需配置部署用的 `DEPLOY_HOST`、`DEPLOY_USER` 和 `DEPLOY_SSH_KEY`（以及启用
Snyk 扫描时的 `SNYK_TOKEN`）。

两条流水线都遵循 backlog Item 28 的验收标准：**任何一步失败（构建/测试失败，或 SonarQube/Snyk
发现 high/critical 级别问题）都会让对应 job 失败**。要让这个红叉真正拦住合并，需要在 GitHub
仓库 Settings → Branches 的分支保护规则里，把两条流水线的 `build-and-test` 和 `security-scan`
都设为 required status checks——这一步必须手动配置一次，workflow 文件本身做不到。

### 关于 SonarQube（暂时跳过）

两条流水线的 `security-scan` job 里，SonarQube 步骤加了 `if: ${{ secrets.SONAR_TOKEN != '' && secrets.SONAR_HOST_URL != '' }}` 判断：只要 `SONAR_TOKEN`/`SONAR_HOST_URL` 这两个 Secret 没配，这两步会被跳过（显示灰色 skipped，不是失败），job 里的 Snyk 扫描仍然正常跑，不影响构建镜像和部署。等以后 SonarQube 服务器就绪、把这两个 Secret 填进仓库设置后，扫描会自动开始生效，**不需要再改 workflow 文件**。



## WEB Backend

Spring Boot 后端骨架，对应 backlog 中 User Management（Item 1-3）+ Web Application（Item 15-20）
两个 epic 的技术栈：Spring Boot + Spring Security + JWT + RBAC + MySQL。

### 已经搭好的部分

- **鉴权骨架**：`POST /api/auth/login`，JWT 签发/校验，5 次失败锁定账号（对应 Item 1 验收标准）
- **RBAC**：`SecurityConfig` 里按路径前缀区分 `ADMIN` / `FINANCE_STAFF` / `MANAGER` 角色（对应 Item 27）
- **管理员账号管理骨架**：`GET/PATCH /api/admin/users/**`（对应 Item 2，字段和校验待补充）
- **健康检查**：`GET /api/health`，供 Docker healthcheck 和网关探测使用
- **统一异常处理**：`GlobalExceptionHandler` 返回结构化 JSON 错误体
- **测试**：用 H2 内存数据库跑 `test` profile，不依赖真实 MySQL 也能在 CI 里跑单元/集成测试

### 本地开发

```bash
# 需要本地起一个 MySQL，或者直接用根目录 docker-compose 里的 mysql 服务
export DB_HOST=localhost DB_PORT=3306 DB_NAME=expense_hub DB_USERNAME=root DB_PASSWORD=xxx
export JWT_SECRET=any-random-string-at-least-32-characters-long
mvn spring-boot:run
```

提交前本地跑一遍和 CI 一致的检查：

```bash
mvn test              # 单元/集成测试（H2，不需要真实数据库）
mvn package            # 打包，确认无编译错误
```

VS Code 里装 **Extension Pack for Java** + **Spring Boot Extension Pack** 就能获得和 IntelliJ 类似的
调试、断点、Bean 依赖图等能力。

### 目录结构

```
src/main/java/com/expensehub/webbackend/
  config/       Spring Security、CORS 等配置
  security/     JwtUtil、JwtAuthenticationFilter
  controller/   REST 接口
  service/      业务逻辑接口与实现
  entity/       JPA 实体
  repository/   Spring Data JPA repository
  dto/          请求/响应 DTO
  exception/    自定义异常 + 全局异常处理
```



## Web Frontend

Web 端前端框架骨架（React + Vite）。当前已完成功能模块占位页面、基础导航与 API 文件骨架，尚未接入真实业务逻辑。

### 已经搭好的前端骨架

- **基础布局与导航**：`src/components/layout/BasicLayout.jsx`
- **功能模块占位页面**：
  - `src/pages/AuthAccountCreationPage.jsx`
  - `src/pages/AdminAccountManagementPage.jsx`
  - `src/pages/FinanceReimbursementPage.jsx`
  - `src/pages/ManagerApprovalsPage.jsx`
  - `src/pages/AnalyticsOverviewPage.jsx`
- **API 占位文件**：
  - `src/api/authApi.js`
  - `src/api/adminAccountsApi.js`
  - `src/api/financeApi.js`
  - `src/api/managerApi.js`
  - `src/api/analyticsApi.js`

### 当前路由（占位）

| 路由 | 页面 |
|---|---|
| `/` | Dashboard |
| `/accounts/create` | 账号创建与角色权限 |
| `/admin/accounts` | 管理员账号管理 |
| `/finance/reimbursements` | 财务报销流程 |
| `/manager/approvals` | 经理审批中心 |
| `/analytics` | 数据分析与可视化 |

### 本地开发

```bash
npm install
npm run dev        # 启动开发服务器，默认代理 /api 到本地后端
```

在提交代码前，本地先跑一遍和 CI 完全一致的检查，避免 PR 被拦：

```bash
npm run lint            # ESLint
npm run format:check    # Prettier 格式检查
npm run test:coverage   # Vitest 单元测试 + 覆盖率
npm run build           # 生产构建，确认无报错
```

VS Code 打开本项目后，会提示安装 `.vscode/extensions.json` 里的推荐插件（ESLint、Prettier、
SonarLint、Docker、Vitest）。`.vscode/settings.json` 已配置保存时自动格式化 + 自动修复 lint 问题，
这样写代码时就能实时看到 CI 会报错的地方，而不是等 PR 跑完流水线才发现。

### CI/CD 流水线做了什么（仓库根目录 `.github/workflows/ci-frontend.yml`）

| Job                 | 触发条件                        | 作用                                                         |
| ------------------- | ------------------------------- | ------------------------------------------------------------ |
| `build-and-test`    | 相关文件的 PR + push to main    | 装依赖、lint、格式检查、单元测试+覆盖率、生产构建            |
| `security-scan`     | 依赖上一步                      | SonarQube 代码质量扫描 + Snyk 依赖漏洞扫描（high/critical 直接失败） |
| `docker-build`      | 仅 push to main，且前两步通过   | 验证 Docker 镜像能够构建                                     |
| `deploy`            | 仅 push to main，且镜像构建成功 | SSH 到服务器，拉取代码后运行 `docker compose up -d --build <service>` |

这对应 backlog 里 Item 28 的验收标准：**任何一步失败（构建失败、测试不过、或 Sonar/Snyk 发现
high/critical 漏洞）都会让对应 job 标红，从而阻止合并**——前提是需要在 GitHub 仓库设置里把
`build-and-test` 和 `security-scan` 设为该分支的 **required status checks**（Settings → Branches →
Branch protection rules），否则红叉只是提示，不会真正拦截合并。



### 目录结构

```
src/
  api/          axios 客户端 + 各模块 API 占位文件
  components/   基础布局组件（含导航）
  pages/        Dashboard / 各角色功能占位页面
  styles/       全局样式
tests/          Vitest 单元测试
```

### Docker

```bash
docker build -t expense-hub-web .
docker run -p 8081:80 expense-hub-web
```

生产镜像是多阶段构建：`node:20-alpine` 编译静态资源，最终只打包进 `nginx:1.27-alpine`，体积小、
攻击面也小（这点 Snyk 扫描镜像时也会更容易过）。`nginx.conf` 里已处理好 React Router 的前端路由回退。
