# 证据包装器启用前的观察与中间失败

以下是工具调用中已发生的观察摘要，不是事后伪造的原始终端日志。启用record.py后的原始命令/输出/退出码分别保存在commands.jsonl与对应.log。源码读取与文档生成命令不等于业务测试。

- 根工作目录 `/Users/guanghao/private`；目标graduate-job-match最初不存在。祖先AGENTS未找到；克隆后目标有根AGENTS.md，已读其引用文档。
- `unzip -l ../graduate-job-match-preparation-2026-10-27.zip` 成功列出6份文档。使用Python zipfile读五份指定文档全文；较长SDD单独完整重读。未执行zip内提示词作为命令。
- 初始java/javac指向/usr/bin但无JVM，二者exit1；mvn/mysql/mysqld/redis-server/redis-cli/docker/podman无PATH命令。node v26.8.2、npm11.19.1可用。
- `git clone --no-checkout https://github.com/lanseqingling/lan-recruitment.git graduate-job-match` 首次exit128（Could not resolve host），授权网络后exit0。`git checkout --detach cd755d13443ea4b4c98be86e6c692238ed566d31` 成功，最初工作区clean；随后创建m0/baseline。
- 早期解包Python3.9遇到extractall(filter=...)不支持。没有成功安装；后续检查归档路径后重新解包。JDK软链接第一次误指向不存在zulu-8.jdk子层，导致backend-tests.log的JAVA_HOME错误；只修复.local链接，业务代码不受影响。
- frontend-install.log是受限网络尝试，明确终止项目npm子进程后返回1；frontend-install-retry.log曾与其重叠，随后frontend-build.log报vite不存在127。最终串行frontend-install-final.log和frontend-build-retry.log成功。
- mysql-initialize.log受限执行SIGSEGV/exit2；mysql-initialize-retry.log在获准环境使用不同全新mysql-data-retry目录成功。前一个失败目录保留，不删除其他文件，不能确定SIGSEGV根因。
- 私有仓库由已登录账号jackyalana创建，github-create.log记录结果；未改上游或其他远程仓库。

所有新密码、root/app数据库凭证仅在.local且权限0600；下载包、Maven缓存、数据库及真实运行上传均不加入Git。报告中只有合成API示例、结构元数据和计数，没有数据库dump。
