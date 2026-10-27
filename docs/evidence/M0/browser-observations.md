# 浏览器实际操作记录

日期：2026-10-27 Asia/Shanghai，约09:54–09:56。工具：Codex CUA，in-app browser；真实地址 http://127.0.0.1:15173 。无静态mock、无注入token绕过登录。

1. 用本机生成的 `demo_student` 密码在用户名密码表单登录。推荐列表显示合成Java岗位；截图 student-home.png。
2. 简历管理新增“M0 浏览器合成简历”，填合成学生A/上海/本科/后端开发并保存；页面显示“保存成功”，重新加载后列表仍在，截图 student-resume-saved.png。
3. 查看学生投递列表，能看到API探针留下的两条同岗位投递；截图 student-applications.png。随后从菜单退出，回到登录。
4. 表单登录demo_hr，进入岗位管理，看到审核通过Java岗位与2条投递；hr-jobs.png。打开本人投递列表，显示合成学生A、标签Java与基线高/低匹配，hr-applicants.png。
5. 浏览器发布“M0 浏览器合成 Vue 岗位”，上海/前端开发/8000-10000/合成描述；保存后提示“岗位将进入待审核”。
6. 表单登录demo_admin，进入审核管理→岗位审核，看到新岗位id=3，admin-pending-job.png；点击通过，队列清空并显示“已处理”，admin-audit-done.png。
7. 表单重新登录demo_student，推荐列表出现新岗位，点击其“投递简历”，显示“已投递”，browser-applied.png。进入投递记录看到Vue岗位新增记录，browser-application-persisted.png。
8. 随后运行restart_check.py真实重启JVM，新登录后API再确认3条投递存在。当前旧浏览器会话需要重新登录，属于memory会话预期。
9. 约10:08，实际刷新浏览器回到登录页，重新表单登录demo_student，再次进入投递记录确认3条仍在；post-restart-student.png。最终保留此已重新认证页面供本机查看。

截图原图保存在screenshots/，没有合成截图；不是对390px移动端、加载空态、全部页面和全部权限的完整验收。UI时间当前显示UTC字符串而非转换为上海时区，属于后续适配事项。
