package org.voice.replace.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TTSVoiceReplacer {

    private Context context;

	public TTSVoiceReplacer(Context context) {
		this.context = context;
	}

    public void replace(String text, boolean isQQ, String voice, String qqNumber, String wechatId) {
        if (text == null || text.trim().isEmpty()) {
            toast("输入文本为空");
            return;
        }

        if (isQQ && (qqNumber == null || qqNumber.trim().isEmpty())) {
            toast("QQ号未配置");
            return;
        }

        if (!isQQ && (wechatId == null || wechatId.trim().isEmpty())) {
            toast("微信文件夹ID未配置");
            return;
        }

        String baseDir = isQQ
            ? "/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/" + qqNumber + "/ptt"
            : "/data/data/com.tencent.mm/MicroMsg/" + wechatId + "/voice2";

		String latestPathCmd = isQQ
			? "su -c \"find '" + baseDir + "' -mindepth 2 -type f -name '*.slk' -printf '%T@ %p\\n' | sort -n | tail -1 | cut -d' ' -f2-\""
			: "su -c \"find '" + baseDir + "' -type f -name '*.amr' -printf '%T@ %p\\n' | sort -n | tail -1 | cut -d' ' -f2-\"";

		String latestFile = runCommand(latestPathCmd).trim();
        Log.i("TTSVoiceReplacer", "最新语音路径: " + latestFile);

        if (latestFile.isEmpty()) {
            toast("未找到目标语音文件");
            return;
        }

        //toast("找到语音文件，开始生成 TTS");

        // 步骤 1：TTS 生成 MP3
        String mp3Path = "/sdcard/Download/tts.mp3";
        //String ttsCmd = "/data/data/com.termux/files/usr/bin/edge-tts --text \"" + text + "\" --voice \"" + voice + "\" --write-media=\"" + mp3Path + "\"";

		String ttsCmd = "/data/data/com.termux/files/usr/bin/edge-tts --text \"" + text + "\" --voice \"" + voice + "\" --write-media=\"" + mp3Path + "\"";

// 打印命令
		Log.i("TTSVoiceReplacer", "执行命令: " + ttsCmd);

// 复制到剪贴板

		if (!runCheck(ttsCmd, "TTS语音生成失败")) return;
        //toast("TTS 语音生成完成");
		if (isQQ) {
			// 步骤 2：MP3 转 PCM
			String pcmPath = "/sdcard/Download/tts.pcm";
			String ffmpegCmd = "/data/data/com.termux/files/usr/bin/ffmpeg -y -i " + mp3Path + " -ar 48000 -ac 1 -f s16le " + pcmPath + " > /dev/null 2>&1";
			if (!runCheck(ffmpegCmd, "MP3 转 PCM 失败")) return;
			//toast("MP3 转 PCM 成功");

			// 步骤 3：PCM 转 Silk
			String silkPath = "/sdcard/Download/tts.slk";
			String silkCmd = "/data/data/com.termux/files/usr/bin/python3 -c \"import pilk; pilk.encode('" + pcmPath + "', '" + silkPath + "', pcm_rate=48000, tencent=True)\"";
			if (!runCheck(silkCmd, "PCM 转 Silk 失败")) return;
			//toast("PCM 转 Silk 成功");

			// 步骤 4：替换语音
			String permCmd = "stat -c '%a %U %G' \"" + latestFile + "\"";
			String[] perm = runCommand("su -c \"" + permCmd + "\"").trim().split(" ");
			if (perm.length != 3) {
				toast("权限信息解析失败");
				return;
			}

			String mode = perm[0], user = perm[1], group = perm[2];
			String replaceCmd = ""
				+ "su -c 'chown " + user + ":" + group + " " + silkPath + "' && "
				+ "su -c 'chmod " + mode + " " + silkPath + "' && "
				+ "su -c 'rm -f \"" + latestFile + "\"' && "
				+ "su -c 'cp -f " + silkPath + " \"" + latestFile + "\"' && "
				+ "su -c 'rm -f /sdcard/Download/tts.*'";
			if (!runCheck(replaceCmd, "❌ 替换语音失败")) return;

			toast("✅ TTS QQ语音替换成功！");
		} else {
			// 步骤 2：MP3 转 AMR
			String amrPath = "/sdcard/Download/tts.amr";
			String ffmpegCmd = "/data/data/com.termux/files/usr/bin/ffmpeg -y -loglevel quiet -i " + mp3Path + " -ar 8000 -ac 1 -ab 12.2k " + amrPath + " > /dev/null 2>&1";
			if (!runCheck(ffmpegCmd, "MP3 转 AMR 失败")) return;

			// 微信语音替换（简化版）
			String copyCmd = ""
				+ "su -c 'cp -f " + amrPath + " \"" + latestFile + "\"' && "
				+ "su -c 'chmod 600 \"" + latestFile + "\"' && "
				+ "su -c 'rm -f /sdcard/Download/tts.*'";
			ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("TTS Command", copyCmd);
			clipboard.setPrimaryClip(clip);
			if (!runCheck(copyCmd, "❌ 替换语音失败")) return;
			
			toast("✅ TTS 微信语音替换成功！");
		}
    }

    private boolean runCheck(String cmd, String failMessage) {
        int code = runShell(cmd);
        if (code != 0) {
            toast(failMessage);
            return false;
        }
        return true;
    }

    private int runShell(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            process.waitFor();
            return process.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private String runCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void toast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        Log.i("TTSVoiceReplacer", msg);
    }
}
