package eve;

/**
 * The Different Message Types sent
 * to Eve for different reasons.
 *
 * @author Joseph Pecoraro
 */
public enum EveType {

	/** Default Message Type */
	MSG,

	/** Messages we may want to ignore later */
	DEBUG,

	/** Register Message (so Eve has a Map of Id to Names) */
	REGISTER,

	/** A message is on route */
	FORWARD,


}
