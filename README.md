# ofbiz-mini-saas
基于Ofbiz15.02改造的支持SaaS、企业分应用的基础技术框架
只支持PostgreSql数据库，Mysql5.7以上才支持，现在已实现5.6版的JSON字段（字符串），性能较差，使用起来也不方便比如排序，正在整理
https://github.com/zhengxyit/ofbiz-mini-saas-frontend 本应用的前端，但只对登录做了处理

2018-11-14 但由于项目太紧没时间更新，项目中完成的一些模块要明年春节的时候更新上去了（- -！）
1 元数据管理（当事人、权限、分类、状态工作流）
2 管理功能：定时任务管理、API接口管理（包括用例测试，集成用例测试）

# 关于项目
1. 坚持技术是为业务服务的原则，但业务的问题只能用业务去解决；
2. 本项目仅适用于传统企业，中小微规模；
3. 开发难度低，可一个项目经理轻松调度16人以下的技术支持团队（需要API与任务管理工具）；
4. 适用于业务变更频繁的场景（比如App接口开发，修改Groovy不需要重启Web服务）；

# 运行项目
把idea的配置也发上去了~参考
```javascript
ant build
ant load-demo
ant start
```
启动后，使用postman测试一下http://localhost:8080/sso/login 使用Basic Auth
18612345678/123456


# 修改内容
1. 本项目只作为中间件使用，只保留了后端代码；（考虑到现在前后端分离）
2. 去掉了所有业务模块；（现在追求短平快，基本上用到原模块1/5-1/3的功能基本就能满足需求，需要哪些自己去找吧）
3. 实体引擎方面基本全保留了下来（只支持Mysql，别的数据库的话自己拷贝相关的文件吧），并增加了support-modules（支持到字段级）、is-tenant两件配置
4. 服务引擎只保留了Groovy和Java两个类型、并且只支持Http协议（需要其它去掉注释即可）
5. 安全方面使用权限编码，自己定义编码直接在代码中进行业务代码开发即可，使用时需要在services.xml中配在接口上

# SaaS支持
common中general配置的最后

Y 启用多租户 N 不启用  多租户使用SaaS同库同表方式
```javascript
multitenant=N
saas.home=localhost
```
默认是N，就是单应用项目

# 关于开发
实例见application/example
* 定义实体
* 写Groovy文件
* 定义服务接口
OK~
测试
1. 先将multitenant=Y
2. ant clean/ant build/ant load-demo/ant start顺序执行
3. 在host中加 127.0.0.1	abc.aaa.com/127.0.0.1	xyz.aaa.com 这两条
4. http://abc.aaa.com:8080/sso/login和http://abc.aaa.com:8080/sso/login分别登录（Ticket不同） 使用Basic Auth
5. 在Header中设置X-TICKET=登录中的Ticket　http://abc.aaa.com:8080/example/j/queryEmployeeList/http://xyz.aaa.com:8080/example/j/queryEmployeeList
6. 返回的数据不同，并且有一个有groupId

# 不足之处
* 开发文档，跟Ofbiz差不多，要是会Ofbiz直接上手试试吧，不足就给我留信息
* 测试做的不够全面，未来的日子我会继续用这个框架做业务应用开发，所以会不断的修改BUG
* 本着技术开源业务不开源的原则（通用业务模型设计部分不会开放出来）

# 下一步计划
* 建基本服务引擎的API和自动化测试
* 通过服务引擎暴露出的接口，直接生成Vue页面或组件，减少前端的开发工作量（这个功能可能会是双向的）

# 注意：不再更新
* 快速开始框架主要还是想降低IT开发和运维的成本，计划使用Go（完成Framework中的功能，完全无业务） + Flutter（前端 + 后台CURD）重构框架；可以大大降低学习成本，最重要的还是业务与技术分离；
* 前端部分会先由Flutter代替，逐步实现
