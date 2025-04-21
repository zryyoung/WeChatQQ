package org.voice.replace;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.voice.replace.MainActivity;
import org.voice.replace.util.FileVoiceReplacer;
import org.voice.replace.util.TTSVoiceReplacer;

public class MainActivity extends Activity {
	
    private EditText editQQ, editWeChatFile;
    private Spinner spinnerTTS;
    private Button btnAutoFill;
    private ImageButton btnSave, btnStart;
    private Button btnTest; // 试听按钮
	private EditText inputText;

    // 可选的微软 TTS 发音人
    private String[] voices = {
		"zh-CN-XiaoxiaoNeural",
		"zh-CN-XiaoyiNeural",
		"zh-CN-YunjianNeural",
		"zh-CN-YunxiNeural",
		"zh-CN-YunxiaNeural",
		"zh-CN-YunyangNeural",
		"zh-CN-liaoning-XiaobeiNeural",
		"zh-CN-shaanxi-XiaoniNeural",
		"zh-HK-HiuGaaiNeural",
		"zh-HK-HiuMaanNeural",
		"zh-HK-WanLungNeural",
		"zh-TW-HsiaoChenNeural",
		"zh-TW-HsiaoYuNeural",
		"zh-TW-YunJheNeural"
    };

	private static final String TERMUX_PREFIX = "/data/data/com.termux/files/usr";
    private static final String EDGE_TTS_BIN = TERMUX_PREFIX + "/bin/edge-tts";
    private final String MP3_FILE = "/sdcard/Download/tts.mp3"; // 定义临时文件路径
    private MediaPlayer mediaPlayer;

	private WindowManager windowManager;
    private LinearLayout floatingBall;
	private LinearLayout expandedLayout; // 扩展部分
    
	private TTSVoiceReplacer ttsVoiceReplacer;
	private FileVoiceReplacer fileVoiceReplacer;


	private void init() {
		checkNotificationPermission();
		requestStoragePermission();
	}
	private void checkNotificationPermission() {
		if (!hasNotificationPermission()) {
			openNotificationSettings(MainActivity.this);
			//Toast.makeText(this, "请授予通知权限", Toast.LENGTH_SHORT).show();
		}
	}

// 检查是否有通知权限
	private boolean hasNotificationPermission() {
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		return manager.areNotificationsEnabled();
	}

// 跳转通知权限设置页
	private void openNotificationSettings(Context context) {
		Intent intent = new Intent();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
			intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
		} else {
			intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
			intent.setData(Uri.parse("package:" + getPackageName()));
		}
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private void requestStoragePermission() {
		if (Build.VERSION.SDK_INT >= 23) {
			int readPermission = checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE");
			int writePermission = checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");

			if (readPermission != PackageManager.PERMISSION_GRANTED ||
				writePermission != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(
					new String[] {
						"android.permission.READ_EXTERNAL_STORAGE",
						"android.permission.WRITE_EXTERNAL_STORAGE",
						"android.settings.APPLICATION_DETAILS_SETTINGS"
					},
					1002
				);
			}		
		}
	}

	public static boolean canDrawOverlays(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.canDrawOverlays(context);
		} else {
			return true; // Android 6 以下默认有权限
		}
	}


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		init();

        // 创建主容器
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

