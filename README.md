### 简介
> SmartFlyper是一个工具类,模仿retrofit使用动态代理模式实现，简化了yyp协议传输字符串方式，使得开发者无需实现重复的工作。

*参考代码*
```java
public interface IRoomInfo{
        @SmartUri(max = 101,req = 312,rsp =313)
        //uid-confirmed
        Observable<String> getMyRoomInfo(@SmartParam("uid") long uid,
                                         @SmartParam("version") String version,@SmartParam("pf") int pf);

        @SmartUri(max = 101,req = 312,rsp =313)
        //uid-confirmed
        Observable<UserRoomInfo> getMyRoomInfo2(@SmartParam("uid") long uid,
                                                @SmartParam("version") String version,@SmartParam("pf") int pf);
        @SmartUri(max = 101,req = 308,rsp =309)
        //uid-confirmed
        Observable<String> getRoomDetailInfo(@SmartParam("channelId") long channelId,@SmartParam("uid") long uid,
                                             @SmartParam("version") String version);

        @SmartBroadCast(max=101,min=310)
        String onGetRoomDetailInfoBroadcast(SmartObserverResult<RoomDetailRspInfo> observerResult);
    }
```
*通过这种方法，即可实现yyp协议的注入*
### 思考
> 动态代理虽好，但是会在运行时依赖反射原理生成很多接口的代理类，而编译时注解可以自动生成接口的代理类，运行的时候，无需再使用反射来调用接口方法了。这样既可以提高运行效率，又可以在编译时检测接口的正确性。

*工程结构图*

![component](http://makefriends.bs2dl.yy.com/TIM%E6%88%AA%E5%9B%BE20181018182906.png)

>mainapi是提供给其他插件使用的，app模块为ui层，annotion，compiler为注解模块，提供给其他插件使用。

*类结构关系图*
![类结构](http://makefriends.bs2dl.yy.com/QQ20181018-174056%402x.png)
- 调用ISkillServer接口的方法时，会调用ISkillServer$$Delegate实现类的方法，而它是apt生成的，如何关联到SmartFlyper来发送协议呢？
- 使用SmartFlyperDelegate类调用SmartFlyper的Create方法。
- 而SmartFlyper会调用ISmartFlyperFactory的实现类找到具体的api代理方法，而每个module下面都有自己的api工厂类，如SmartFlyper$$main_app，在app模块下自动生成的。

### 如何使用？[![](https://jitpack.io/v/ly-android/smartflyper.svg)](https://jitpack.io/#ly-android/smartflyper)


*加入到root build.gradle中*
```groovy
allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
```
> 1.在需要的使用到注解的module中引入smartflyper annotation和compiler的依赖
```gradle
dependencies {
    implementation 'com.github.ly-android.smartflyper:annotation:1.0.1'
    annotationProcessor 'com.github.ly-android.smartflyper:compiler:1.0.1'
  }
```

> 2.然后在当前module的android节点defaultConfigs下面配置moduleName(保证唯一)

```gradle
javaCompileOptions {
            annotationProcessorOptions {
                arguments += [moduleName: "main_app"]
          }
        }
```
> 3.接下来需要注册apt生成的api工厂，一般在插件入口PluginEntry.initialize注册

```java
SmartFlyper.getInstance().registerFactory(new SmartFlyperFactory$$main_app());
        SmartFlyper.getInstance().registerFactory(new SmartFlyperFactory$$main_core());
        SmartFlyper.getInstance().registerFactory(new SmartFlyperFactory$$main_api());
```
> 4.如果是动态插件，还需要在插件的terminate中卸载api工厂

```java
SmartFlyper.getInstance().unRegisterFactory(SmartFlyperFactory$$main_app.class);
```
> 5. api接口写法和之前保持一致。
```java

@LazyInit(value=true)
public interface IRoomInfo{
        
        @SmartUri(max = 101,req = 312,rsp =313)
        Observable<String> getMyRoomInfo(@SmartParam("uid") long uid,
                                         @SmartParam("version") String version,@SmartParam("pf") int pf);

        @SmartUri(max = 101,req = 312,rsp =313)
        Observable<UserRoomInfo> getMyRoomInfo2(@SmartJson String json);
        
        @SmartUri(max = 101,req = 333,rsp =334)
        Observable<BaseEntity> getMyRoomInfo3(@SmartMap Map<String,String> map);
        
        @SmartBroadCast(max=101,min=310)
        String onGetRoomDetailInfoBroadcast(SmartObserverResult<RoomDetailRspInfo> observerResult);
    }
```
*注意*
- 在接口中不能有其他非注解形式的抽象方法，否则编译不过
- LazyInit注解默认是false，表示是否需要延迟创建api实现类;如果true,在使用SmartFlyper.getInstance().Create()的时候才会实例化。



