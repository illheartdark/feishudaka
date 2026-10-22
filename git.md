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
- 待用户在 GitHub 添加授权（Deploy key 勾选 write，或账号 SSH key）后，使用：```powershell
$env:GIT_SSH_COMMAND = "ssh -p 443 -i $env:TEMP\daka-ssh\id_daka -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL"
git remote set-url origin ssh://git@ssh.github.com:443/illheartdark/feishudaka.git
git push -u origin main
```

## 10. 二次受阻：密钥未授权 + 沙箱管道限制（2026-10-23 00:40）

| 检查项 | 结果 |
|---|---|
| `github.com:22` | ✅ 端口可达 |
| 专用密钥 SSH（22 与 443） | ❌ `Permission denied (publickey)`，密钥未在 GitHub 注册 |
| ssh-agent | 未运行，无可用密钥 |
| 远程仓库（API） | ✅ 存在且**为空**（`size=0`，`Git Repository is empty`） |
| `git push` | ❌ `sh.exe: couldn't create signal pipe, Win32 error 5` |

**两个独立阻塞：**

1. **授权**：`git@github.com:owner/repo.git` 只是地址；GitHub 仍要求一把已注册的密钥。
2. **沙箱**：Git 启动 SSH 需经 MSYS `sh.exe`，而本会话禁止创建命名管道，SSH 无法启动。

**可行路线：**

| 路线 | 做法 | 优点 | 代价 |
|---|---|---|---|
| 甲 | 用户提供 PAT → Node 调 `api.github.com`（已实测可达）走 Git Data API 建首个提交 | 绕开 SSH 与管道限制，一次成功 | Token 出现在对话中，建议用后即废 |
| 乙 | 用户授权公钥 + 本会话提权执行一次 `git push` | 不传 token，后续可持续推送 | 需一次「完全权限」批准；每次推送仍受管道限制 |

## 11. 推送完成记录（2026-10-23 00:52）

**已通过 GitHub REST API（Node + `api.github.com`）把提交推送上去。** 过程与结果：

| 步骤 | 结果 |
|---|---|
| Token 认证 | ✅ `illheartdark` |
| 空仓库限制 | ⚠️ 空仓库无法调 Git Data API（`409 Git Repository is empty`），改用 Contents API 先落 `.gitignore` 作为根提交 |
| 根提交 | `bd64603` `chore: 初始化仓库` |
| 5 次历史重建 | ✅ `a9f2fbc` → `5415db7` → `1629b83` → `2afed2f` → `bf5ca31` |
| 远端 `main` | `bf5ca316b06c7dc437911116f4612ccb12a019db` |
| 远端提交数 / 文件数 | 6 / 26 |

**必须知道的两点：**

1. **远端是本项目在 GitHub 上的正式副本**：<https://github.com/illheartdark/feishudaka>
2. **本地与远端的对象哈希不同**：远端提交由 API 重建，author/committer 时间与父链由 API 生成，因此 commit SHA 与本地不一致（本地 `c48c58e…66c9765` vs 远端 `a9f2fbc…bf5ca31`），**但文件内容一致（26 个文件）**。后续不要直接 `git pull` 合并两者；需要同步时，以远端为准重新 clone，或在本机有 SSH 凭据时用 `git push --force` 覆盖远端。

**安全提醒：** 本次使用的 PAT 已出现在对话记录中，推送完成后应在 GitHub 设置中删除该 token。

### 11.1 后续推送约定（2026-10-23 01:12）

`github.com:443` 在本机被重置、SSH 又被沙箱命名管道限制，因此后续每次推送都沿用同一方式：把本地新增提交逐个用 `git archive` 导出为 blobs/trees，再用 Git Data API 接到**远端当前 HEAD** 之后，最后 PATCH `refs/heads/main`。

要点：

- 每次推送前先查远端 HEAD，作为 `INITIAL_PARENT`；
- 只推「上次同步点之后」的提交，避免重复；
- 推送完成后核对远端提交数与文件数；
- 本地与远端哈希始终不同（对象由 API 重建），**以内容一致为准**，不要 `git pull` 合并。