//        // 添加标题
//        TextView title = new TextView(this);
//        title.setText("微信QQ自定义语音");
//        title.setTextSize(20);
//        title.setPadding(0, 0, 0, 30);
//        layout.addView(title);

        // 添加环境准备文本
        addText(layout, "一、环境准备");
        addText(layout, "1. 下载 ZeroTermux:\nhttp://getzt.icdown.club/");
        addText(layout, "2. 安装依赖:\npkg update -y && pkg upgrade -y\npkg install python3 ffmpeg -y\npip install pilk edge-tts");
        addText(layout, "3. 安装完后即可使用本应用。");

        // 添加参数配置文本
        addText(layout, "\n二、参数配置");

        // 添加微软 TTS 发音人下拉选择框和试听按钮
        addText(layout, "微软 TTS 发音人");

        LinearLayout ttsLayout = new LinearLayout(this);
        ttsLayout.setOrientation(LinearLayout.HORIZONTAL);
        ttsLayout.setGravity(Gravity.START);

        spinnerTTS = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, voices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTTS.setAdapter(adapter);
        ttsLayout.addView(spinnerTTS);

        btnTest = new Button(this);
        btnTest.setText("试听");
        btnTest.setLayoutParams(new LinearLayout.LayoutParams(
									LinearLayout.LayoutParams.WRAP_CONTENT,
									LinearLayout.LayoutParams.WRAP_CONTENT
								));
        btnTest.setPadding(10, 5, 10, 5); // 适当调整按钮内边距
        ttsLayout.addView(btnTest);

        layout.addView(ttsLayout);

		layout.setFocusable(true);
		layout.setFocusableInTouchMode(true);
		layout.requestFocus();
        // 添加 QQ 号配置输入
        addText(layout, "QQ 号配置");
        editQQ = new EditText(this);
        //editQQ.setText("QQ号");
        layout.addView(editQQ);

        // 添加微信指定文件夹输入
        addText(layout, "微信指定文件夹");
        editWeChatFile = new EditText(this);
        //editWeChatFile.setText("微信文件夹id");
        layout.addView(editWeChatFile);

		// 从 SharedPreferences 中读取保存的配置
		SharedPreferences sharedPreferences = getSharedPreferences("VoiceConfig", MODE_PRIVATE);
		String savedVoice = sharedPreferences.getString("selectedVoice", "zh-CN-XiaoxiaoNeural");
		String savedQQ = sharedPreferences.getString("qqNumber", "");
		String savedWxFolder = sharedPreferences.getString("wxFolder", "");

// 设置语音选择的索引
		int voiceIndex = adapter.getPosition(savedVoice);
		if (voiceIndex >= 0) {
			spinnerTTS.setSelection(voiceIndex);
		}

// 设置 QQ 和微信文件夹输入
		editQQ.setText(savedQQ);
		editWeChatFile.setText(savedWxFolder);

        // 提示文本
        addText(layout, "\n提示：QQ、微信录制语音不要发送，点击自动填入，授权ROOT...");

        // 自动填入按钮
        btnAutoFill = new Button(this);
        btnAutoFill.setText("自动填入");
        layout.addView(btnAutoFill);

        // 按钮布局
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.RIGHT);

        // 保存按钮
        btnSave = new ImageButton(this);
        btnSave.setImageResource(android.R.drawable.ic_menu_save);
        btnLayout.addView(btnSave);

        // 启动按钮
        btnStart = new ImageButton(this);
        btnStart.setImageResource(android.R.drawable.ic_media_play);
        btnLayout.addView(btnStart);

        layout.addView(btnLayout);

        scrollView.addView(layout);
        setContentView(scrollView);

        // 点击事件监听
		btnAutoFill.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// 自动填入逻辑
					String latestQQNum = getLatestQQFolder("/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ");
					String latestFolder = getLatestFolder("/data/data/com.tencent.mm/MicroMsg/");
					//showToast(latestFolder);
					if (latestFolder != null && latestQQNum != null) {
						editWeChatFile.setText(latestFolder);
						editQQ.setText(latestQQNum);
						//Toast.makeText(MainActivity.this, "自动填入完成", Toast.LENGTH_SHORT).show();
						saveConfig();
					} else {
						Toast.makeText(MainActivity.this, "没有找到文件夹", Toast.LENGTH_SHORT).show();
					}
				}
			});

        btnSave.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// 保存配置逻辑
					//Toast.makeText(MainActivity.this, "保存配置成功", Toast.LENGTH_SHORT).show();
					saveConfig();
				}
			});

        btnStart.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// 开始执行逻辑
					if (!canDrawOverlays(MainActivity.this)) {
						Toast.makeText(MainActivity.this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
						Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						return;
					}
					//Toast.makeText(MainActivity.this, "开始执行替换操作", Toast.LENGTH_SHORT).show();
					// 这里可以添加调用 root 命令或写文件等逻辑
					startFloatingBall();
				}
			});

        // 试听按钮的点击事件
        btnTest.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String selectedVoice = spinnerTTS.getSelectedItem().toString();
					//Toast.makeText(MainActivity.this, "试听发音人: " + selectedVoice, Toast.LENGTH_SHORT).show();
					// 这里可以调用 TTS 播放逻辑
					//String selectedVoice = spinnerTTS.getSelectedItem().toString();
					playSampleAudio(selectedVoice);  // 直接调用播放逻辑

				}
			});
    }

    private void addText(LinearLayout layout, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setPadding(0, 10, 0, 10);
        layout.addView(tv);
    }
	// 播放示例音频
	private void playSampleAudio(String voice) {
		final String TEXT = "你好，这是一个语音合成试例"; 
		final String cmd = EDGE_TTS_BIN + " --text \"" + TEXT + "\" --voice \"" + voice + "\" --write-media=\"" + MP3_FILE + "\"";

		new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
						process.waitFor();

						// 播放音频
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									playAudio();
								}
							});
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
						showToast("合成失败：" + e.getMessage());
					}
				}
			}).start();
	}

	private void playAudio() {
		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDataSource(MP3_FILE);
			mediaPlayer.prepare();
			mediaPlayer.start();

			// 播放完后释放资源
			mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						mp.release();
						mediaPlayer = null;
					}
				});
			showToast("✅ 合成成功！正在播放...");
		} catch (IOException e) {
			e.printStackTrace();
			showToast("❌ 播放失败：" + e.getMessage());
		}
	}

	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}


