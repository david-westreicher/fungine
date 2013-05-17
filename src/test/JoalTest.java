package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import com.jogamp.openal.AL;
import com.jogamp.openal.ALFactory;
import com.jogamp.openal.util.ALut;

public class JoalTest {

	static AL al = ALFactory.getAL();

	// Buffers hold sound data.
	static int[] buffer = new int[1];

	// Sources are points emitting sound.
	static int[] source = new int[1];
	// Position of the source sound.
	static float[] sourcePos = { 0.0f, 0.0f, 0.0f };

	// Velocity of the source sound.
	static float[] sourceVel = { 0.0f, 0.0f, 0.0f };

	// Position of the listener.
	static float[] listenerPos = { 0.0f, 0.0f, 0.0f };

	// Velocity of the listener.
	static float[] listenerVel = { 0.0f, 0.0f, 0.0f };

	// Orientation of the listener. (first 3 elements are "at", second 3 are
	// "up")
	static float[] listenerOri = { 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f };

	static int LoadALData() {

		// variables to load into

		int[] format = new int[1];
		int[] size = new int[1];
		ByteBuffer[] data = new ByteBuffer[1];
		int[] freq = new int[1];
		int[] loop = new int[1];

		// Load wav data into a buffer.
		al.alGenBuffers(1, buffer, 0);
		if (al.alGetError() != AL.AL_NO_ERROR)
			return AL.AL_FALSE;

		ALut.alutLoadWAVFile("ressources/explosion.wav", format, data, size,
				freq, loop);
		al.alBufferData(buffer[0], format[0], data[0], size[0], freq[0]);
		// ALut.alutUnloadWAV(format[0], data[0], size[0], freq[0]);
		// Bind buffer with a source.
		al.alGenSources(1, source, 0);

		if (al.alGetError() != AL.AL_NO_ERROR)
			return AL.AL_FALSE;

		al.alSourcei(source[0], AL.AL_BUFFER, buffer[0]);
		al.alSourcef(source[0], AL.AL_PITCH, 1.0f);
		al.alSourcef(source[0], AL.AL_GAIN, 1.0f);
		al.alSourcefv(source[0], AL.AL_POSITION, sourcePos, 0);
		al.alSourcefv(source[0], AL.AL_VELOCITY, sourceVel, 0);
		al.alSourcei(source[0], AL.AL_LOOPING, loop[0]);
		// Do another error check and return.
		if (al.alGetError() == AL.AL_NO_ERROR)
			return AL.AL_TRUE;

		return AL.AL_FALSE;
	}

	static void setListenerValues() {
		al.alListenerfv(AL.AL_POSITION, listenerPos, 0);
		al.alListenerfv(AL.AL_VELOCITY, listenerVel, 0);
		al.alListenerfv(AL.AL_ORIENTATION, listenerOri, 0);
	}

	static void killALData() {
		al.alDeleteBuffers(1, buffer, 0);
		al.alDeleteSources(1, source, 0);
		ALut.alutExit();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ALut.alutInit();
		al.alGetError();
		// Load the wav data.
		if (LoadALData() == AL.AL_FALSE)
			return;

		setListenerValues();

		// Setup an exit procedure.

		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new Thread(new Runnable() {
			public void run() {
				killALData();
			}
		}));
		char[] c = new char[1];
		while (c[0] != 'q') {
			try {
				BufferedReader buf = new BufferedReader(new InputStreamReader(
						System.in));
				System.out
						.println("Press a key and hit ENTER: "
								+ "'p' to play, 's' to stop, 'h' to pause and 'q' to quit");
				buf.read(c);
				switch (c[0]) {
				case 'p':
					// Pressing 'p' will begin playing the sample.
					al.alSourcePlay(source[0]);
					break;
				case 's':
					// Pressing 's' will stop the sample from playing.
					al.alSourceStop(source[0]);
					break;
				case 'h':
					// Pressing 'n' will pause (hold) the sample.
					al.alSourcePause(source[0]);
					break;
				}
			} catch (IOException e) {
				System.exit(1);
			}
		}
	}
}
