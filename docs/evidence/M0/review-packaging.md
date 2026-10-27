# 提交打包核对

`git diff --cached --check` 对原始Maven日志和启动横幅的行尾空格提出警告；这些是实际stdout，保持原始输出，不清洗成假原日志。排除 `docs/evidence/M0/*.log` 后执行同一检查退出0，源代码、配置、报告无该类问题。

最终暂存项检查排除.local、数据库文件、私有凭证和生成class；实际随机密码值扫描通过。原LICENSE与指定上游字节一致。提交/推送命令使用 `git commit`、`git push -u origin m0/baseline`；其完整SHA和远程对齐结果在交付消息中提供，不使用含自身SHA的循环文件。没有推送上游或其他仓库。