// 保存配置的方法
	private void saveConfig() {
		String selectedVoice = spinnerTTS.getSelectedItem().toString(); // 获取用户选择的发音人
		String qqNumber = editQQ.getText().toString(); // 获取用户输入的 QQ 号
		String wxFolder = editWeChatFile.getText().toString(); // 获取用户输入的微信文件夹名

		// 示例保存逻辑：这里可以将配置信息保存到 SharedPreferences 或其他存储方式
		SharedPreferences sharedPreferences = getSharedPreferences("VoiceConfig", MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("selectedVoice", selectedVoice);
		editor.putString("qqNumber", qqNumber);
		editor.putString("wxFolder", wxFolder);
		editor.apply(); // 提交保存

		Toast.makeText(MainActivity.this, "配置保存成功", Toast.LENGTH_SHORT).show();
	}
	// 获取最新的微信文件夹
	private String getLatestFolder(String path) {
		String cmd = "cd " + path + " && ls -lt -d */ | grep -Eo \"[0-9a-f]{32}/$\" | head -n 1";
		String result = executeCommand(cmd);
		if (result != null && !result.trim().isEmpty()) {
			//showToast(result);
			return result.trim().replace("/", ""); // 获取文件夹名
		} else {
			return null;
		}
	}

// 获取最新的 QQ 号文件夹
	private String getLatestQQFolder(String baseDir) {
		String cmd = "cd " + baseDir + " && ls -d */ | grep -Eo \"[0-9]+\" | head -n 1";
		String result = executeCommand(cmd);
		//showToast(result);
		return (result != null && !result.trim().isEmpty()) ? result.trim() : null;
	}

// 执行 shell 命令
	private String executeCommand(String command) {
		try {
			Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
			reader.close();
			process.waitFor();
			return output.toString();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}



	private void startFloatingBall() {
		if (floatingBall != null) {
			// 如果悬浮窗已经存在，不再创建
			showToast("已开启");
			return;
		}
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		floatingBall = new LinearLayout(this);  
		floatingBall.setOrientation(LinearLayout.VERTICAL);  
		floatingBall.setBackgroundColor(0x80000000); // 半透明黑  

		// 展开布局（包含所有按钮）  
		expandedLayout = new LinearLayout(this);  
		expandedLayout.setOrientation(LinearLayout.VERTICAL);  
		expandedLayout.setVisibility(View.VISIBLE);  


		// 当前模式按钮（点击切换）  
		final Button modeButton = new Button(this);  
		modeButton.setText("模式：QQ");  
		modeButton.setTextSize(14);  
		modeButton.setBackgroundColor(Color.TRANSPARENT); // 背景透明  
		modeButton.setTextColor(0xFFFFFFFF);  
		modeButton.setPadding(10, 10, 10, 10);  
		final boolean[] isQQ = {true};  
		modeButton.setOnClickListener(new View.OnClickListener() {  
				@Override  
				public void onClick(View v) {  
					isQQ[0] = !isQQ[0];  
					modeButton.setText(isQQ[0] ? "模式：QQ" : "模式：微信");  
				}  
			});  

		// TTS 按钮  
		Button ttsButton = new Button(this);  
		ttsButton.setText("微软 TTS");  
		ttsButton.setTextSize(13);  
		ttsButton.setBackgroundColor(Color.TRANSPARENT);  
		ttsButton.setTextColor(0xFFFFFFFF);  
		ttsButton.setPadding(10, 10, 10, 10);  
		ttsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showTextInputFloating(new TextInputListener() {
							@Override
							public void onTextEntered(String text) {
								// 这里处理输入完成后的内容
								// Toast.makeText(MainActivity.this, "你输入了：" + text, Toast.LENGTH_SHORT).show();
								// final String text = "这是一段语音合成实例。";

								if (text.isEmpty()) {
									Toast.makeText(MainActivity.this, "❌ 文本不能为空", Toast.LENGTH_SHORT).show();
									return;
								}

								SharedPreferences sharedPreferences = getSharedPreferences("VoiceConfig", MODE_PRIVATE);

								final String selectedVoice = sharedPreferences.getString("selectedVoice", "zh-CN-XiaoxiaoNeural");
								final String qqNumber = sharedPreferences.getString("qqNumber", "");
								final String wxFolder = sharedPreferences.getString("wxFolder", "");

								final boolean isQQMode = isQQ[0];

								//Toast.makeText(MainActivity.this, isQQMode ? "QQ TTS" : "微信 TTS", Toast.LENGTH_SHORT).show();

								ttsVoiceReplacer = new TTSVoiceReplacer(MainActivity.this);
								ttsVoiceReplacer.replace(text, isQQMode, selectedVoice, qqNumber, wxFolder);
//								new Thread(new Runnable() {
//										@Override
//										public void run() {
//											new TTSVoiceReplacer(MainActivity.this).replace(text, isQQMode, selectedVoice, qqNumber, wxFolder);
//										}
//									}).start();
							}
						});
					
				}
			});

		// 文件按钮  
		Button fileButton = new Button(this);  
		fileButton.setText("本地音频");  
		fileButton.setTextSize(13);  
		fileButton.setBackgroundColor(Color.TRANSPARENT);  
		fileButton.setTextColor(0xFFFFFFFF);  
		fileButton.setPadding(10, 10, 10, 10);  
		fileButton.setOnClickListener(new View.OnClickListener() {  
				@Override  
				public void onClick(View v) {  
					SharedPreferences sharedPreferences = getSharedPreferences("VoiceConfig", MODE_PRIVATE);

					//final String selectedVoice = sharedPreferences.getString("selectedVoice", "zh-CN-XiaoxiaoNeural");
					final String qqNumber = sharedPreferences.getString("qqNumber", "");
					final String wxFolder = sharedPreferences.getString("wxFolder", "");

					final boolean isQQMode = isQQ[0];
					//Toast.makeText(MainActivity.this, isQQ[0] ? "QQ 文件" : "微信 文件", Toast.LENGTH_SHORT).show();  
					fileVoiceReplacer = new FileVoiceReplacer(MainActivity.this);
					fileVoiceReplacer.startReplace(isQQMode, qqNumber, wxFolder);
				}  
			});  

