
#本文为借鉴Replugin的原理而写的一个简单的demo，展示RePLugin的核心原理，方便理解插件化


## 本文分为三个module组成
1. app
    我们要运行的app，在插件化中称为宿主。
    我们hook了其加载Activity的ClassLoder。
2. plugin
    我们的插件App
    非插件化时，其可作为单独的app运行
    在插件化时我们改变了其assert查找方式，同时将其打包后直接放到app->src->main->assets目录下   
3. middle
    共用组件，提供Plugin类供1，2使用，本module非必须，只是为了检验app可以反射plugin的类并正常使用
    
 
核心原理：
    classLoder加载的类名，与实际返回的类的名字可以不同，这样就可以做到偷梁换柱。
    宿主主要是替换classLoader，插件主要是处理资源文件的查找。
    详见MainActivity和MainActivityP注解
   