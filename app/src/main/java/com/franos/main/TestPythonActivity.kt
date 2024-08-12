package com.franos.main

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.chaquo.myapplication.R
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.util.concurrent.Executors

class TestPythonActivity : Activity() {
    val mHandler = Handler(Looper.getMainLooper())
    var pyname: EditText? = null
    var methodname: EditText? = null
    var args: EditText? = null
    var result: TextView? = null
    var doneBtn: Button? = null
    val executor = Executors.newCachedThreadPool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_python_layout)
        pyname = findViewById(R.id.pn)
        methodname = findViewById(R.id.methodname)
        args = findViewById(R.id.argsname)
        result = findViewById(R.id.result)
        doneBtn = findViewById(R.id.clickbtn)
        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        val py = Python.getInstance()

        doneBtn?.setOnClickListener {
            val progressDialog = ProgressDialog.show(this@TestPythonActivity, "执行python", "正在执行中，请稍后......")
            progressDialog.show()
            executor.execute {
                try {
                    val moduleName = pyname?.text.toString()
                    val methodName = methodname?.text.toString()
                    val argsName = args?.text.toString()
                    val module = py.getModule(moduleName)
                    val data = module.callAttr(methodName, argsName)
//                    val data = module.callAttr("get_html", edt?.text.toString())
                    mHandler.post {
                        progressDialog.dismiss()
                        result?.text = data.toString()
                    }
//                val result = module.callAttr("plus", findViewById<EditText>(R.id.etX).text.toString().toInt(),
//                    findViewById<EditText>(R.id.etY).text.toString().toInt())
//                resultTextview.setText(result.toString())
//                val bytes = module.callAttr("plot",
//                                            findViewById<EditText>(R.id.etX).text.toString(),
//                                            findViewById<EditText>(R.id.etY).text.toString())
//                    .toJava(ByteArray::class.java)
//                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//                findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
//
//                currentFocus?.let {
//                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
//                        .hideSoftInputFromWindow(it.windowToken, 0)
//                }
                } catch (e: Throwable) {
                    mHandler.post {
                        result?.text = Log.getStackTraceString(e)
                    }
                }
            }
        }


    }

}