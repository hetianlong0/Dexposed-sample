package com.taobao.dexposed;

import java.io.IOException;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.os.Build;
import android.util.Log;
import android.view.Choreographer;

import com.taobao.android.dexposed.XC_MethodHook;
import com.taobao.android.dexposed.XC_MethodHook.MethodHookParam;
import com.taobao.android.dexposed.XC_MethodHook.Unhook;
import com.taobao.android.dexposed.DexposedBridge;

public class NetWorkHook {

	private static NetWorkHook netWorkHook;

	// connection 开始时间
	public long connectionStartTime;
	public long connectionEndTime;

	// http执行时间
	public long executeStartTime;
	public long executeEndTime;

	// http连接的网络地址
	public String connectionUrl;
	public String executeUrl;

	public String executeMethod;

	// http请求的返回值
	// 200请求成功; 303重定向; 400请求错误; 401未授权; 403禁止访问; 404文件未找到; 500服务器错误
	public int urlRetCode;

	synchronized public static NetWorkHook instance() {
		if (netWorkHook == null)
			netWorkHook = new NetWorkHook();

		return netWorkHook;
	}

	private boolean isEnable() {
		if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 21) {
			return true;
		} else
			return false;
	}

	private NetWorkHook() {

		if (!isEnable())
			return;

	}

	private void hook() {

		Class<?> cls = null;
		try {
			cls = Class
					.forName("org.apache.http.impl.client.AbstractHttpClient");
		} catch (ClassNotFoundException e) {
			Log.w(TAG, "fail to find class AbstractHttpClient");
			e.printStackTrace();
			return;
		}

		XC_MethodHook mehodHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				executeEndTime = System.currentTimeMillis();
				
				super.afterHookedMethod(param);
				
				HttpRequest request = (HttpRequest) param.args[1];
				getUriAndMethod(request);
				
				HttpResponse resp = (HttpResponse) param.getResult();
				if (resp != null) {
					urlRetCode = resp.getStatusLine().getStatusCode();
					Log.d(TAG, "Status Code = "
							+ resp.getStatusLine().getStatusCode());
				}
			}

			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				executeStartTime = System.currentTimeMillis();
				HttpHost host = (HttpHost) param.args[0];
				HttpRequest request = (HttpRequest) param.args[1];
				getUriAndMethod(request);
//				if (request instanceof HttpGet) {
//					HttpGet httpGet = (HttpGet) request;
//					executeUrl = httpGet.getURI().toString();
//					executeMethod = httpGet.getMethod();
//					Log.d(TAG, "HTTP Method : " + httpGet.getMethod());
//					Log.d(TAG, "HTTP URL : " + httpGet.getURI().toString());
//				} else if (request instanceof HttpPost) {
//					HttpPost httpPost = (HttpPost) request;
//					executeUrl = httpPost.getURI().toString();
//					executeMethod = httpPost.getMethod();
//					Log.d(TAG, "HTTP Method : " + httpPost.getMethod());
//					Log.d(TAG, "HTTP URL : " + httpPost.getURI().toString());
//				} else {	//Android-async-http重写了 HttpGet,未进入 HttpGet 和 HttpPost的判读),加入一个else
//					HttpEntityEnclosingRequestBase httpGet = (HttpEntityEnclosingRequestBase) request;
//					HttpEntity entity = httpGet.getEntity();
//					executeUrl = httpGet.getURI().toString();
//					executeMethod = httpGet.getMethod();
//					Log.d(TAG, "HttpRequestBase URL : "
//							+ httpGet.getURI().toString() + " method is: " + executeMethod);
//				}
			}
		};

		onExecute = DexposedBridge
				.findAndHookMethod(cls, "execute", HttpHost.class,
						HttpRequest.class, HttpContext.class, mehodHook);
	}

	private void getUriAndMethod(HttpRequest request) {
		if (request instanceof HttpGet) {
			HttpGet httpGet = (HttpGet) request;
			executeUrl = httpGet.getURI().toString();
			executeMethod = httpGet.getMethod();
			Log.d(TAG, "HTTP Method : " + httpGet.getMethod());
			Log.d(TAG, "HTTP URL : " + httpGet.getURI().toString());
		} else if (request instanceof HttpPost) {
			HttpPost httpPost = (HttpPost) request;
			executeUrl = httpPost.getURI().toString();
			executeMethod = httpPost.getMethod();
			Log.d(TAG, "HTTP Method : " + httpPost.getMethod());
			Log.d(TAG, "HTTP URL : " + httpPost.getURI().toString());
		} else {	//Android-async-http重写了 HttpGet,未进入 HttpGet 和 HttpPost的判读),加入一个else
			HttpEntityEnclosingRequestBase httpGet = (HttpEntityEnclosingRequestBase) request;
			HttpEntity entity = httpGet.getEntity();
			executeUrl = httpGet.getURI().toString();
			executeMethod = httpGet.getMethod();
			Log.d(TAG, "HttpRequestBase URL : "
					+ httpGet.getURI().toString() + " method is: " + executeMethod);
		}
	}
	
	private void unhook() {

		if (onExecute != null) {
			onExecute.unhook();
			onExecute = null;
		}
	}

	private void hookConnection() {

		Class<?> cls = null;
		try {
			cls = Class.forName("java.net.URL");
		} catch (ClassNotFoundException e) {
			Log.w(TAG, "fail to find class java.net.URL");
			e.printStackTrace();
			return;
		}

		XC_MethodHook mehodHookConnection = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				connectionStartTime = System.currentTimeMillis();
				URL url = (URL) param.thisObject;
				Log.d(TAG, "Connect to URL, the URL = " + url.toString()
						+ "connection start time = " + connectionStartTime);
			}

			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				connectionEndTime = System.currentTimeMillis();
				URL url = (URL) param.thisObject;
				Log.d(TAG, "Connect to URL, the URL = " + url.toString()
						+ "connection end time = " + connectionEndTime);
				Log.d(TAG, "connection time is = "
						+ (connectionEndTime - connectionStartTime));
			}
		};

		onConnection = DexposedBridge.findAndHookMethod(cls, "openConnection",
				mehodHookConnection);
	}

	private void unhookConnection() {

		if (onConnection != null) {
			onConnection.unhook();
			onConnection = null;
		}
	}

	private static final String TAG = "Lag";
	private long mFrameIntervalNanos;
	private long mTolerableSkippedNanos;
	private Unhook onExecute;
	private Unhook onConnection;
	private long jitterNanos;
	private boolean isStart = false;

	public void start() {
		if (!isEnable())
			return;

		if (isStart)
			return;

		hookConnection();
		hook();

		isStart = true;
	}

	public void stop() {
		if (!isEnable())
			return;

		if (!isStart)
			return;

		unhookConnection();
		unhook();

		isStart = false;
	}
}
