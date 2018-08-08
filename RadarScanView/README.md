# Android 雷达扫描控件

[![](https://jitpack.io/v/kongqw/AndroidRadarScanView.svg)](https://jitpack.io/#kongqw/AndroidRadarScanView)

To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

``` gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency

``` gradle
dependencies {
        compile 'com.github.kongqw:AndroidRadarScanView:1.0.1'
}
```

## 效果图

![AndroidRadarScanView](http://img.blog.csdn.net/20170310182212626?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvcTQ4Nzg4MDI=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

![AndroidRadarScanView](http://img.blog.csdn.net/20170310182359258?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvcTQ4Nzg4MDI=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

## XML

``` xml
    <com.kongqw.radarscanviewlibrary.RadarScanView
        android:id="@+id/radarScanView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

## 初始化

``` java
radarScanView = (RadarScanView) findViewById(R.id.radarScanView);
```

## 设置属性

### XML

``` xml
xmlns:app="http://schemas.android.com/apk/res-auto"
```

``` xml
<com.kongqw.radarscanviewlibrary.RadarScanView
    android:id="@+id/radarScanView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_centerInParent="true"
    app:radarBackgroundColor="@color/colorAccent"
    app:radarBackgroundLinesColor="@color/colorPrimaryDark"
    app:radarBackgroundLinesNumber="3"
    app:radarBackgroundLinesWidth="5.5"
    app:radarScanAlpha="0x33"
    app:radarScanColor="#FF000000"
    app:radarScanTime="5000" />
```


| 属性 | 类型 | 描述 |
| --- | ----| ---- |
| radarScanTime | integer | 设置雷达扫描一圈时间 |
| radarBackgroundLinesNumber | integer | 设置雷达背景圆圈数量 |
| radarBackgroundLinesWidth | float | 设置雷达背景圆圈宽度 |
| radarBackgroundLinesColor | color | 设置雷达背景圆圈颜色 |
| radarBackgroundColor | color | 设置雷达背景颜色 |
| radarScanColor | color | 设置雷达扫描颜色 |
| radarScanAlpha | integer | 设置雷达扫描透明度 |


### Java

``` java
radarScanView
        // 设置雷达扫描一圈时间
        .setRadarScanTime(2000)
        // 设置雷达背景颜色
        .setRadarBackgroundColor(Color.WHITE)
        // 设置雷达背景圆圈数量
        .setRadarBackgroundLinesNumber(4)
        // 设置雷达背景圆圈宽度
        .setRadarBackgroundLinesWidth(2)
        // 设置雷达背景圆圈颜色
        .setRadarBackgroundLinesColor(Color.GRAY)
        // 设置雷达扫描颜色
        .setRadarScanColor(0xFFAAAAAA)
        // 设置雷达扫描透明度
        .setRadarScanAlpha(0xAA);
```

## 备用

### 手动开始扫描

``` java
radarScanView.startScan();
```

### 手动停止扫描

``` java
radarScanView.stopScan();
```