//		// 最小化按钮  
//		Button minimizeButton = new Button(this);  
//		minimizeButton.setText("最小化");  
//		minimizeButton.setTextSize(12);  
//		minimizeButton.setBackgroundColor(Color.TRANSPARENT);  
//		minimizeButton.setTextColor(0xFFFFFFFF);  
//		minimizeButton.setPadding(10, 10, 10, 10);  
//		minimizeButton.setOnClickListener(new View.OnClickListener() {  
//				@Override  
//				public void onClick(View v) {  
//					floatingBall.removeAllViews();  
//					ImageView icon = new ImageView(MainActivity.this);  
//					//icon.setImageResource(android.R.drawable.ic_media_play); // 最小化图标  
//					icon.setImageResource(android.R.drawable.ic_btn_speak_now);  
//					icon.setColorFilter(Color.WHITE); // 相当于 XML 中的 tint="#ffffff"  
//					icon.setPadding(10, 10, 10, 10);  
//					floatingBall.addView(icon);  
//
//					icon.setOnClickListener(new View.OnClickListener() {  
//							@Override  
//							public void onClick(View v) {  
//								floatingBall.removeAllViews();  
//								floatingBall.addView(expandedLayout);  
//							}  
//						});  
//				}  
//			});  
//
//		// 关闭按钮  
//		Button closeButton = new Button(this);  
//		closeButton.setText("关闭");  
//		closeButton.setTextSize(12);  
//		closeButton.setBackgroundColor(Color.TRANSPARENT);  
//		closeButton.setTextColor(0xFFFFFFFF);  
//		closeButton.setPadding(10, 10, 10, 10);  
//		closeButton.setOnClickListener(new View.OnClickListener() {  
//				@Override  
//				public void onClick(View v) {  
//					if (floatingBall.getWindowToken() != null) {  
//						windowManager.removeView(floatingBall);  
//						floatingBall = null;
//					}  
//				}  
//			});  
//		TextView title = new TextView(this);
//        title.setText("按我移动");
//        title.setTextSize(14);
//        title.setPadding(0, 0, 0, 30);
//        expandedLayout.addView(title);
		LinearLayout titleBar = new LinearLayout(this);
		titleBar.setOrientation(LinearLayout.HORIZONTAL);
		titleBar.setGravity(Gravity.CENTER_VERTICAL);
		titleBar.setBackgroundColor(Color.TRANSPARENT);
		titleBar.setPadding(10, 10, 10, 10);

