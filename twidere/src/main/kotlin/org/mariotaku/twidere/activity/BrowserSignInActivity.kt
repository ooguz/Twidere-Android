/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_browser_sign_in.*
import org.attoparser.ParseException
import org.mariotaku.ktextension.removeAllCookiesSupport
import org.mariotaku.restfu.oauth.OAuthToken
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.*
import org.mariotaku.twidere.util.OAuthPasswordAuthenticator
import org.mariotaku.twidere.util.webkit.DefaultWebViewClient
import java.io.IOException
import java.io.StringReader

class BrowserSignInActivity : BaseActivity() {

    private var requestToken: OAuthToken? = null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_sign_in)
        CookieManager.getInstance().removeAllCookiesSupport()
        webView.setWebChromeClient(AuthorizationWebChromeClient(this))
        webView.setWebViewClient(AuthorizationWebViewClient(this))
        webView.isVerticalScrollBarEnabled = false
        webView.addJavascriptInterface(InjectorJavaScriptInterface(this), "injector")
        val webSettings = webView.settings
        webSettings.loadsImagesAutomatically = true
        webSettings.javaScriptEnabled = true
        webSettings.blockNetworkImage = false
        webSettings.saveFormData = true

        webView.loadUrl(intent.dataString)
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    private fun readOAuthPin(html: String): String? {
        try {
            val data = OAuthPasswordAuthenticator.OAuthPinData()
            OAuthPasswordAuthenticator.readOAuthPINFromHtml(StringReader(html), data)
            return data.oauthPin
        } catch (e: ParseException) {
            Log.w(LOGTAG, e)
        } catch (e: IOException) {
            Log.w(LOGTAG, e)
        }

        return null
    }

    private fun setLoadProgressShown(shown: Boolean) {
        progressContainer.visibility = if (shown) View.VISIBLE else View.GONE
    }

    private fun setLoadProgress(progress: Int) {
        loadProgress.progress = progress
    }

    internal class AuthorizationWebChromeClient(val activity: BrowserSignInActivity) : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            activity.setLoadProgress(newProgress)
        }
    }

    internal class AuthorizationWebViewClient(activity: BrowserSignInActivity) : DefaultWebViewClient<BrowserSignInActivity>(activity) {

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            activity.setLoadProgressShown(true)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            view.loadUrl(INJECT_CONTENT)
            activity.setLoadProgressShown(false)
            val uri = Uri.parse(url)
            // Hack for fanfou
            if ("fanfou.com" == uri.host) {
                val path = uri.path
                val paramNames = uri.queryParameterNames
                if ("/oauth/authorize" == path && paramNames.contains("oauth_callback")) {
                    // Sign in successful response.
                    val intent = activity.intent
                    val data = Intent()
                    data.putExtra(EXTRA_EXTRAS, intent.getBundleExtra(EXTRA_EXTRAS))
                    activity.setResult(Activity.RESULT_OK, data)
                    activity.finish()
                }
            }
        }

        @Suppress("Deprecation", "OverridingDeprecatedMember")
        override fun onReceivedError(view: WebView, errorCode: Int, description: String?,
                failingUrl: String?) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            val activity = activity
            Toast.makeText(activity, description, Toast.LENGTH_SHORT).show()
            activity.finish()
        }

        @Suppress("Deprecation", "OverridingDeprecatedMember")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            val uri = Uri.parse(url)
            val data = Intent()
            data.putExtra(EXTRA_EXTRAS, activity.intent.getBundleExtra(EXTRA_EXTRAS))
            if (url.startsWith(OAUTH_CALLBACK_URL)) {
                val oauthVerifier = uri.getQueryParameter("oauth_verifier") ?: return false
                data.putExtra(EXTRA_OAUTH_VERIFIER, oauthVerifier)
            } else if (url.startsWith(MASTODON_CALLBACK_URL)) {
                val code = uri.getQueryParameter("code") ?: return false
                data.putExtra(EXTRA_CODE, code)
            } else {
                return false
            }
            activity.setResult(Activity.RESULT_OK, data)
            activity.finish()
            return true
        }

    }

    internal class InjectorJavaScriptInterface(private val activity: BrowserSignInActivity) {

        @JavascriptInterface
        fun processHTML(html: String) {
            val oauthVerifier = activity.readOAuthPin(html)
            if (oauthVerifier != null) {
                val intent = activity.intent
                val data = Intent()
                data.putExtra(EXTRA_OAUTH_VERIFIER, oauthVerifier)
                data.putExtra(EXTRA_EXTRAS, intent.getBundleExtra(EXTRA_EXTRAS))
                activity.setResult(Activity.RESULT_OK, data)
                activity.finish()
            }
        }
    }

    companion object {

        private val INJECT_CONTENT = "javascript:window.injector.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');"
    }
}
