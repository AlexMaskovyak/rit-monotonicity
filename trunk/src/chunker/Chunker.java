/*
 * Chunker.java
 *
 */

package chunker;

import java.io.*;

/**
 * A file splitter based on raid 5 like XOR'ing
 *
 * @author Kevin Cheek
 * @author Alex Maskovyak
 */
public class Chunker {
	
	/**
	 * This function will split the file into m chunks.
	 *
	 * @param path 	The directory for storage of the input and output
	 * @param fName	The file name
	 * @param m		The number of chunks. (3-6 recommended size)
	 */
	public static ChunkedFileInfo chunk(String path, String fName, int m) {
		File file = null;
		FileInputStream fi = null;
		FileOutputStream fos[] = null;
		ChunkedFileInfo chunkInfo = null;
		
		try{
			// setup original file inputstream and chunk output streams
			file = new File( path + fName );
			fi = new FileInputStream( file );
			fos = new FileOutputStream[m];
			
			// create resultant chunk info container
			chunkInfo = new ChunkedFileInfo( file.getAbsolutePath() );
			
			// process control information
			int block;
			int parity = m;
			int pData = 0;

			String chunkPath;
			
			for( int i = 0; i < m; i++ ) {
				chunkPath = String.format( "%s%d_%s", path, i, fName );
				chunkInfo.addChunkPaths( chunkPath );
				fos[ i ] = new FileOutputStream( chunkPath );
			}
			while( fi.available() > 0 ){
				pData = 0;
				parity = ++parity%m;
				for( int i = 0; i < m; i++ ){
					if( i != parity && fi.available() > 0 ){
						block = fi.read();
						fos[ i ].write(block);
						pData = pData ^ block;
		//				System.out.println("data: " + (char)block + " i: "+ i);
					}
				}
				fos[ parity ].write(pData);
			}
			
			chunkInfo.calculateChunkInfo();

		} catch( FileNotFoundException e ) {
			e.printStackTrace();
		} catch( IOException e ) {
			e.printStackTrace();
		} finally {
			try {
				fi.close();
				for( FileOutputStream fo : fos ) {
					fo.close();
				}
				System.gc();
			} catch ( Exception e ) {}
		}
		
		return chunkInfo;
	}

	
	/**
	 * Reassemble the file chunks into a single file.
	 * FileChunks must be in order from 0 -> M with null being given to missing
	 * files.
	 *
	 * @param iPath 		Input file path
	 * @param fileChunks	Input file names
	 * @param oPath			Output file path
	 * @param outputFile	Output file name
	 * @throws IllegalArgumentException when more than one of the specified filechunks cannot be found.
	 */
	public static void reassemble(String iPath, String[] fileChunks, String oPath,
			String outputFile) throws IllegalArgumentException {

		try{
			int filesNotFound = 0;
			
			int m = fileChunks.length;
			int block[] = new int[m];
			int pData = 0;
			boolean done = false;
			int regenerate = -1;
			int parity = m;
			FileOutputStream fo = new FileOutputStream(oPath + outputFile);
			FileInputStream fis[] = new FileInputStream[m];

			for( int i = 0; i < m; i++ ){
				try{
					fis[ i ] = new FileInputStream(iPath + fileChunks[ i ]);
				}catch( FileNotFoundException e ){
					fis[i] = null;
					regenerate = i;
					filesNotFound++;
					if( filesNotFound > 1 ) {
						throw new IllegalArgumentException("Must have all specified fileChunks for reassembly.");
					}
				}
			}

			while(!done){
				pData = 0;
				parity = ++parity%m;
				for( int i=0; i < m; i++){
					if( fis[i] != null){
						block[i] = fis[i].read();
						if( block[i] >= 0){
							pData = block[i] ^ pData;
		//					System.out.println("Reading: "+ block[i] + " i: "+ i + " pData: "+ pData);
						}else{
							done = true;
						}
					}
				}

				if( regenerate >= 0 ){
	//				System.out.println("Regenerating: " + (char)pData);
					block[regenerate] = pData;
				}

				for(int i=0; i < m; i++){
					if( i != parity && block[i] >= 0){
						fo.write(block[i]);
					}
				}
			}

		} catch( IOException e ) {
			e.printStackTrace();
		}

	}
}