// 最小化按钮
		ImageView toggleExpand = new ImageView(this);
		toggleExpand.setImageResource(R.drawable.min);
		toggleExpand.setPadding(10, 10, 10, 10);
		titleBar.addView(toggleExpand, new LinearLayout.LayoutParams(
							 LinearLayout.LayoutParams.WRAP_CONTENT,
							 LinearLayout.LayoutParams.WRAP_CONTENT));
		// 设置尺寸为 24dp x 24dp
		LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
			dpToPx(20), dpToPx(20));
		toggleExpand.setLayoutParams(iconParams);
// 设置图标颜色为白色
		toggleExpand.setColorFilter(Color.WHITE);
		toggleExpand.setOnClickListener(new View.OnClickListener() {  
				@Override  
				public void onClick(View v) {  
					floatingBall.removeAllViews();  
					ImageView icon = new ImageView(MainActivity.this);  
					//icon.setImageResource(android.R.drawable.ic_media_play); // 最小化图标  
					icon.setImageResource(android.R.drawable.ic_btn_speak_now);  
					icon.setColorFilter(Color.WHITE); // 相当于 XML 中的 tint="#ffffff"  
					icon.setPadding(10, 10, 10, 10);  
					floatingBall.addView(icon);  

					icon.setOnClickListener(new View.OnClickListener() {  
							@Override  
							public void onClick(View v) {  
								floatingBall.removeAllViews();  
								floatingBall.addView(expandedLayout);  
							}  
						});  
				}  
			});					 
// 标题文本
		TextView title = new TextView(this);
		title.setText(" 语音替换 ");
		title.setTextColor(Color.WHITE);
		title.setTextSize(12);
		title.setTypeface(null, Typeface.BOLD);
		title.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
			0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
		toggleExpand.setColorFilter(Color.WHITE);
		titleBar.addView(title, titleParams);

// 关闭按钮
		ImageView closeBtn = new ImageView(this);
		closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
		closeBtn.setPadding(10, 10, 10, 10);
		closeBtn.setColorFilter(Color.WHITE);

		titleBar.addView(closeBtn, new LinearLayout.LayoutParams(
							 LinearLayout.LayoutParams.WRAP_CONTENT,
							 LinearLayout.LayoutParams.WRAP_CONTENT));

		// 设置尺寸为 24dp x 24dp
		LinearLayout.LayoutParams iconParams1 = new LinearLayout.LayoutParams(
			dpToPx(24), dpToPx(24));


		closeBtn.setLayoutParams(iconParams1);
		closeBtn.setOnClickListener(new View.OnClickListener() {  
				@Override  
				public void onClick(View v) {  
					if (floatingBall.getWindowToken() != null) {  
						windowManager.removeView(floatingBall);  
						floatingBall = null;
					}  
				}  
			});

