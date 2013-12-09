package util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import physics.AbstractCollisionShape;
import rendering.model.ChunkRenderer;
import rendering.model.ModelRenderer;
import rendering.model.TerrainRenderer;
import rendering.model.VoxelTerrainRenderer;
import rendering.voxel.VoxelWorldRenderer;
import settings.Settings;
import world.GameObjectType;

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
			} else if (currentTag.equals("physics")) {
				if (s.equals("box")) {
					currentObject.shape = new AbstractCollisionShape.BoxShape();
				} else if (s.equals("sphere")) {
					currentObject.shape = new AbstractCollisionShape.SphereShape();
				} else if (s.equals("capsule")) {
					currentObject.shape = new AbstractCollisionShape.CapsuleShape();
				}
			} else if (currentTag.equals("renderer")) {
				if (s.equals("terrain"))
					currentObject.renderer = new TerrainRenderer();
				else if (s.equals("voxelTerrain"))
					currentObject.renderer = new VoxelTerrainRenderer();
				if (split.length > 1) {
					if (split[1].equals("obj"))
						currentObject.renderer = new ModelRenderer(split[0],
								split.length == 3, split.length == 4);
					else if (split[1].equals("vox")) {
						ChunkRenderer cr = new ChunkRenderer(split[0]);
						currentObject.renderer = cr;
					} else if (split[1].equals("voxWorld"))
						currentObject.renderer = new VoxelWorldRenderer();
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
