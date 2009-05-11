package eve;

import java.io.Serializable;

/**
 * Standard Message type sent to Eve to
 * provide some Structure.
 *
 * @author Joseph Pecoraro
 */
public class EveMessage implements Serializable {

	private static final long serialVersionUID = -6996458820753824403L;

	/** Sent From */
	private String m_from;

	/** Sent To */
	private String m_to;

	/** Type of Message */
	private EveType m_type;

	/** Data */
	private String m_data;

	/**
	 * Basic Constructor
	 * @param from who the message is coming from
	 * @param to who the message is going to
	 * @param type the type of the message
	 * @param data the message's information
	 */
	public EveMessage(String from, String to, EveType type, String data) {
		m_from = from;
		m_to = to;
		m_type = type;
		m_data = data;
	}


	/**
	 * Simple String representation of a message
	 * @return a nicely formatted message
	 */
	public String toString() {
		return "EveMessage:\n" +
		       "-----------\n" +
		       "From: " + m_from + "\n" +
		       "To:   " + m_to + "\n" +
		       "Type: " + m_type + "\n" +
		       "Data: " + m_data + "\n\n";
	}


	/*
	 * Getters and Setters
	 */

	public String getFrom() {
		return m_from;
	}

	public void setFrom(String from) {
		m_from = from;
	}

	public String getTo() {
		return m_to;
	}

	public void setTo(String to) {
		m_to = to;
	}

	public EveType getType() {
		return m_type;
	}

	public void setType(EveType type) {
		m_type = type;
	}

	public String getData() {
		return m_data;
	}

	public void setData(String data) {
		m_data = data;
	}

}
