# ClassRemapper
class's package name remapper using javassist


And I provided a bugly patch sample.

bugly的patch主要将其变成一个通用的bug上报组件，废弃其java crash/native crash/anr的捕获能力

捕获交由其他组件，bugly仅负责上报。

处理后的bugly可与原始bugly共存，上报至配有其他appId的应用里，场景：Flutter异常上报到独立应用中进行管理与统计


## How to use

```
allprojects {
    repositories {
        maven() {
            url "https://maven.pkg.github.com/"
        }
        google()
        jcenter()
    }
    configurations.all {
        it.resolutionStrategy.cacheDynamicVersionsFor(5, 'minutes')
        it.resolutionStrategy.cacheChangingModulesFor(0, 'seconds')
    }
}


dependencies {
    implementation 'io.github.lizhangqu:remapper:1.0.0'
}
```