package Sound;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SoundManager {
    private static volatile float globalVolume = 0.5f;
    private static Clip bgClip = null;
    private static final Object bgLock = new Object();

    public static void playBackground(String filePath) {
        stopBackground();
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filePath));
            AudioFormat baseFormat = ais.getFormat();
            AudioFormat decoded = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            AudioInputStream din = AudioSystem.getAudioInputStream(decoded, ais);
            synchronized (bgLock) {
                bgClip = AudioSystem.getClip();
                bgClip.open(din);
                setClipVolume(bgClip, globalVolume);
                bgClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Exception e) {
            System.err.println("SoundManager.playBackground error: " + e.getMessage());
        }
    }

    public static void stopBackground() {
        synchronized (bgLock) {
            if (bgClip != null) {
                try {
                    bgClip.stop();
                    bgClip.close();
                } catch (Exception ignored) {
                }
                bgClip = null;
            }
        }
    }

    public static void playOnce(String filePath) {
        new Thread(() -> {
            try (AudioInputStream ais0 = AudioSystem.getAudioInputStream(new File(filePath))) {
                AudioFormat baseFormat = ais0.getFormat();
                AudioFormat decoded = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false);
                AudioInputStream din = AudioSystem.getAudioInputStream(decoded, ais0);
                Clip clip = AudioSystem.getClip();
                clip.open(din);
                setClipVolume(clip, globalVolume);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.err.println("SoundManager.playOnce error: " + e.getMessage());
            }
        }, "SFX-Player").start();
    }

    public static void setGlobalVolume(float v) {
        if (v < 0f) v = 0f;
        if (v > 1f) v = 1f;
        globalVolume = v;
        synchronized (bgLock) {
            if (bgClip != null && bgClip.isOpen()) {
                setClipVolume(bgClip, globalVolume);
            }
        }
    }

    public static float getGlobalVolume() {
        return globalVolume;
    }

    private static void setClipVolume(Clip clip, float linearVolume) {
        if (clip == null) return;
        try {
            FloatControl vol = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB;
            if (linearVolume <= 0.0001f) {
                dB = vol.getMinimum();
            } else {
                dB = (float) (20.0 * Math.log10(linearVolume));
                if (dB < vol.getMinimum()) dB = vol.getMinimum();
                if (dB > vol.getMaximum()) dB = vol.getMaximum();
            }
            vol.setValue(dB);
        } catch (Exception ignored) {
        }
    }
}
