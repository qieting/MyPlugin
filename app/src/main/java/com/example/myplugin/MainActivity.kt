package com.example.myplugin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.myplugin.utils.AssetsUtils
import dalvik.system.DexClassLoader
import java.io.File
import java.lang.reflect.Method

class MainActivity : AppCompatActivity() {


    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        //该步骤是将apk文件从aaets目录移到file目录下
        AssetsUtils.extractTo(this, "plugin-debug.apk", filesDir.path, "plugin.apk")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 想要修改activity启动时使用的类加载器，需要找到new 一个activity时使用的classLoader
        // 通过对AcitiyThread类的分析，找到相对应使用的classLoader对象
        // 确定是需要修改application的baseContext的packageInfo的classLoder参数
        //具体查找方式可以查看ActivityThread源码中的performLaunchActivity方法，as即可查看
        try {
            //这几部都是在获取application的baseContext的packageInfo的classLoder参数
            var filed = application.baseContext::class.java.getDeclaredField("mPackageInfo");
            filed.isAccessible = true
            //这里的oPackageInfo类型是LoadApk
            val oPackageInfo = filed.get(application.baseContext)
            filed = oPackageInfo::class.java.getDeclaredField("mClassLoader");
            filed.isAccessible = true
            val originClaaLoader = filed.get(oPackageInfo) as ClassLoader;

            //自定义classLoder对象，将原有的classLoder作为父classLoder
            val classLoader = object : DexClassLoader(
                //此处指定插件的apk路径，就会加载插件的路径
                "${filesDir.path}/plugin.apk",
                //代码解析缓存路径
                getDir("dex1", 0).path,
                null,
                originClaaLoader
            ) {
                override fun loadClass(name: String?, resolve: Boolean): Class<*> {
                    //这里是本类的核心，java有一个特点，就是你可以自定义classLoader，
                    // 1. classLoder加载的类名，与实际返回的类的名字可以不同，这样就可以做到偷梁换柱
                    // 2. classloader遵循双亲委派制，优先交给父类处理，如果插件activity类名和宿主相同，那么就记载宿主，否则插件
                    // 3. 我们默认启动宿主的MainActivity，但是我们其实想启动的是插件的，因此做一个class name的替换
                    // 4. 你可能想问，如果插件activity名和宿主activity名真的相同改怎么处理呢，其实这就是replugin插件代码那么多的原因了
                    //   拓展： replugin会预设专门的容器activity去容纳我们插件activity，当然，这就超出我们这个项目的范围了
                    var newName = name;
                    if (name == "com.example.myplugin.MainActivity") {
                        newName = "com.example.myplugin.MainActivityP"
                    }
                    Log.e("test1", newName!!)
                    return super.loadClass(newName, resolve)
                }


                override fun findClass(name: String?): Class<*> {
                    Log.e("test2", name!!)
                    return super.findClass(name)
                }


            }
            //完成hook
            filed.set(oPackageInfo, classLoader)
        } catch (t: Throwable) {
            Log.e("test", t.message!!)
        }


    }

    @SuppressLint("WrongConstant")
    override fun onResume() {
        super.onResume()

        //下面的代码是用来演示你将classLoader的代码路径设置为插件后可以做到的各种事件，相当于是插件化可以实现所依赖基础

        // 1. 设置classLoder，从插件中读取代码  DexClassLoader
        val file = File(filesDir.path, "plugin.apk")
        file.mkdirs()
        val dexPath = file.path
        val fileRelease = getDir("dex", 0)
        val classLoader = DexClassLoader(dexPath, fileRelease.absolutePath, null, getClassLoader())

        try {
            //下面的代码是从插件的PluginMessage中获取Plugin
            // 但是因为使用的是Kotlin，多加个一个Companion，变麻烦了一点，但是本质是不变的

            // 2. 获取PluginMessage的Companion对象，因为我们要调用其getPlugin方法
            val pluginClass = classLoader.loadClass("com.example.myplugin.PluginMessage\$Companion")
            val method = pluginClass.getMethod("getPlugin")
            // 3. 从PluginMessage获取其Companion实例对象
            val pluginMessageClass = classLoader.loadClass("com.example.myplugin.PluginMessage")
            val re = pluginMessageClass.getField("Companion")
            // 4. 获取到Plugin对象
            val oobject = method.invoke(re.get("com.example.myplugin.PluginMessage\$Companion"));

            if (oobject != null && oobject is Plugin) {
                Log.e("test", oobject.version)
            }

            // 5. 通过packageManager获取插件的各类信息,这里演示获取插件的application类名
            //    拓展：实际上Replugin在插件初始化时就会根据这个方法获取插件所生命的applicaiton和四大组件相关问题
            val pkInfo = packageManager.getPackageArchiveInfo(
                "${filesDir.path}/plugin.apk",
                Int.MAX_VALUE
            )
            Log.e("test", pkInfo!!.applicationInfo.className)


            //这里演示从宿主获取插件的资源文件
            try {
                val pluginAssetManager = AssetManager::class.java.newInstance()
                val mm =
                    pluginAssetManager::class.java.getMethod("addAssetPath", String::class.java)
                mm.invoke(pluginAssetManager, "${filesDir.path}/plugin.apk")
                val pluginResources =
                    Resources(pluginAssetManager, resources.displayMetrics, resources.configuration)
                //加载插件appname
                val pluginStringclass = classLoader.loadClass("com.example.myplugin.R\$string")
                val pluginAppNameId =
                    pluginStringclass.getField("app_name").get(Int.javaClass) as Int;
                val pluginAppName = pluginResources.getString(pluginAppNameId);
                Log.e("test", pluginAppName)
            } catch (t: Throwable) {
                Log.e("test", t.message!!)
            }

        } catch (t: Throwable) {
            t.message?.let { Log.e("test", it) }
        }

        findViewById<View>(R.id.jump).setOnClickListener {
            //我们写的是跳到宿主首页，但是因为在onCreate我们hook了classLoder，就会加载插件首页
            startActivity(Intent(this, MainActivity::class.java))
        }

    }


}