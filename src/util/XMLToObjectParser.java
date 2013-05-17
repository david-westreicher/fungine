package util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Vector3f;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import physics.HeightfieldTerrainShape;
import physics.PhysicsTest;
import rendering.AnimationRenderer;
import rendering.ChunkRenderer;
import rendering.ModelRenderer;
import rendering.SpriteRenderer;
import rendering.TerrainRenderer;
import rendering.VoxelWorldRenderer;
import script.GameScript;
import settings.Settings;
import world.GameObjectType;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.SphereShape;

public class XMLToObjectParser extends DefaultHandler {
	private GameObjectType currentObject;
	private String currentTag;
	private Map<String, String> currentAtt = new HashMap<String, String>();

	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		currentAtt.clear();
		for (int i = 0; i < atts.getLength(); i++) {
			currentAtt.put(atts.getQName(i), atts.getValue(i));
		}
		if (qName.equals("object")) {
			currentObject = new GameObjectType(currentAtt.get("name"));
		} else {
			currentTag = qName;
		}

	}

	public void endElement(String namespaceURI, String localName, String qName) {
		if (qName.equals("object")) {
			Log.log(this, GameObjectType.getType(currentObject.name) + " added");
		}
	}

	public void characters(char ch[], int start, int length) {
		String s = new String(ch, start, length).trim();
		String[] split = s.split(":");
		if (s.length() > 0) {
			if (currentTag.equals("name")) {
				currentObject.name = s;
			} else if (currentTag.equals("script")) {
				currentObject.script = new GameScript(s);
			} else if (currentTag.equals("physics")) {
				float size = Float.parseFloat(split[1])
						* PhysicsTest.PHYSICS_SCALE / 2;
				float size2 = 100;
				if (split.length >= 3) {
					size2 = Float.parseFloat(split[2])
							* PhysicsTest.PHYSICS_SCALE;
				}
				if (split[0].equals("box"))
					currentObject.shape = new BoxShape(new Vector3f(size, size,
							size));
				else if (split[0].equals("sphere"))
					currentObject.shape = new SphereShape(size);
				else if (split[0].equals("wall"))
					currentObject.shape = new BoxShape(new Vector3f(1, size,
							size));
				else if (split[0].equals("capsule"))
					currentObject.shape = new CapsuleShape(size, size2);
				else if (split[0].equals("terrain")) {
					float terrainSize = Float.parseFloat(split[1])
							* PhysicsTest.PHYSICS_SCALE;
					HeightfieldTerrainShape height = new HeightfieldTerrainShape(
							TerrainRenderer.WIDTH + 1,
							TerrainRenderer.HEIGHT + 1,
							TerrainRenderer.getHeightField(), terrainSize,
							-0.5f, 0.5f, HeightfieldTerrainShape.YAXIS, false);
					float gridSpacing = terrainSize / TerrainRenderer.WIDTH;
					Log.log(this, gridSpacing, size);
					height.setLocalScaling(new Vector3f(gridSpacing, 1,
							gridSpacing));
					currentObject.shape = height;
				}
			} else if (currentTag.equals("renderer")) {
				if (s.equals("terrain"))
					currentObject.renderer = new TerrainRenderer();
				else if (s.equals("voxelTerrain"))
					currentObject.renderer = new VoxelTerrainRenderer();
				else if (s.equals("voxelTerrain"))
					currentObject.renderer = new VoxelTerrainRenderer();
				else if (s.split("/")[0].equals("img"))
					currentObject.renderer = new SpriteRenderer(split[0]);
				if (split.length > 1) {
					if (split[1].equals("anim"))
						currentObject.renderer = new AnimationRenderer(split[0]);
					else if (split[1].equals("obj"))
						currentObject.renderer = new ModelRenderer(split[0],
								split.length == 3, split.length == 4);
					else if (split[1].equals("vox")) {
						ChunkRenderer cr = new ChunkRenderer(split[0]);
						currentObject.renderer = cr;
					} else if (split[1].equals("voxWorld")) {
						currentObject.renderer = new VoxelWorldRenderer();
					} else if (split[1].equals("md5"))
						currentObject.renderer = new Md5Renderer(split[0]);
				}
			} else if (currentTag.equals("val")) {
				currentObject.set(currentAtt.get("name"), s);
			}
		}
	}

	public void parse() {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(Settings.RESSOURCE_FOLDER + Settings.OBJECTS_XML,
					this);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
