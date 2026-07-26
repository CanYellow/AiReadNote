package com.example.helloworld

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 用代码直接创建一个文本视图，不使用 xml 布局文件以保持极简
        val textView = TextView(this)
        textView.text = "Hello World from WSL2!"
        textView.textSize = 24f

        setContentView(textView)
    }
}
