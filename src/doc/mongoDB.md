## mongoDB 环境安装并集成SpringBoot使用
### mongoDB的安装
为了安装配置的快捷性采用docker的方式进行安装，所以需要提前准备好docker环境
#### 下载镜像
使用命令`docker pull` 下载最新的镜像
```shell
$ docker pull mongo
Using default tag: latest
latest: Pulling from library/mongo
eaead16dc43b: Downloading
8a00eb9f68a0: Download complete
f683956749c5: Download complete
b33b2f05ea20: Download complete
3a342bea915a: Download complete
fa956ab1c2f0: Download complete
138a8542a624: Download complete
acab179a7f07: Download complete
f88335710e84: Download complete
latest: Pulling from library/mongo
eaead16dc43b: Pull complete
8a00eb9f68a0: Pull complete
f683956749c5: Pull complete
b33b2f05ea20: Pull complete
3a342bea915a: Pull complete
fa956ab1c2f0: Pull complete
138a8542a624: Pull complete
acab179a7f07: Pull complete
f88335710e84: Pull complete
Digest: sha256:71a63fc2438e45714f6c8a2505968ee0beeb94ec77a88ef12190f7cee9b95f32
Status: Downloaded newer image for mongo:latest
docker.io/library/mongo:latest
```
查看当前本地的镜像`docker images`
```shell
$ docker images
REPOSITORY                     TAG          IMAGE ID       CREATED        SIZE
postgres                       latest       68f5d950dcd3   44 hours ago   379MB
mongo                          4.4.18-rc0   82bc21bcac12   6 days ago     438MB
mongo                          latest       b70536aeb250   3 weeks ago    695MB
```
#### 运行镜像
通过命令运行mongo镜像
```shell
$ docker run -d -p 27018:27017 --name mongodev mongo:4.4.18-rc0
be65ce82152b54b5339b3c5f74ad2ed153f82ff1c78a06463e38960f03bdb785
# 查看当前正在运行的镜像
$ docker ps
CONTAINER ID   IMAGE              COMMAND                  CREATED          STATUS         PORTS                                        NAMES
be65ce82152b   mongo:4.4.18-rc0   "docker-entrypoint.s…"   10 seconds ago   Up 8 seconds   0.0.0.0:27018->27017/tcp                     mongodev
```
#### 通过配置认证的方式启动镜像
通过增加运行镜像的参数`--auth`实现认证
```shell
$ docker run -d -p 27018:27017 --name mongodev mongo:4.4.18-rc0 --auth
de4a075da4c411c7f93610f687c94b064e3a517209eb4e699c760f562059bf99

$ docker ps
CONTAINER ID   IMAGE              COMMAND                  CREATED         STATUS         PORTS                                        NAMES
de4a075da4c4   mongo:4.4.18-rc0   "docker-entrypoint.s…"   5 seconds ago   Up 5 seconds   0.0.0.0:27018->27017/tcp                     mongodev
```
#### 配置用户和密码并分配权限
在配置用户前首先需要需要进入mongo shell，然后创建管理用户
> 如果使用最新的镜像则需要使用 `docker exec -it mongodev mongosh admin` 进入mongo shell
```shell
$ docker exec -it mongodev mongo admin
MongoDB shell version v4.4.18-rc0
connecting to: mongodb://127.0.0.1:27017/admin?compressors=disabled&gssapiServiceName=mongodb
Implicit session: session { "id" : UUID("d752b753-5e24-408b-85d6-5a9b247d3bc4") }
MongoDB server version: 4.4.18-rc0
Welcome to the MongoDB shell.
For interactive help, type "help".
For more comprehensive documentation, see
	https://docs.mongodb.com/
Questions? Try the MongoDB Developer Community Forums
	https://community.mongodb.com
>
```
1. 切换到`admin` 数据库
2. 创建用于管理其他账户的管理员用户
3. 重新启动具有控制管理的实例
```shell
> use admin
switched to db admin
> db
admin
> db.createUser(
   {
     user: "admin",
     pwd: passwordPrompt(), // or cleartext password
     roles: [
       { role: "userAdminAnyDatabase", db: "admin" },
       { role: "readWriteAnyDatabase", db: "admin" }
     ]
   }
 )
Enter password:
Successfully added user: {
	"user" : "admin",
	"roles" : [
		{
			"role" : "userAdminAnyDatabase",
			"db" : "admin"
		},
		{
			"role" : "readWriteAnyDatabase",
			"db" : "admin"
		}
	]
}
```
> `passwordPrompt()`该方法用于提示用户输入密码，该方式可以避免由于shell的历史记录泄露密码

使用命令`db.auth("userName","password")`进行认证
```shell
> use testDb
switched to db testDb
> db.testDb.insert({"name": "1234"})
WriteCommandError({
	"ok" : 0,
	"errmsg" : "command insert requires authentication",
	"code" : 13,
	"codeName" : "Unauthorized"
})
> db.auth("admin","admin")
Error: Authentication failed.
0
> use admin
switched to db admin
> db.auth("admin","admin")
1
> use testDb
switched to db testDb
> db.c1.insert({"test": "test"})
WriteResult({ "nInserted" : 1 })
> db.c1.find()
{ "_id" : ObjectId("6375a3afd8608931bf3d0995"), "test" : "test" }
```
- mongoDB的权限信息官方文档：https://www.mongodb.com/docs/manual/reference/built-in-roles/#std-label-built-in-roles
#### 相关命令简介
官方文档地址：https://www.mongodb.com/docs/manual/reference/

#### 通过Spring Boot连接mongoDB数据库
创建用于远程连接的用户
```shell
> use admin
switched to db admin
> db.createUser({ user: 'JavaUser', pwd: '123456', roles: [ { role: "dbOwner", db: "testDb" } ] });
Successfully added user: {
	"user" : "JavaUser",
	"roles" : [
		{
			"role" : "dbOwner",
			"db" : "testDb"
		}
	]
}
>
```
创建 Spring Boot 项目并引入 mongo start
```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
```
设置配置文件
```yaml
server:
  port: 8081
spring:
  data:
    mongodb:
      username: JavaUser
      password: "123456"
      database: testDb
      authentication-database: admin
      port: 27017
```
测试连接
```java
@SpringBootTest
class MongoDemoApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(MongoDemoApplicationTests.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void contextLoads() {
        User user = new User();
        user.setName("tom");
        user.setAge(20);
        user.setGender("man");
        var insert = mongoTemplate.save(user,"test");
        var result = mongoTemplate.getCollection("test").find(new JsonObject("{name: \"tom\"}"));
        log.info(result.first().toJson());
    }

}
```
```text
2022-11-17 11:20:54.922  INFO 50859 --- [           main] org.mongodb.driver.connection            : Opened connection [connectionId{localValue:3, serverValue:71}] to localhost:27017
2022-11-17 11:20:54.974  INFO 50859 --- [           main] c.d.mongodemo.MongoDemoApplicationTests  : {"_id": {"$oid": "6375a8967b990d6cbf5a565b"}, "name": "tom", "age": 20, "gender": "man", "_class": "com.demo.mongodemo.domian.User"}
```