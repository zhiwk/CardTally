# 可选启动统计

采用 PostHog 美国区直接 HTTP 上报，复用 OkHttp/WorkManager，无统计 SDK，无财务数据库迁移。

- 正式包且配置项目写入 Token 时提供开关，按用户新要求默认开启：仅当 enabled 偏好不存在时使用 true，保存过的关闭状态不覆盖。首次可统计的进入前台生成随机 UUID 并记录本次启动；设置说明可点击查看完整范围，主动关闭后重新开启仍提供原生确认；不回溯历史启动。dev/verification 不发送。
- app_launch 仅含安装标识、事件 UUID、时间、版本及 release 渠道；person profile 和 GeoIP enrichment 关闭。不访问财务内容、硬件标识或主动采集 IP，说明接收方仍可看到连接 IP。
- 每进程首次可统计的 onResume 只记一次；重建、后台返回、开关反复启用不重复本进程事件。事件数据固定，最终去重可能有延迟，不承诺精确真人数。
- 有网络后异步发送；断网/IO/429/5xx 最多额外三次指数退避重试，七天过期停止，其他非成功状态停止；无重定向，不记录请求体、Token 或服务响应。
- 关闭立即撤销本机状态，取消进行中的调用与待发送 WorkManager 任务，清理随机标识；已上传事件不自动删除。再次启用生成新安装标识，旧标识任务即使恢复也不发送。
- 独立偏好 launch_analytics 排除完整/云备份；Token 构建时来自未提交 analytics.properties 或环境变量 POSTHOG_PROJECT_TOKEN，CI 从同名 Repository Secret 提供。未配置则禁用功能，不上传任何数据。

仅源码实施与静态复核；构建/安装/真实上报及后端报表验证按后续授权执行。
