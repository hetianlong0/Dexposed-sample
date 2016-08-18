package com.taobao.dexposed;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.taobao.android.dexposed.XC_MethodHook;
import com.taobao.android.dexposed.DexposedBridge;
import com.taobao.patch.PatchMain;
import com.taobao.patch.PatchResult;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity {

	public static final int SHOW_RESPONSE = 0;
	//新建Handler的对象，在这里接收Message，然后更新TextView控件的内容
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case SHOW_RESPONSE:
                String response = (String) msg.obj;
    //            textView_response.setText(response);
    //            Log.d("TAG, "aaa ");
               int a = 0;
            default:
                break;
            }            
        }

    };
	static {
		// load xposed lib for hook.
		try {
			if (android.os.Build.VERSION.SDK_INT == 22){
				System.loadLibrary("dexposed_l51");
			} else if (android.os.Build.VERSION.SDK_INT > 19 && android.os.Build.VERSION.SDK_INT <= 21){
				System.loadLibrary("dexposed_l");
			} else if (android.os.Build.VERSION.SDK_INT > 14){
				System.loadLibrary("dexposed");
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private boolean isSupport = false;
    private boolean isLDevice= false;
	
	private TextView mLogContent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mLogContent = (TextView) (this.findViewById(R.id.log_content));
		// check device if support and auto load libs
        isSupport = true;
		isLDevice = android.os.Build.VERSION.SDK_INT >= 20;
		
		
	}

	//Hook system log click
	public void hookSystemLog(View view) {
		if (isSupport) {
			DexposedBridge.findAndHookMethod(isLDevice ? this.getClass(): Log.class, isLDevice ? "showLog" : "d", String.class, String.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam arg0) throws Throwable {
					String tag = (String) arg0.args[0];
					String msg = (String) arg0.args[1];
					mLogContent.setText(tag + "," + msg);
				}
			});
            if (isLDevice) {
                showLog("dexposed", "It doesn't support AOP to system method on ART devices");
            } else {
                Log.d("dexposed", "Logs are redirected to display here");
            }
		} else {
			mLogContent.setText("This device doesn't support dexposed!");
		}
	}

    private void showLog(String tag, String msg) {
        Log.d(tag, msg);
    }
	
	// Hook choreographer click
	public void hookChoreographer(View view) {
		Log.d("dexposed", "hookChoreographer button clicked.");
		if (isSupport && !isLDevice) {
			ChoreographerHook.instance().start();
		} else {
			showLog("dexposed", "This device doesn't support this!");
		}
	}
	
	// Run patch apk
	public void runPatchApk(View view) {
		Log.d("dexposed", "runPatchApk button clicked.");
        if (isLDevice) {
            showLog("dexposed", "It doesn't support this function on L device.");
            return;
        }
        if (!isSupport) {
			Log.d("dexposed", "This device doesn't support dexposed!");
			return;
		}
		File cacheDir = getExternalCacheDir();
    	if(cacheDir != null){
    		String fullpath = cacheDir.getAbsolutePath() + File.separator + "PatchDemo.apk";
    		PatchResult result = PatchMain.load(this, fullpath, null);
    		if (result.isSuccess()) {
    			Log.e("Hotpatch", "patch success!");
    		} else {
    			Log.e("Hotpatch", "patch error is " + result.getErrorInfo());
    		}
    	}
    	showDialog();
	}
	
	public void hookHttpClient(View view) {
		NetWorkHook.instance().start();
		Log.d("hook_http_client", "runPatchApk button clicked.");
		  new Thread(new Runnable() {
	            @Override
	            public void run() {
	                //用HttpClient发送请求，分为五步
	                //第一步：创建HttpClient对象
	                HttpClient httpCient = new DefaultHttpClient();
	                //第二步：创建代表请求的对象,参数是访问的服务器地址
	                HttpGet httpGet = new HttpGet("http://www.baidu.com");
	                
	                try {
	                    //第三步：执行请求，获取服务器发还的相应对象
	                    HttpResponse httpResponse = httpCient.execute(httpGet);
	                    //第四步：检查相应的状态是否正常：检查状态码的值是200表示正常
	                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
	                        //第五步：从相应对象当中取出数据，放到entity当中
	                        HttpEntity entity = httpResponse.getEntity();
	                        String response = EntityUtils.toString(entity,"utf-8");//将entity当中的数据转换为字符串
	                        
	                        //在子线程中将Message对象发出去
	                        Message message = new Message();
	                        message.what = SHOW_RESPONSE;
	                        message.obj = response.toString();
	                        handler.sendMessage(message);
	                    }
	                } catch (Exception e) {
	                    // TODO Auto-generated catch block
	                    e.printStackTrace();
	                }
	            }
	        }).start();//这个start()方法不要忘记了           
	}
	
	public void sendHttpCommand(View view) {
		NetWorkHook.instance().stop();
		new Thread() {
			@Override
			public void run() {
				String url = "http://192.168.1.49:8081/sdk/mobile/device";
				try {
					JSONObject object = new JSONObject();
					object.put("url", NetWorkHook.instance().executeUrl);
					object.put("method", NetWorkHook.instance().executeMethod);
					object.put("startTime", NetWorkHook.instance().executeStartTime);
					object.put("endTime", NetWorkHook.instance().executeEndTime);
					object.put("ret code", NetWorkHook.instance().urlRetCode);
					submitPostJSONData(url, object);
				} catch (Exception e) {
				}
			}
		}.start();
	}
	
	public static String submitPostJSONData(String strUrlPath,
			JSONObject param) {
		String returnLine = "";
		byte[] data = param.toString().getBytes();
		try {
			
			URL url = new URL(strUrlPath);

			HttpURLConnection httpURLConnection = (HttpURLConnection) url
					.openConnection();
			httpURLConnection.setConnectTimeout(3000);
			httpURLConnection.setDoInput(true);
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setUseCaches(false);
			
			httpURLConnection.setRequestProperty("Content-Type", "application/json");
			
			httpURLConnection.connect();
			
			DataOutputStream outputStream = new DataOutputStream(httpURLConnection
				     .getOutputStream());
						   
			outputStream.write(data, 0, data.length);
			outputStream.flush();
			outputStream.close(); // flush and close

			int response = httpURLConnection.getResponseCode();
			if (response == HttpURLConnection.HTTP_OK) {
				InputStream inptStream = httpURLConnection.getInputStream();
				String result = dealResponseResult(inptStream);
				
				try {
					JSONObject ret = new JSONObject(result);
					int code = ret.optInt("result");
					
					if( code == 1) {
						return "1";
					}
					return "0";
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
			return "err: " + e.getMessage().toString();
		}
		return "-1";
	}
	
	private void showDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Dexposed sample")
				.setMessage(
						"Please clone patchsample project to generate apk, and copy it to \"/Android/data/com.taobao.dexposed/cache/patch.apk\"")
				.setPositiveButton("ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				}).create().show();
	}
	
	public static String dealResponseResult(InputStream inputStream) {
		String resultData = null;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] data = new byte[1024*16];
		int len = 0;
		try {
			while ((len = inputStream.read(data)) != -1) {
				byteArrayOutputStream.write(data, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		resultData = new String(byteArrayOutputStream.toByteArray());
		return resultData;
	}
}