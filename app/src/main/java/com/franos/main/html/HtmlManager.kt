package com.franos.main.html

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.ObjectUtils.Null
import java.lang.Thread.sleep


@SuppressLint("SetJavaScriptEnabled")
class WebViewHelper(context: Activity) {

    public var webView: WebView = WebView(context)
    private var cur_url: String = ""
    private var context = context

    init {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
//        webView.settings.layoutAlgorithm = true
//        webView.setVisibility(View.VISIBLE);
//        webView.setVisibility(View.INVISIBLE);
        webView.webViewClient = WebViewClient()
    }

    public fun guestureOperation(direction: String){
        when(direction) {
            // TODO: Add forward and backward navigation support in Python's HTML cache to avoid regenerating content each time.
            "Right" -> {
                if (webView.canGoBack()) {
                    webView.goBack() // 向后导航
                }
            }
            "Left" -> {
                if (webView.canGoForward()) {
                    webView.goForward() // 向前导航
                }
            }
            "Up" -> {
                val py = Python.getInstance()
                val module = py.getModule("agent")
                module.callAttr("reduce_html_cur_scroll_idx")
            }
            "Down" -> {
                loadURLAndToFetch(cur_url)
            }
        }
    }

    public fun loadURLAndToFetch(url: String){
        /*
        * The interface to be called by python agent or guesture operation
        * After get the html, will pass it to python agent
        * */
        context.runOnUiThread{
            if (cur_url == url){
                checkScrollAndTodo(webView, url)
            } else{
                webView.loadUrl(url)
                cur_url = url
                Log.d("python", "loadURLAndToFetch")
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        checkRenderAndTodo(view, url)
                    }
                }
            }
        }
    }

    public fun helpfillin(jsscript: String){
        /*
        * The interface to be called by python agent
        * */
        context.runOnUiThread {
            GlobalScope.launch(Dispatchers.Main) {
                webView.evaluateJavascript(jsscript) {
                    Log.d("python", "helpfillin")
                    // todo: tell the user, fill in successfully
                }
            }
        }
    }
    private fun checkRenderAndTodo(view: WebView, url: String){
        GlobalScope.launch(Dispatchers.Main){
            webView.evaluateJavascript("(function() { return document.readyState; })();") { state ->
                Log.d("python", "checkRenderAndTodot ${state}")
                if (state == "\"complete\"") {
                    checkScrollAndTodo(view, url)
                }
            }
        }
    }

    private fun checkScrollAndTodo(view: WebView, url: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Log.d("python", "checkScrollAndTodo")
            // todo: judge whether us documentElement.scrollHeight or body.scrollHeight
            // todo: process news.google.com. THe scrollHeight is always 0 when processing news.google.com
            // todo: if the web is scrollable, currently we scroll one scrollHeight and then fetch the html. We need to decide the
            //  scrolled height each time
            // todo: whether we need to know it can scroll again after this function
            // todo: make sure the scrolling is finished and the new content is rendered
            webView.evaluateJavascript("(function() { return document.documentElement.scrollHeight; })();") { initialHeight ->
                webView.postDelayed({
                    webView.evaluateJavascript("(function() { window.scrollBy(0, document.documentElement.scrollHeight); })();") {
                        Log.d("python", "checkScrollAndTodo after scroll ${initialHeight}")
                        webView.postDelayed({
                            webView.evaluateJavascript("(function() { return document.documentElement.scrollHeight; })();") { finalHeight ->
                                Log.d(
                                    "python",
                                    "checkScrollAndTodo ${finalHeight} ${initialHeight}"
                                )
                                webView.postDelayed({
                                    fetchHtmlContentAndTodo(
                                        view,
                                        url,
                                        initialHeight.toInt(),
                                        finalHeight.toInt()
                                    )
                                }, 500)
                            }
                        }, 500)

                    }
                }, 500)
            }
        }
    }

    private fun fetchHtmlContentAndTodo(view: WebView, url: String, lastScrollHeight: Int, curScrollHeight: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            Log.d("python", "fetchHtmlContentAndTodo")
            webView.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { html ->
                GlobalScope.launch(Dispatchers.IO) {
                    val dhtml = decodeHtml(html)
                    // TODO: Determine whether to manage with Python; ensure thread safety when handling callbacks in Python.
                    val py = Python.getInstance()
                    val module = py.getModule("agent")

                    val pythondict: HashMap<String, Any> = HashMap()
                    pythondict["html"] = dhtml
                    pythondict["url"] = url //url should not be changed after scrolling
                    pythondict["last_scrollheight"] = lastScrollHeight.toString()
                    pythondict["cur_scrollheight"] = curScrollHeight.toString()

                    module.callAttr("finish_get_html", pythondict)
                }
            }
        }
    }



    private fun decodeHtml(encodedHtml: String?): String {
        if (encodedHtml == null) return ""

        var result = encodedHtml
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length - 1)
        }

        result = result.replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\u003C", "<")
            .replace("\\u003E", ">")
            .replace("\\u0026", "&")

        return result
    }


    fun testfetchHtmlContent(url: String) {
        webView.loadUrl(url)
//        Log.d("htmltest", "fetchHtmlContent")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { html ->
                    val dhtml = decodeHtml(html)
                    // TODO: Decide whether to manage this with Python; ensure thread safety in Python during callbacks.
                    val py = Python.getInstance()
                    val module = py.getModule("agent_html_processing")

                    val pythondict: MutableMap<String, String> = HashMap()
                    pythondict["html"] = dhtml
                    pythondict["url"] = url
                    pythondict["scroll_page_idx"] = "0"
                    pythondict["scrollable"] = "false"
                    module.callAttr("finish_get_html", pythondict)
                }
            }
        }
    }


    fun testisScrollableWebPage(url: String, callback: (Boolean) -> Unit) {
        webView.loadUrl(url)
        Log.d("htmltest", "in url ${url}")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("htmltest", "onPageFinished")
                val script = "(function() {" +
                        "    var elements = document.getElementsByTagName('*');" +
                        "    var result = [];" +
                        "    for (var i = 0; i < elements.length; i++) {" +
                        "        var tagName = elements[i].tagName;" +
                        "        var scrollHeight = elements[i].scrollHeight;" +
                        "        if (scrollHeight > 0) {" +
                        "            result.push({tag: tagName, height: scrollHeight});" +
                        "        }" +
                        "    }" +
                        "    return JSON.stringify(result);" +
                        "})();"
                val script2 = "(function() {" +
                        "    var elements = document.getElementsByTagName('*');" +
                        "    var scrollHeights = [];" +
                        "    for (var i = 0; i < elements.length; i++) {" +
                        "        scrollHeights.push(elements[i].scrollHeight);" +
                        "    }" +
                        "    return JSON.stringify(scrollHeights);" +
                        "})();"
                val script3 = """
                            (function() {
                                var documentElement = document.documentElement;
                                var body = document.body;
                
                                return JSON.stringify({
                                    documentClientHeight: documentElement.clientHeight,
                                    documentOffsetHeight: documentElement.offsetHeight,
                                    documentScrollHeight: documentElement.scrollHeight,
                                    bodyClientHeight: body.clientHeight,
                                    bodyOffsetHeight: body.offsetHeight,
                                    bodyScrollHeight: body.scrollHeight
                                });
                            })();
                        """
                val script4 = """
                    (function() {
                        var element = document.documentElement;
                        var style = window.getComputedStyle(element);
                        return JSON.stringify({
                            display: style.display,
                            visibility: style.visibility,
                            opacity: style.opacity
                        });
                    })();
                """
//                webView.evaluateJavascript(script) { initialHeight ->
//                    Log.d("htmltest", "first scrollHeight ${initialHeight}")
//                    }
//                webView.evaluateJavascript(script2) { initialHeight ->
//                    Log.d("htmltest", "first scrollHeight ${initialHeight}")
//                }

                sleep(10000)

                Log.d("htmltest", "-1 ${url}")
                webView.evaluateJavascript("(function() { window.scrollBy(0, 10000); })();"){

                }
                webView.evaluateJavascript(
                    "(function() { window.scrollTo(0, 100000); })();"
                ){}


                webView.evaluateJavascript("(function() { return document.readyState; })();") { initialHeight ->
                    Log.d("htmltest", "1 scrollHeight ${initialHeight}")
                }
                webView.evaluateJavascript(script3) { initialHeight ->
                    Log.d("htmltest", "2 scrollHeight ${initialHeight}")
                }
                webView.evaluateJavascript(script4) { initialHeight ->
                    Log.d("htmltest", "3 scrollHeight ${initialHeight}")
                }
                // todo: judge whether us documentElement.scrollHeight or body.scrollHeight
//                webView.evaluateJavascript("(function() { return document.documentElement.scrollHeight; })();") { initialHeight ->
//                    webView.evaluateJavascript("(function() { window.scrollBy(0, document.documentElement.scrollHeight); })();") {
//                        webView.evaluateJavascript("(function() { return document.documentElement.scrollHeight; })();") { finalHeight ->
//                            Log.d("htmltest", "javascript ${finalHeight} ${initialHeight}")
//                            val isScrollable = finalHeight.toInt() > initialHeight.toInt()
//                            callback(isScrollable)
//                        }
//                    }
//                }
                webView.evaluateJavascript("(function() { return document.documentElement.scrollHeight; })();") { initialHeight ->
                    webView.evaluateJavascript("(function() { window.scrollBy(0, document.documentElement.scrollHeight); })();") {
                        webView.postDelayed({
                            webView.evaluateJavascript("(function() { return document.documentElement.scrollHeight; })();") { finalHeight ->
                                Log.d("htmltest", "checkScrollAndTodo ${finalHeight} ${initialHeight}")
                                val isScrollable = finalHeight.toInt() > initialHeight.toInt()
                                callback(isScrollable)
                            }
                        }, 500)

                    }
                }
            }
        }
    }
}
