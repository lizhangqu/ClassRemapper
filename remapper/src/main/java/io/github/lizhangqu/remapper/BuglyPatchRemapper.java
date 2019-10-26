package io.github.lizhangqu.remapper;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.ConstPool;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BuglyPatchRemapper extends ClassRemapper {

    File androidJar;

    public BuglyPatchRemapper(File srcFile, File outFile, String originalPrefix, String newPrefix) {
        super(srcFile, outFile, originalPrefix, newPrefix);
    }

    public BuglyPatchRemapper(File srcFile, File outFile, String originalPrefix, String newPrefix, String originalAndroidPackageName, String newAndroidPackageName) {
        super(srcFile, outFile, originalPrefix, newPrefix, originalAndroidPackageName, newAndroidPackageName);
    }

    @Override
    protected void edit(ClassPool classPool, CtClass ctClass) throws Exception {
        super.edit(classPool, ctClass);
        if (androidJar == null) {
            throw new RuntimeException("please set android.jar path using setAndroidJar() method");
        } else {
            classPool.insertClassPath(androidJar.getAbsolutePath());
        }
        editConstPool(ctClass);
        editCall(ctClass);
    }

    public void setAndroidJar(File androidJar) {
        this.androidJar = androidJar;
    }

    private void editCall(CtClass ctClass) throws CannotCompileException {
        CtBehavior[] declaredBehaviors = ctClass.getDeclaredBehaviors();
        for (CtBehavior ctBehavior : declaredBehaviors) {
            ctBehavior.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall methodCall) throws CannotCompileException {
                    super.edit(methodCall);
                    if (!ctClass.getName().contains("com.tencent.bugly.proguard.z")
                            && methodCall.getClassName().equals("com.tencent.bugly.proguard.z")
                            && ((methodCall.getMethodName().equals("a") && methodCall.getSignature().equals("(Landroid/content/Context;Ljava/lang/String;J)Z"))
                            || (methodCall.getMethodName().equals("b") && methodCall.getSignature().equals("(Landroid/content/Context;Ljava/lang/String;)Z")))
                    ) {
                        System.out.println(ctClass.getName() + ": " + methodCall.getClassName() + "." + methodCall.getMethodName() + methodCall.getSignature() + " -> Add prefix `shadow_` to second param.");
                        //对文件锁调用加入shadow前缀
                        methodCall.replace("$2 = \"shadow_\"+ $2; $_ = $proceed($$);");
                    } else if ((!ctClass.getName().contains("com.tencent.bugly.crashreport.crash.e") && methodCall.getClassName().equals("com.tencent.bugly.crashreport.crash.e"))
                            || (!ctClass.getName().contains("com.tencent.bugly.crashreport.crash.anr.b") && methodCall.getClassName().equals("com.tencent.bugly.crashreport.crash.anr.b"))
                            || (!ctClass.getName().contains("com.tencent.bugly.crashreport.crash.jni.NativeCrashHandler") && methodCall.getClassName().equals("com.tencent.bugly.crashreport.crash.jni.NativeCrashHandler"))
                    ) {
                        //废掉java捕获功能
                        //将非类com.tencent.bugly.crashreport.crash.e对类com.tencent.bugly.crashreport.crash.e任何函数调用加入前置null判断
                        //废掉anr捕获功能
                        //将非类com.tencent.bugly.crashreport.crash.anr.b对类com.tencent.bugly.crashreport.crash.anr.b任何函数调用加入前置null判断
                        //废掉jni捕获功能
                        //将非类com.tencent.bugly.crashreport.crash.jni.NativeCrashHandler对类com.tencent.bugly.crashreport.crash.jni.NativeCrashHandler任何函数调用加入前置null判断

                        System.out.println(ctClass.getName() + ": " + methodCall.getClassName() + "." + methodCall.getMethodName() + methodCall.getSignature() + " -> Add null check for method call.");

                        methodCall.replace("if($0 != null) $_ = $proceed($$);");
                    }
                }


                @Override
                public void edit(NewExpr newExpr) throws CannotCompileException {
                    super.edit(newExpr);
                    if (newExpr.getClassName().equals("com.tencent.bugly.crashreport.crash.e")
                            || newExpr.getClassName().equals("com.tencent.bugly.crashreport.crash.anr.b")
                            || newExpr.getClassName().equals("com.tencent.bugly.crashreport.crash.jni.NativeCrashHandler")
                    ) {
                        //废掉java捕获功能
                        //将对com.tencent.bugly.crashreport.crash.e构造函数调用置空
                        //废掉anr捕获功能
                        //将对com.tencent.bugly.crashreport.crash.anr.b构造函数调用置空
                        //废掉native捕获功能
                        //将对com.tencent.bugly.crashreport.crash.jni.NativeCrashHandler构造函数调用置空

                        System.out.println(ctClass.getName() + ": " + newExpr.getClassName() + newExpr.getSignature() + " -> Remove new object call.");


                        newExpr.replace("$_ = null;");
                    }
                }

                @Override
                public void edit(FieldAccess fieldAccess) throws CannotCompileException {
                    super.edit(fieldAccess);
                    //不是com.tencent.bugly.crashreport.crash.d访问的不处理
                    //不是com.tencent.bugly.crashreport.crash.CrashDetailBean上的字段不处理
                    //不是字段b不处理
                    //字段B的签名不是int不处理
                    //强制设置b的值为0
                    if (ctClass.getName().equals("com.tencent.bugly.crashreport.crash.d")
                            && fieldAccess.getClassName().equals("com.tencent.bugly.crashreport.crash.CrashDetailBean")
                            && fieldAccess.getFieldName().equals("b")
                            && fieldAccess.getSignature().equals("I")) {
                        //0为java crash

                        System.out.println(ctClass.getName() + ": " + fieldAccess.getClassName() + "." + fieldAccess.getFieldName() + " " + fieldAccess.getSignature() + " -> Set field value = 0.");

                        fieldAccess.replace(String.format("$0.%s = 0;", fieldAccess.getFieldName()));

                    }
                }
            });

        }
    }

    private void editConstPool(CtClass ctClass) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        //常量池修改
        String name = ctClass.getName();
        if (name.equals("com.tencent.bugly.proguard.y")
                || name.equals("com.tencent.bugly.proguard.q")
                || name.equals("com.tencent.bugly.proguard.n")
                || name.equals("com.tencent.bugly.proguard.x")
                || name.equals("com.tencent.bugly.crashreport.common.info.a")
                || name.equals("com.tencent.bugly.crashreport.crash.anr.b")
                || name.equals("com.tencent.bugly.crashreport.crash.jni.NativeCrashHandler")
                || name.equals("com.tencent.bugly.crashreport.crash.d")
        ) {
            ConstPool constPool = ctClass.getClassFile().getConstPool();
            Field items1 = constPool.getClass().getDeclaredField("items");
            items1.setAccessible(true);
            Object items = items1.get(constPool);

            Class<?> Utf8InfoClass = Class.forName("javassist.bytecode.Utf8Info");
            Field tag1 = Utf8InfoClass.getDeclaredField("tag");
            tag1.setAccessible(true);
            int tagFlag = (int) tag1.get(null);


            Method elementAt = items.getClass().getDeclaredMethod("elementAt", int.class);
            elementAt.setAccessible(true);

            int size = constPool.getSize();
            for (int i = 1; i < size; ++i) {
                int tag = constPool.getTag(i);
                //noinspection GroovyAccessibility
                if (tag == tagFlag) {
                    String utf8Info = constPool.getUtf8Info(i);
                    if (utf8Info.equals("/buglylog_")) {
                        //将常量池/buglylog_修改为/shadow_buglylog_
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "/shadow_buglylog_");

                    } else if (utf8Info.equals("bugly_db_")) {
                        //将常量池bugly_db_修改为shadow_bugly_db_
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "shadow_bugly_db_");
                    } else if (utf8Info.equals("bugly_db")) {
                        //将常量池bugly_db修改为shadow_bugly_db
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "shadow_bugly_db");
                    } else if (utf8Info.equals("crashrecord")) {
                        //将常量池crashrecord修改为shadow_crashrecord
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "shadow_crashrecord");
                    } else if (utf8Info.equals("bugly")) {
                        //将常量池bugly修改为bugly
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "shadow_bugly");
                    } else if (utf8Info.equals("/app_bugly")) {
                        //将常量池/app_bugly修改为/shadow_app_bugly
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "/app_shadow_bugly");
                    } else if (utf8Info.equals("H5")) {
                        //将常量池H5修改为Other
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "Other");
                    } else if (utf8Info.equals("CrashReportInfo")) {
                        //将常量池CrashReportInfo修改为ShadowCrashReportInfo
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "ShadowCrashReportInfo");
                    } else if (utf8Info.equals("CrashReport")) {
                        //将常量池CrashReport修改为ShadowCrashReport
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "ShadowCrashReport");
                    }

                    Object item = elementAt.invoke(items, i);
                    Field string = item.getClass().getDeclaredField("string");
                    string.setAccessible(true);
                    String currentValue = (String) string.get(item);
                    if (!utf8Info.equals(currentValue)) {
                        System.out.println(ctClass.getName() + ": " + utf8Info + " -> " + currentValue);
                    }
                }
            }
        }
    }
}
