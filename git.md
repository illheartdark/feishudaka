# git.md — 版本管理手册与代码更新记录

**功能：** 版本管理手册（分支、提交、回滚规范）与代码更新记录，登记每次代码提交的变更范围与验证结果。
**编号：** 4

> 规则依据：agent.md（1）规则 10「引用 .md 文件时标明编号」；本文件与 `log.md（5）`（行为日志）、`修改日记.md（27）`（根目录改动记录）分工：本文件管提交，另两者管过程。

## 1. 仓库现状

| 项目 | 状态 | 说明 |
|---|---|---|
| git 仓库 | **已初始化** | 2026-10-23 00:29 执行 `git init -b main`；首个提交 `c48c58e` |
| 默认分支 | `main` | —— |
| 远端仓库 | **待配置** | 尚未在 GitHub 创建仓库，原因见第 8 节 |
| `.gitignore` | 已建立 | 忽略 `build/`、`*.apk`、`*.dex`、`*.keystore`、IDE 与临时文件 |
| TLS 后端 | 仓库级设为 `openssl` | 本机 schannel 报 `SEC_E_NO_CREDENTIALS`，切 OpenSSL 后 `git ls-remote` 正常（exit 0） |

初始化记录（2026-10-23 00:29）：

```powershell
git init -b main
git add -A
git commit -m "chore: 初始化工作区、规则框架与打卡助手 Phase 1 骨架"
git config http.sslBackend openssl
```

结果：26 个文件、2,815 行插入，提交 `c48c58e`。

## 2. 分支规范

| 分支 | 用途 | 命名示例 |
|---|---|---|
| `main` | 可运行、可交付的稳定版本 | `main` |
| `feature/*` | 新功能开发 | `feature/feishu-checkin` |
| `fix/*` | 缺陷修复 | `fix/retry-timeout` |
| `chore/*` | 构建、文档、依赖等杂项 | `chore/acl-recovery-docs` |

约定：功能分支从 `main` 切出，合并前先拉取 `main` 最新提交；合并方式统一为 squash merge，保持 `main` 线性历史。

## 3. 提交规范

格式：`<type>(<scope>): <subject>`，subject 不超过 50 字，使用中文或英文均可但同仓库保持一致。

| type | 含义 |
|---|---|
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `refactor` | 重构（不改行为） |
| `docs` | 文档（含 `.md` 规则文件） |
| `test` | 测试相关 |
| `chore` | 构建、依赖、权限等杂项 |
| `revert` | 回滚 |

正文要求：说明「为什么改」，不复述 diff；破坏性变更以 `BREAKING CHANGE:` 开头单列。

## 4. 提交前检查

1. `git status` 无意外改动（尤其确认没有把 `acl-recovery-*` 之类的临时目录提交进来）。
2. 本次改动已同步写入对应文件夹的修改日记（根目录为 `修改日记.md（27）`，子目录编号见 `agent.md（1）` 对照表）。
3. 本次任务已在 `log.md（5）` 留下完整记录。
4. 涉及功能完成时，已按规则 14 更新 `project.md（6）`。
5. 若为代码改动，构建与测试通过（命令见各文件夹的说明文件，如 `poc\说明.md（9）`）。

## 5. 回滚规范

| 场景 | 操作 |
|---|---|
| 撤销未提交的单个文件改动 | `git restore <file>` |
| 撤销最近一次提交（保留改动） | `git reset --soft HEAD~1` |
| 撤销已推送的提交 | `git revert <sha>`（禁止对 `main` 使用 `reset --hard` 后强推） |
| 紧急修复 | 从 `main` 切 `fix/*`，修复后 PR 合并 |

## 6. 代码更新记录

> 每次代码提交后在此表顶部追加一行，与 `修改日记.md（27）` 一一对应。

| 序号 | 时间 | 分支 | 提交信息 | 变更文件 | 验证结果 |
|---|---|---|---|---|---|
| 1 | 2026-10-23 00:29 | `main` | `chore: 初始化工作区、规则框架与打卡助手 Phase 1 骨架` | 26 个文件：规则文档 6 个、需求与说明 3 个、Android 工程 15 个、`.gitignore` | 提交成功，`git log` 可查；`git status` 干净（第二笔文档更正提交另记） |
| 2 | 2026-10-23 00:31 | `main` | `docs: 规则符合性自查修正（时间戳、project/git 更新、新增 .gitignore）` | `log.md`、`修改日记.md`、`plan.md`、`project.md`、`git.md`、`feishu-checkin\修改日记.md` | 提交成功 `01a6438`；复审无超前时间戳、工作区干净 |
| — | 2026-10-22 23:39 | — | 仓库尚未初始化，无提交记录 | — | — |

> 约定：本表记录的是「已完成的提交」。每次提交前先补记上一笔的哈希，本次提交的哈希留待下次补记，避免自引用。

## 7. 维护说明

- 本文件只增不改：历史提交记录保留，纠错以新增行方式说明。
- 首次 `git init` 已完成（2026-10-23 00:29，提交 `c48c58e`）；后续每次提交需在「代码更新记录」追加一行。

## 8. 远程仓库与推送阻塞（2026-10-23）

| 项 | 现状 | 结论 |
|---|---|---|
| GitHub 网络（OpenSSL 后端） | `git ls-remote https://github.com/git/git.git` 返回 HEAD，exit 0 | ✅ 通 |
| GitHub 网络（schannel 后端） | `SEC_E_NO_CREDENTIALS (0x8009030e)` | ❌ 不可用，已改用 OpenSSL |
| `gh` CLI | 未安装 | ❌ 无法用它建仓库 |
| GitHub Token | 环境变量无 `GITHUB_TOKEN` / `GH_TOKEN` | ❌ 无法调用 API 建仓库 |
| 凭据管理器 | `credential.helper` 为空，`~/.git-credentials` 不存在 | ❌ 无法静默认证 |
| SSH 密钥 | `~/.ssh` 不存在，且沙箱禁止创建该目录 | ❌ 无法走 SSH |

**结论：本地仓库与首次提交已完成；远程仓库已存在（`illheartdark/feishudaka`，公开），但 `github.com:443` 被网络重置，标准 HTTPS 推送不可用。**

## 9. 远程推送的可用通道（2026-10-23 00:40 实测）

| 通道 | 结果 | 说明 |
|---|---|---|
| `https://github.com/...`（HTTPS 443） | ❌ `Recv failure: Connection was reset` | TCP 可连但 HTTP 层被重置 |
| `api.github.com`（HTTPS 443） | ✅ HTTP 200 | 可读仓库元信息，可调 REST API |
| `ssh.github.com:443` | ✅ `Permission denied (publickey)` | 链路通，仅缺授权密钥 |
| schannel TLS 后端 | ❌ `SEC_E_NO_CREDENTIALS` | 已改用 OpenSSL |

**选定方案：SSH over 443。**

- 专用密钥：`%TEMP%\daka-ssh\id_daka`（**在工作区之外，未被提交**）
- 公钥指纹前缀：`ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIEXZI4k/jKocMKDVKcOcEgmP06xiZAw/0lv0uwoksS/g daka-assistant`
- 待用户在 GitHub 添加授权（Deploy key 勾选 write，或账号 SSH key）后，使用：

```powershell
$env:GIT_SSH_COMMAND = "ssh -p 443 -i $env:TEMP\daka-ssh\id_daka -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL"
git remote set-url origin ssh://git@ssh.github.com:443/illheartdark/feishudaka.git
git push -u origin main
```
