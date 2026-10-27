# M1 浏览器观察

在本机 Vite 15174（demo标记）和 M1后端18081、隔离合成库上实际操作。截图只展示合成数据。

- 早期 M1 构建：demo_student 登录，保存专业代码并刷新仍保留（student-profile.png）；HR 创建岗位待审（hr-job-pending.png），ADMIN 审核（admin-pending.png）。这些是中间构建证据。
- 添加审核版本后，浏览器 HR 创建待审岗位，管理员列表提交版本并成功处理（release-audit-pending.png）；SQL/API对旧版本拒绝见 http-tests-release.log。此截图在最后一次时区改动之前。
- 此前构建：demo_student 真实登录，首页显示合成数据标记和北京时间 11:00，截图 final-student-home.png；画像读取展示本科、2026、城市偏好，截图 final-profile.png。
- 此前构建经浏览器使用合成信息注册 m1_final_student，显示“演示注册成功，邮箱未验证”（final-registration.png）；登录后首页提示完善学历/毕业年份（final-new-student.png）。

截图仅证明对应页面与操作；不代表所有浏览器、移动端或全流程体验通过。完整权限判定以服务端和真实API测试为准。

- 最终后端重建后重新使用 demo_student 浏览器登录，首页及画像分别见 handoff-student-home.png 和 handoff-profile.png；上列其他截图属于中间构建。