// 这里添加事件监听...
		expandedLayout.addView(titleBar);
		// 添加所有按钮  
		expandedLayout.addView(modeButton);  
		expandedLayout.addView(ttsButton);  
		expandedLayout.addView(fileButton);  
		//expandedLayout.addView(minimizeButton);  
		//expandedLayout.addView(closeButton);  

		// 初始状态为展开  
		floatingBall.addView(expandedLayout);  

		// 添加到窗口  
		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(  
			WindowManager.LayoutParams.WRAP_CONTENT,  
			WindowManager.LayoutParams.WRAP_CONTENT,  
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?  
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :  
			WindowManager.LayoutParams.TYPE_PHONE,  
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,  
			PixelFormat.TRANSLUCENT  
		);  
		params.gravity = Gravity.TOP | Gravity.LEFT;  
		params.x = 100;  
		params.y = 100;  

		windowManager.addView(floatingBall, params);  

		// 拖动功能  
		floatingBall.setOnTouchListener(new View.OnTouchListener() {  
				private int initialX, initialY;  
				private float initialTouchX, initialTouchY;  

				@Override  
				public boolean onTouch(View v, MotionEvent event) {  
					switch (event.getAction()) {  
						case MotionEvent.ACTION_DOWN:  
							initialX = params.x;  
							initialY = params.y;  
							initialTouchX = event.getRawX();  
							initialTouchY = event.getRawY();  
							return true;  
						case MotionEvent.ACTION_MOVE:  
							params.x = initialX + (int) (event.getRawX() - initialTouchX);  
							params.y = initialY + (int) (event.getRawY() - initialTouchY);  
							windowManager.updateViewLayout(floatingBall, params);  
							return true;  
					}  
					return false;  
				}  
			});  
	}

	@Override
    protected void onDestroy() {
		super.onDestroy();
		// 移除悬浮球
        if (floatingBall != null) {
			windowManager.removeView(floatingBall);
			floatingBall = null;
        }
    }

	private int dpToPx(int dp) {
		float density = getResources().getDisplayMetrics().density;
		return Math.round(dp * density);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (fileVoiceReplacer != null) {
			fileVoiceReplacer.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	
	private WindowManager inputWindowManager;
	private View inputView;

	public interface TextInputListener {
		void onTextEntered(String text);
	}

	private void showTextInputFloating(final TextInputListener listener) {
		if (inputView != null) return; // 避免重复添加

		inputWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		// 外层点击区域（全屏透明）
		FrameLayout rootLayout = new FrameLayout(this);
		rootLayout.setBackgroundColor(Color.parseColor("#80000000")); // 半透明遮罩

		// 内部浮窗内容区域
		final LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(100, 100, 100, 100);
		layout.setBackgroundColor(Color.WHITE);
		layout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

		// 美化圆角（兼容方式）
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			layout.setElevation(20);
		}

		// 添加标题
		TextView title = new TextView(this);
		title.setText("微软 TTS 文字转语音");
		title.setTextSize(16);
		title.setTypeface(null, Typeface.BOLD);
		title.setPadding(0, 0, 0, 20); // 设置内边距
		title.setGravity(Gravity.CENTER); // 文本居中

// 创建布局参数
		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, // 宽度占满父布局
			ViewGroup.LayoutParams.WRAP_CONTENT  // 高度自适应
		);

// 设置外边距
		params1.setMargins(80, 40, 80, 40); // 左、上、右、下外边距
		title.setLayoutParams(params1);    // 应用布局参数到 TextView
		
		final EditText editText = new EditText(this);
		editText.setHint(""); // 不显示提示文字
		editText.setBackgroundColor(Color.WHITE);
		editText.setTextColor(Color.BLACK);
		editText.setPadding(20, 20, 20, 20);
		editText.setTextSize(16);

		Button confirmBtn = new Button(this);
		confirmBtn.setText("确定");
		confirmBtn.setTextSize(12);
		
		layout.addView(title);
		layout.addView(editText);
		layout.addView(confirmBtn);

		// 内容区域添加到外层布局中并居中
		FrameLayout.LayoutParams innerParams = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT,
			Gravity.CENTER
		);
		rootLayout.addView(layout, innerParams);
		inputView = rootLayout;

		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			Build.VERSION.SDK_INT >= 26 ?
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
			WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
			WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
			PixelFormat.TRANSLUCENT
		);

		inputWindowManager.addView(inputView, params);

		// 点击外部区域关闭浮窗
		rootLayout.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						int[] location = new int[2];
						layout.getLocationOnScreen(location);
						int left = location[0];
						int top = location[1];
						int right = left + layout.getWidth();
						int bottom = top + layout.getHeight();

						// 如果触摸点不在内容区域内
						if (event.getRawX() < left || event.getRawX() > right ||
							event.getRawY() < top || event.getRawY() > bottom) {
							removeTextInputFloating();
							return true;
						}
					}
					return false;
				}
			});

		// 自动弹出输入法
		editText.requestFocus();
		editText.postDelayed(new Runnable() {
				public void run() {
					InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					if (imm != null) {
						imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
					}
				}
			}, 200);

		// 确定按钮监听
		confirmBtn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String text = editText.getText().toString().trim();
					if (text.isEmpty()) {
						Toast.makeText(MainActivity.this, "❌ 输入不能为空", Toast.LENGTH_SHORT).show();
					} else {
						listener.onTextEntered(text);
						removeTextInputFloating();
					}
				}
			});
	}

	private void removeTextInputFloating() {
		if (inputWindowManager != null && inputView != null) {
			inputWindowManager.removeView(inputView);
			inputView = null;
		}
	}
	
}
