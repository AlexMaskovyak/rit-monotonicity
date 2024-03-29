package chunker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.SHA1;

/**
 * Stores information about a chunked file.
 * @author Alex Maskovyak
 *
 */
public class ChunkedFileInfo {

	///
	///
	/// Hidden variables.
	///
	///
	
	/** Path to the original file. */
	private String m_originalPath;
	/** SHA-1 hash of original file. */
	private String m_originalFileHash;
	
	/** Paths to file chunks. */
	private List<String> m_chunkPaths;
	/** Hashes of file chunks. */
	private Map<String,String> m_chunkHashes;
	/** Size of the largest file chunk. */
	private long m_maxChunkSize;
	
	///
	///
	/// Constructors.
	///
	///
	
	/**
	 * Default constructor.
	 * @param p_originalPath Path to the original
	 */
	public ChunkedFileInfo( String p_originalPath ) {
		m_originalPath = new File( p_originalPath ).getAbsoluteFile().getAbsolutePath();
		m_originalFileHash = SHA1.getInstance().hash( new File( m_originalPath ) );
		
		m_chunkPaths = new ArrayList<String>();
		m_chunkHashes = new HashMap<String,String>();
		
		m_maxChunkSize = 0l;
	}
	
	///
	///
	/// Operations.
	///
	///
	
	public void addChunkPaths( String... p_chunkPaths ) {
		for( String chunk : p_chunkPaths ) {
			File chunkFile = new File( chunk ).getAbsoluteFile();
			m_chunkPaths.add( chunkFile.getAbsolutePath() );
		}
	}
	
	/**
	 * Calculate the hashes and maximum chunk size.
	 */
	public void calculateChunkInfo() {
		for( String chunk : m_chunkPaths ) {
			File chunkFile = new File( chunk );
			m_chunkHashes.put( chunk, SHA1.getInstance().hash( chunkFile ) );
			m_maxChunkSize = 
				( chunkFile.length() > m_maxChunkSize ) 
					? chunkFile.length()
					: m_maxChunkSize;
		}
	}
	
	///
	///
	/// Access methods.
	///
	///
	
	/**
	 * Obtain the original file's path.
	 * @return Path to original file.
	 */
	public String getOriginalPath() {
		return m_originalPath;
	}
	
	/**
	 * Obtain the original file's hash.
	 * @return Hash of the original file's contents.
	 */
	public String getOriginalFileHash() {
		return m_originalFileHash;
	}
	
	/**
	 * Obtain paths to the chunked files.
	 * @return Paths to file chunks.
	 */
	public String[] getChunkPaths() {
		String[] chunkArray = new String[ m_chunkPaths.size() ];
		return m_chunkPaths.toArray( chunkArray );
	}
	
	/**
	 * Obtain the size, in bytes, of the maximum file chunk.
	 * @return Size of the largest chunk.
	 */
	public long getMaxChunkSize() {
		return m_maxChunkSize;
	}
	
	/**
	 * Obtain the SHA-1 hash of a file chunk.
	 * @param p_chunkPath file chunk of whose contents we want to get a hash 
	 * 			value.
	 * @return the SHA1 hash of the contents of the file chunk.
	 */
	public String getHashForChunk( String p_chunkPath ) {
		return m_chunkHashes.get( p_chunkPath );
	}
}
