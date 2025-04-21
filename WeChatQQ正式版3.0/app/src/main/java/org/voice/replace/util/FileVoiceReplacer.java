package org.voice.replace.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class FileVoiceReplacer {

    private static final int REQUEST_CODE_PICK_AUDIO = 999;

    private Context context;
    private boolean isQQ;
    private String qqNumber;
    private String wechatId;

    public FileVoiceReplacer(Context context) {
        this.context = context;
    }

    public void startReplace(boolean isQQ, String qqNumber, String wechatId) {
        this.isQQ = isQQ;
        this.qqNumber = qqNumber;
        this.wechatId = wechatId;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        ((Activity) context).startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_PICK_AUDIO || resultCode != Activity.RESULT_OK || data == null) {
            toast("未选择文件");
            return;
        }

        Uri uri = data.getData();		
        if (uri == null) {
            toast("获取文件失败");
            return;
        }
		//toast(uri.toString());
        File srcFile = getFileFromUri(uri);
        if (srcFile == null || !srcFile.exists()) {
            toast("读取文件失败");
            return;
        }
		//toast(srcFile.toString());
        String latestFile = getLatestVoicePath();
        if (latestFile.isEmpty()) {
            toast("未找到原语音文件");
            return;
        }
		//toast(latestFile.toString());
        Log.i("FileVoiceReplacer", "目标语音: " + latestFile);

        if (isQQ) {
			String openQQ = "am start -n com.tencent.mobileqq/com.tencent.mobileqq.activity.SplashActivity";
			runShell("su -c \"" + openQQ + "\"");
            replaceQQVoice(srcFile, latestFile);
        } else {
			String openWeChat = "am start -n com.tencent.mm/.ui.LauncherUI";
			runShell("su -c \"" + openWeChat + "\"");
            replaceWeChatVoice(srcFile, latestFile);
        }
    }

    private File getFileFromUri(Uri uri) {
        String path = FilePathUtil.getPath(this.context, uri); // 你需要自行实现 FilePathUtil.getPath() 获取真实路径
        //toast(path);
		if (path == null) return null;
        return new File(path);
    }

    private String getLatestVoicePath() {
        String baseDir = isQQ
			? "/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/" + qqNumber + "/ptt"
			: "/data/data/com.tencent.mm/MicroMsg/" + wechatId + "/voice2";

        String cmd = isQQ
			? "find '" + baseDir + "' -mindepth 2 -type f -name '*.slk' -printf '%T@ %p\\n' | sort -n | tail -1 | cut -d' ' -f2-"
			: "find '" + baseDir + "' -type f -name '*.amr' -printf '%T@ %p\\n' | sort -n | tail -1 | cut -d' ' -f2-";

        return runCommand("su -c \"" + cmd + "\"").trim();
    }

    private void replaceQQVoice(File mp3File, String targetPath) {
		File dir = mp3File.getParentFile();
		String baseName = mp3File.getName();
		if (baseName.lastIndexOf('.') > 0) {
			baseName = baseName.substring(0, baseName.lastIndexOf('.'));
		}
		File silkFile = new File(dir, baseName + ".slk");
		String pcmPath = new File(dir, baseName + ".pcm").getAbsolutePath();
		String silkPath = new File(dir, baseName + ".slk").getAbsolutePath();
		String mp3Path = mp3File.getAbsolutePath();

		//toast(mp3Path);
		if (silkFile.exists()) {
			Log.i("TTS", "Silk 文件已存在，跳过 MP3 -> PCM -> Silk 全流程");
		} else {
			//toast("转换中...");
			toast("▶️ 正在转换为 SLK ...");
			// 绝对路径替换为你设备上的ffmpeg和python3路径，务必确认存在
			String ffmpegPath = "/data/data/com.termux/files/usr/bin/ffmpeg";
			String pythonPath = "/data/data/com.termux/files/usr/bin/python3";

			String cmd1 = ffmpegPath + " -y -i \"" + mp3Path + "\" -ar 48000 -ac 1 -f s16le \"" + pcmPath + "\"";
			String cmd2 = pythonPath + " -c \"import pilk; pilk.encode(\\\"" + pcmPath + "\\\", \\\"" + silkPath + "\\\", pcm_rate=48000, tencent=True)\"";
			if (!runCheck("su -c \'" + cmd1 + "\'", "MP3转PCM失败")) return;
			if (!runCheck("su -c \'" + cmd2 + "\'", "PCM转Silk失败")) return;
			//pythonPath + " -c \"import pilk; pilk.encode('" + pcmPath + "', '" + silkPath + "', pcm_rate=48000, tencent=True)\"";
		}
		String permInfo = runCommand("su -c \"stat -c '%a %U %G' '" + targetPath + "'\"").trim();
		String[] perms = permInfo.split("\\s+");
		if (perms.length != 3) {
			toast("权限读取失败");
			return;
		}

//		String replaceCmd = ""
//			+ "chown " + perms[1] + ":" + perms[2] + " '" + silkPath + "' && "
//			+ "chmod " + perms[0] + " '" + silkPath + "' && "
//			+ "rm -f '" + targetPath + "' && "
//			+ "cp -f '" + silkPath + "' '" + targetPath + "' && "
//			+ "rm -f '" + pcmPath + "'";
		String replaceCmd = ""
			+ "chown " + perms[1] + ":" + perms[2] + " \"" + silkPath + "\" && "
			+ "chmod " + perms[0] + " \"" + silkPath + "\" && "
			+ "rm -f \"" + targetPath + "\" && "
			+ "cp -f \"" + silkPath + "\" \"" + targetPath + "\" && "
			+ "rm -f \"" + pcmPath + "\"";
//		ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
//		ClipData clip = ClipData.newPlainText("TTS Command", replaceCmd.toString());
//		clipboard.setPrimaryClip(clip);
		if (!runCheck("su -c \'" + replaceCmd + "\'", "替换语音失败")) return;

		toast("✅ QQ语音替换成功！");
	}

	private void replaceWeChatVoice(File mp3File, String targetPath) {
		File dir = mp3File.getParentFile();
		String baseName = mp3File.getName();
		if (baseName.lastIndexOf('.') > 0) {
			baseName = baseName.substring(0, baseName.lastIndexOf('.'));
		}
		File amrFile = new File(dir, baseName + ".amr");
		String amrPath = amrFile.getAbsolutePath();
		String mp3Path = mp3File.getAbsolutePath();

		//toast(mp3Path);
		if (amrFile.exists()) {
			Log.i("TTS", "Silk 文件已存在，跳过 MP3 -> PCM -> Silk 全流程");
		} else {
			toast("▶️ 正在转换为 AMR ...");
			// ffmpeg的绝对路径，替换成你设备实际路径
			String ffmpegPath = "/data/data/com.termux/files/usr/bin/ffmpeg";

			// 1. 转换 MP3 -> AMR
			//String cmd1 = ffmpegPath + " -y -i \'" + mp3Path + "\' -ar 8000 -ac 1 -ab 12.2k \'" + amrPath + "\'";
			// 推荐方式（适用于含空格路径）
			String cmd1 = ffmpegPath + " -y -i \"" + mp3Path + "\" -ar 8000 -ac 1 -ab 12.2k \"" + amrPath + "\"";
			ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("TTS Command", cmd1.toString());
			clipboard.setPrimaryClip(clip);
			if (!runCheck("su -c \'" + cmd1 + "\'", "MP3转AMR失败")) return;
		}
		// 2. 替换目标文件并设置权限
		// 这里简化，默认权限 600，若目标文件权限有特殊需求，可用类似之前那段先获取权限再赋值
		String replaceCmd = "su -c 'cp -f \"" + amrPath + "\" \"" + targetPath + "\" && chmod 600 \"" + targetPath + "\"'";

		if (!runCheck(replaceCmd, "替换语音失败")) return;

		// 运行完你可以删除临时文件
		// rm命令（可选）
		//String rmTmpCmd = "rm -f '" + amrPath + "'";
		//runCommand("su -c \"" + rmTmpCmd + "\"");

		toast("✅ 微信语音替换成功！");
	}

    private boolean runCheck(String cmd, String failMsg) {
        int code = runShell(cmd);
        if (code != 0) {
            toast(failMsg);
            return false;
        }
        return true;
    }

    private int runShell(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
            return p.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private String runCommand(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void toast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        Log.i("FileVoiceReplacer", msg);
    }
}
