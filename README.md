# ClassRemapper
class's package name remapper using javassist


And I provided a bugly patch sample.

bugly的patch主要将其变成一个通用的bug上报组件，废弃其java crash/native crash/anr的捕获能力
捕获交由其他组件，bugly仅负责上报。