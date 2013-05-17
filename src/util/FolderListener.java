package util;

public interface FolderListener {

	void removed(String s);

	void changed(String s);

	void added(String s);

}
