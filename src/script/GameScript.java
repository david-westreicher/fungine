package script;

import game.Game;

import javax.script.CompiledScript;

import manager.ScriptManager;

public class GameScript {

	public CompiledScript script;

	public GameScript(String s) {
		((ScriptManager) Game.INSTANCE.getManager("script")).update(s, this);
	}

}
