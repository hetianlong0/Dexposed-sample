package com.taobao.dexposed;

import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.os.Build;
import android.util.Log;
import android.view.Choreographer;

import com.taobao.android.dexposed.XC_MethodHook;
import com.taobao.android.dexposed.XC_MethodHook.MethodHookParam;
import com.taobao.android.dexposed.XC_MethodHook.Unhook;
import com.taobao.android.dexposed.DexposedBridge;


public class NetWorkHook {


    private static NetWorkHook netWorkHook;
    
    //connection 开始时间
    private long connectionStartTime;
    private long connectionEndTime;
    
    //http执行时间
    private long executeStartTime;
    private long executeEndTime;
    
    //http连接的网络地址
    private String  connectionUrl;
    private String  executeUrl;
    
    //http请求的返回值
    //200请求成功; 303重定向; 400请求错误; 401未授权; 403禁止访问; 404文件未找到; 500服务器错误 
    private int urlRetCode;

    synchronized public static NetWorkHook instance(){
        if(netWorkHook == null)
        	netWorkHook = new NetWorkHook();

        return netWorkHook;
    }

	private boolean isEnable(){
		if(Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 21) {
			return true;
		} else 
			return false;
	}
	
	private NetWorkHook() {

		if(!isEnable())
			return;
		
	}
	
	private void hook() {
				
		Class<?> cls = null;
		try {
			cls = Class.forName("org.apache.http.impl.client.AbstractHttpClient");
		} catch (ClassNotFoundException e) {
			Log.w(TAG, "fail to find class AbstractHttpClient");
			e.printStackTrace();
			return;
		}

		XC_MethodHook mehodHook =  new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				executeEndTime = System.currentTimeMillis();
				// TODO Auto-generated method stub
				super.afterHookedMethod(param);
				HttpResponse resp = (HttpResponse) param.getResult();
				if (resp != null) {
					int strRet = resp.getStatusLine().getStatusCode();
					Log.d(TAG, "Status Code = " + resp.getStatusLine().getStatusCode());
					Header[] headers = resp.getAllHeaders();
					if (headers != null) {
						for (int i = 0; i < headers.length; i++) {
							Log.d(TAG, headers[i].getName() + ":" + headers[i].getValue());
						}
					}
				}
			}
			
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				executeStartTime = System.currentTimeMillis();
				HttpHost host = (HttpHost)param.args[0];
				HttpRequest request = (HttpRequest) param.args[1];
				if (request instanceof HttpGet) {
					HttpGet httpGet = (HttpGet) request;
					Log.d(TAG, "HTTP Method : " + httpGet.getMethod());
					Log.d(TAG, "HTTP URL : " + httpGet.getURI().toString());
					Header[] headers = request.getAllHeaders();
					if (headers != null) {
						for (int i = 0; i < headers.length; i++) {
							Log.d(TAG, headers[i].getName() + ":" + headers[i].getName());
						}
					}
				} else if (request instanceof HttpPost) {
					HttpPost httpPost = (HttpPost) request;
					Log.d(TAG, "HTTP Method : " + httpPost.getMethod());
					Log.d(TAG, "HTTP URL : " + httpPost.getURI().toString());
					Header[] headers = request.getAllHeaders();
					if (headers != null) {
						for (int i = 0; i < headers.length; i++) {
							Log.d(TAG, headers[i].getName() + ":" + headers[i].getValue());
						}
					}
					HttpEntity entity = httpPost.getEntity();
					String contentType = null;
					if (entity.getContentType() != null) {
						contentType = entity.getContentType().getValue();
						if (URLEncodedUtils.CONTENT_TYPE.equals(contentType)) {
							try {
								byte[] data = new byte[(int) entity.getContentLength()];
								entity.getContent().read(data);
								String content = new String(data, HTTP.DEFAULT_CONTENT_CHARSET);
								Log.d(TAG, "HTTP POST Content : " + content);
							} catch (IllegalStateException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						} else if (contentType.startsWith(HTTP.DEFAULT_CONTENT_TYPE)) {
							try {
								byte[] data = new byte[(int) entity.getContentLength()];
								entity.getContent().read(data);
								String content = new String(data, contentType.substring(contentType.lastIndexOf("=") + 1));
								Log.d(TAG, "HTTP POST Content : " + content);
							} catch (IllegalStateException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					}else{
						byte[] data = new byte[(int) entity.getContentLength()];
						try {
							entity.getContent().read(data);
							String content = new String(data, HTTP.DEFAULT_CONTENT_CHARSET);
							Log.d(TAG, "HTTP POST Content : " + content);
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		};
		
		onExecute = DexposedBridge.findAndHookMethod(cls, "execute", HttpHost.class, HttpRequest.class, 
				 HttpContext.class, mehodHook);
	}
	
	private void unhook() {

		if(onExecute != null) {
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

		XC_MethodHook mehodHookConnection =  new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				connectionStartTime = System.currentTimeMillis();
				URL url = (URL) param.thisObject;
				Log.d(TAG, "Connect to URL, the URL = " + url.toString() + "connection start time = " + connectionStartTime);
			}
			
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				connectionEndTime = System.currentTimeMillis();
				URL url = (URL) param.thisObject;
				Log.d(TAG, "Connect to URL, the URL = " + url.toString() + "connection end time = " + connectionEndTime);
				Log.d(TAG, "connection time is = " + (connectionEndTime - connectionStartTime));
			}
		};
		
		onConnection = DexposedBridge.findAndHookMethod(cls, "openConnection", mehodHookConnection);
	}
	
	private void unhookConnection() {

		if(onConnection != null) {
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
		if(!isEnable()) return;

        if(isStart) return;

        hookConnection();
		hook();

        isStart = true;
	}

	public void stop() {
		if(!isEnable()) return;

        if(!isStart) return;

        unhookConnection();
        unhook();
        

        isStart = false;
	}
}
