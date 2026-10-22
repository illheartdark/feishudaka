# Skill 功能归纳
**功能：** Skill 功能归纳
**编号：** 2

> 可用技能来源：OpenCode 技能库（`C:\Users\King\.config\opencode\skills`，共 39 个）。其中 14 个与 superpowers 同名重叠，按用户指示**不调用、不记录**；以下记录其余 **25 个 DSH 环境可加载且可用的技能**。

## 一、需求与设计（Define 阶段）

- **interview-me** — 逐个提问式访谈，挖掘用户真实意图（而非表面要求）；适用于需求描述模糊（"build me X"缺对象/动机）、用户主动要求"面试我/拷问我"，或在任何计划、规格、代码产生前发现自己正在脑补需求时。
- **idea-refine** — 结构化发散/收敛思维，把模糊想法打磨成清晰可执行的概念；适用于想法还不明确、下计划前想检验假设、或想先扩展选项再收敛时。
- **spec-driven-development** — 编码前先写结构化规格说明书（定义做什么、为什么、如何验收）；适用于新项目/新功能/重大变更且尚无规格、或需求含糊不清时。
- **api-and-interface-design** — 设计稳定、难以误用的 API 与接口；适用于 REST/GraphQL 端点、模块间类型契约、前后端边界、组件 props 等一切代码间交互面。
- **frontend-ui-engineering** — 构建生产级用户界面（可访问、高性能、视觉精良、遵循设计系统、无"AI 味"）；适用于创建/修改任何面向用户的界面、组件、布局、状态管理。
- **context-engineering** — 优化 agent 上下文配置（给 agent 恰好的信息量）；适用于新会话启动、输出质量下降、任务切换、或需要配置项目规则文件与上下文时。

## 二、计划与执行

- **planning-and-task-breakdown** — 把工作拆解为有序、可验证的小任务并给出验收标准；适用于已有规格需拆分任务、任务过大无从下手、需要估算范围或可并行时。
- **incremental-implementation** — 薄切片增量实施（实现一块→测试→验证→再扩展），每步保持系统可工作可测试；适用于任何涉及多文件的变更、即将一次写大量代码、或任务大得无法一步落地时。
- **source-driven-development** — 每个实现决策都以官方文档为准（不凭记忆写框架代码，验证并引用来源）；适用于使用任何框架/库且正确性重要时。
- **doubt-driven-development** — 对每个非平凡决策做"新鲜上下文"的对抗性复核（偏向证伪而非认可）；适用于正确性优先于速度、陌生代码、高风险场景（生产/安全/不可逆操作）、或自信输出现在验证比以后调试更便宜时。
- **git-workflow-and-versioning** — 规范的 git 工作流（提交即存档点、分支即沙盒、历史即文档）；适用于任何代码变更的提交、分支、冲突解决、多并行流组织。

## 三、质量与安全

- **code-review-and-quality** — 多维代码审查（正确性、可读性、架构、安全、性能五轴），合并前必经；适用于审查自己/其他 agent/人类写的代码，标准为"整体改善即通过"。
- **code-simplification** — 在保持行为完全不变的前提下简化代码、降低复杂度；适用于代码可用但比应然的更难读、难维护、难扩展，或审查时发现冗余复杂度。
- **debugging-and-error-recovery** — 系统化根因调试与结构化分诊（停止加功能、保留证据、按流程找根因）；适用于测试失败、构建崩溃、行为不符预期、任何意外错误，或生产事故。
- **security-and-hardening** — 安全优先开发（一切外部输入视为敌对、密钥视为神圣、鉴权必做）；适用于处理用户输入、认证、数据存储、外部集成，或任何接收不可信数据的功能。
- **performance-optimization** — 先测量后优化（profile 定位真实瓶颈→修复→再测量）；适用于存在性能要求、怀疑性能回退、Core Web Vitals/加载时间需改善、或 profile 已发现瓶颈时。
- **browser-testing-with-devtools** — 用 Chrome DevTools MCP 在真实浏览器中测试（检查 DOM、捕获控制台错误、分析网络请求、profile 性能、验证视觉输出）；适用于构建/调试任何浏览器中运行的东西；**需配置 chrome-devtools MCP server**。

## 四、交付与运维

- **ci-cd-and-automation** — 自动化 CI/CD 流水线（质量门禁、CI 测试运行器、部署策略，左移尽早拦截问题）；适用于搭建/修改构建与部署流水线。
- **shipping-and-launch** — 生产发布准备（预发布清单、监控、分阶段发布、回滚方案，可逆/可观测/增量）；适用于首次部署到生产、需要发布检查表或监控设置时。
- **deprecation-and-migration** — 弃用与迁移管理（移除不再有价值的代码、安全迁移用户、决定维护还是日落）；适用于移除旧系统/API/功能、迁移用户、或评估存量代码去留。
- **documentation-and-adrs** — 记录决策与文档（ADR 架构决策记录、公共 API 变更、发布记录，记录"为什么"）；适用于做出架构决策、变更公共 API、发布功能、或需要为未来工程师/agent 留上下文时。

## 五、技能与元能力

- **skill-creator** — 创建、修改、优化技能并评估性能（从零编写、编辑、运行 eval 测试、方差分析、优化触发描述）；适用于用户要创建/编辑/优化技能时。
- **opencode-skill-creator** — 用 opencode-skill-creator 插件创建、测试、评估、优化、打包 OpenCode 技能；仅适用于用户明确提及 OpenCode 技能场景（非通用技能创建）。
- **find-skills** — 帮助发现并安装开源 agent 技能（用户问"怎么做 X""有没有做 X 的技能""能否扩展能力"时）；适用于用户寻找可能以可安装技能存在的功能时。
- **using-agent-skills** — 技能发现与调用的元技能（按开发阶段定位适用技能）；适用于会话开始或需要判断当前任务该用哪个技能时。

---

> **整理说明（已清除的记录）**：
> - 原「系统级 Skill」（`C:\Users\King\.codex\skills\.system`：imagegen、openai-docs、plugin-creator、review-agent、skill-creator、skill-installer）与「插件运行时 Skill」（`...\openai-primary-runtime`：documents、pdf、presentations、excel-live-control、spreadsheets、template-creator）位于 **Codex 环境目录**，DSH 技能发现机制（`skill-filesystem`）不扫描这些路径，当前环境不可调用 → 已清除记录。
> - 原「个人 Agent Skill」（`C:\Users\King\.agents\skills` 下 superpowers 14 个）按**用户指示不调用** → 已清除记录。
> - 所有被清除记录的技能**文件均未改动**，仍保留在原位置（其他 agent 工具照常使用）。
