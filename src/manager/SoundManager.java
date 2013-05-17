package manager;

import game.Game;
import game.Updatable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3f;

import settings.Settings;
import util.Log;
import world.Camera;
import world.GameObject;

import com.jogamp.openal.AL;
import com.jogamp.openal.ALFactory;
import com.jogamp.openal.util.ALut;

public class SoundManager implements Updatable {
	public class SoundInfo {
		public Integer sound;
		public GameObject go;
	}

	static AL al = ALFactory.getAL();
	public static Map<String, SoundInfo> sounds = new HashMap<String, SoundInfo>();
	public static List<int[][]> buffers = new ArrayList<int[][]>();
	private static Vector3f tmpVector3f = new Vector3f();
	private static Vector3f tmp2Vector3f = new Vector3f();

	public SoundManager() {
		ALut.alutInit();
		if (al.alGetError() != AL.AL_NO_ERROR)
			Log.err(this, "couldn't load joal");
	}

	public void playSound(String file, GameObject go, float pitch, float gain) {
		SoundInfo soundInfo = sounds.get(file);
		if (soundInfo == null) {
			Log.log(this, "loading sound: " + file);
			soundInfo = new SoundInfo();
			soundInfo.sound = loadSound(file);
			if (soundInfo.sound == null)
				Log.err(this, "couldn't load sound: " + file);
			else {
				Log.log(this, "successfully loaded sound: " + file);
				sounds.put(file, soundInfo);
			}
		}
		soundInfo.go = go;
		if (soundInfo.sound != null) {
			al.alSourcef(soundInfo.sound, AL.AL_PITCH, pitch);
			al.alSourcef(soundInfo.sound, AL.AL_GAIN, gain);
			al.alSourcePlay(soundInfo.sound);
		}
	}

	public void playSound(String file) {
		playSound(file, null, 1.0f, 1.0f);
	}

	private static Integer loadSound(String file) {
		int[] buffer = new int[1];
		int[] source = new int[1];
		int[] format = new int[1];
		int[] size = new int[1];
		ByteBuffer[] data = new ByteBuffer[1];
		int[] freq = new int[1];
		int[] loop = new int[1];
		float[] sourcePos = { 0.0f, 0.0f, 0.0f };
		float[] sourceVel = { 0.0f, 0.0f, 0.0f };

		// Load wav data into a buffer.
		al.alGenBuffers(1, buffer, 0);
		if (al.alGetError() != AL.AL_NO_ERROR)
			return null;

		ALut.alutLoadWAVFile(Settings.RESSOURCE_FOLDER + file, format, data,
				size, freq, loop);
		al.alBufferData(buffer[0], format[0], data[0], size[0], freq[0]);
		// ALut.alutUnloadWAV(format[0], data[0], size[0], freq[0]);
		// Bind buffer with a source.
		al.alGenSources(1, source, 0);

		if (al.alGetError() != AL.AL_NO_ERROR)
			return null;

		al.alSourcei(source[0], AL.AL_BUFFER, buffer[0]);
		al.alSourcef(source[0], AL.AL_PITCH, 1.0f);
		al.alSourcef(source[0], AL.AL_GAIN, 1.0f);
		al.alSourcefv(source[0], AL.AL_POSITION, sourcePos, 0);
		al.alSourcefv(source[0], AL.AL_VELOCITY, sourceVel, 0);
		al.alSourcei(source[0], AL.AL_LOOPING, AL.AL_FALSE);
		// Do another error check and return.
		if (al.alGetError() == AL.AL_NO_ERROR) {
			buffers.add(new int[][] { buffer, source });
			return source[0];
		}

		return null;
	}

	@Override
	public void update(float interp) {
		Camera cam = Game.INSTANCE.cam;
		float[] listenerPos = cam.pos;
		al.alListenerfv(AL.AL_POSITION, listenerPos, 0);
		float[] listenerVel = new float[] { 0, 0, 0 };
		al.alListenerfv(AL.AL_VELOCITY, listenerVel, 0);
		tmpVector3f.set(0, 0, -1);
		tmp2Vector3f.set(0, 1, 0);
		cam.rotationMatrix.transform(tmpVector3f);
		cam.rotationMatrix.transform(tmp2Vector3f);
		float[] listenerOri = new float[] { tmpVector3f.x, tmpVector3f.y,
				tmpVector3f.z, tmp2Vector3f.x, tmp2Vector3f.y, tmp2Vector3f.z };
		al.alListenerfv(AL.AL_ORIENTATION, listenerOri, 0);
		for (SoundInfo si : sounds.values()) {
			if (si.go != null && si.sound != null) {
				al.alSourcefv(si.sound, AL.AL_POSITION, si.go.pos, 0);
			}
		}
	}

	@Override
	public void dispose() {
		for (int[][] bufSrc : buffers) {
			al.alDeleteBuffers(1, bufSrc[0], 0);
			al.alDeleteSources(1, bufSrc[1], 0);
		}
		ALut.alutExit();
	}

}
