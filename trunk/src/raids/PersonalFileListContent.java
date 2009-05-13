package raids;

import java.util.List;

import rice.p2p.commonapi.Id;

/**
 * PastContent storage data structure that stores a User's List of Files,
 * which are PersonalFileInfo objects.
 *
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class PersonalFileListContent extends PastContentList<PersonalFileInfo> {

	/** Generated serial version */
	private static final long serialVersionUID = 3421181011400632594L;

	/**
	 * Default Constructor
	 * Creates an Empty list of Personal Files.
	 * @param myId id of the content
	 */
	public PersonalFileListContent(Id myId) {
		super(myId);
	}

	/**
	 * Constructor
	 * Initializes storage with a list of files
	 * @param myId id of the content
	 * @param list list of files
	 */
	public PersonalFileListContent(Id myId, List<PersonalFileInfo> list) {
		super(myId, list);
	}

}
