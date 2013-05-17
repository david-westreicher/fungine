package test;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;

import settings.Settings;
import util.Log;
import util.RepeatedThread;
import util.Stoppable;

public class FrameDisposal extends JFrame {
	public FrameDisposal() {
		super("test");
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				Stoppable.stopAll();
				Log.log(FrameDisposal.class, "close");
				// WindowEvent wev = new WindowEvent(FrameDisposal.this,
				// WindowEvent.WINDOW_CLOSING);
				// Toolkit.getDefaultToolkit().getSystemEventQueue()
				// .postEvent(wev);
				FrameDisposal.this.dispose();
			}

		});
		new RepeatedThread(1000, "test") {

			@Override
			protected void executeRepeatedly() {
				try {
					BufferedReader br = new BufferedReader(
							new FileReader(new File(Settings.RESSOURCE_FOLDER
									+ "objects.xml")));
					Log.log(this, br.readLine());
					br.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		this.getContentPane().addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub

			}
		});
	}

	public static void main(String[] args) {
		FrameDisposal fr = new FrameDisposal();
		Log.log(FrameDisposal.class, "open");
		fr.setVisible(true);
	}

}
