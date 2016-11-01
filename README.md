
# MoreTextLayout


## Install

1. Add the JitPack repository to your build file

```
    allprojects {
        repositories {
            ...
            maven { url "https://jitpack.io" }
        }
    }
```

2. Add the dependency

```
    dependencies {
            compile 'com.github.iQuick:MoreTextLayout:v1.0.0'
    }
```

## Use
```xml
<me.imli.lib_moretextlayout.MoreTextLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_marginTop="16dp"
    app:mtlExpandLines="3"
    app:mtlTextExpand="全部"
    app:mtlTextShrink="收缩">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="我与父亲不相见已二年余了....." />
</me.imli.lib_moretextlayout.MoreTextLayout>
```