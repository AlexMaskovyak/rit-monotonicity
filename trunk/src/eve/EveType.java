package eve;

/**
 * The Different Message Types sent
 * to Eve for different reasons.
 *
 * @author Joseph Pecoraro
 * @author Kevin Cheek
 */
public enum EveType {

	/** Default Message Type */
	MSG,

	/** Messages we may want to ignore later */
	DEBUG,

	/** Register Message (so Eve has a Map of Id to Names) */
	REGISTER,

	/** File chunk is being uploaded */
	UPLOAD,

	/** File chunk is being downloaded */
	DOWNLOAD,

	/** Heart beat message */
	HEARTBEAT,

	/** Forward message */
	FORWARD
}
