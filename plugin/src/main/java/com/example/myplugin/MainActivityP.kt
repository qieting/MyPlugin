package com.example.myplugin

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.AssetManager
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class MainActivityP : AppCompatActivity() {


    //增加log，区分两个activity，同时二者界面也有不同
    init {
        Log.e("test", "activity p  oncreate")
    }

    //我们需要修改插件的resourcer目录，原因是默认使用application的resource
    //而我们插件启动默认用的是宿主的，因此需要修改
    var pluginResources: Resources? = null

    @SuppressLint("WrongConstant")
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        try {

            val oPackageInfo: PackageInfo? = packageManager.getPackageArchiveInfo(
                "${filesDir.path}/plugin.apk",
                Int.MAX_VALUE
            )
            pluginResources =
                oPackageInfo?.let { packageManager?.getResourcesForApplication(it.applicationInfo) }
//          也可以用这种方式获取，但是这样增加了不必要的hook，而replugin'的宣传点就是one hook
//            pluginAssetManager = AssetManager::class.java.newInstance() as AssetManager
//            val mm = pluginAssetManager!!::class.java.getMethod("addAssetPath", String::class.java)
//            mm.invoke(pluginAssetManager, "${filesDir.path}/plugin.apk")
//            pluginResources =
//                Resources(pluginAssetManager, resources.displayMetrics, resources.configuration)
        } catch (t: Throwable) {
            Log.e("test", t.message!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //无用代码
        PluginMessage.getMessage()
    }

    override fun getResources(): Resources {
        return pluginResources ?: super.getResources()
    }
}