package com.example.myplugin

import android.util.Log

//该类为了演示宿主反射插件类
class PluginMessage {

    companion object {
        fun getPlugin() = Plugin("1.0.1")

        fun getMessage() {
            Log.e("test",javaClass.name)
        }
    }


}