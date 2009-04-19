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
 */
public class Chunker {

	/*
	 * Test the chunker class
	 */
	public static void main() {
		// Chunker chk = new Chunker("/Users/kevincheek/Desktop/monalisa.jpg",
		// 3, 3);
		// chunk("test.txt", 3, 3);
		String path = "/Users/kevincheek/Desktop/t/";
	//	chunk(path, "test.txt", 5);
		chunk(path, "monalisa.jpg", 5);
		String[] fileChunks = new String[5];
		for( int i=0; i < 5; i++){
	//		fileChunks[i] = i+"_"+"test.txt";
			fileChunks[i] = i+"_"+"monalisa.jpg";
		}
		reassemble(path, fileChunks, path, "monalisaOut.jpg");
	//	reassemble(path, fileChunks, path, "testOut.txt");

	}

	/**
	 * This function will split the file into m chunks.
	 *
	 * @param path 	The directory for storage of the input and output
	 * @param fName	The file name
	 * @param m		The number of chunks. (3-6 recommended size)
	 */
	public static void chunk(String path, String fName, int m) {
		try{
			File file = new File(path + fName);
			FileInputStream fi = new FileInputStream(file);
			FileOutputStream fos[] = new FileOutputStream[m];
			int block;
			int parity = m;
			int pData = 0;

			for( int i = 0; i < m; i++ ){
				fos[ i ] = new FileOutputStream(path + i + "_" + fName);
			}
			while( fi.available() > 0 ){
				pData = 0;
				parity = ++parity%m;
				for( int i = 0; i < m; i++ ){
					if( i != parity && fi.available() > 0 ){
						block = fi.read();
						fos[ i ].write(block);
						pData = pData ^ block;
						System.out.println("data: " + (char)block + " i: "+ i);
					}
				}
				fos[ parity ].write(pData);
			}
		}catch( FileNotFoundException e ){
			e.printStackTrace();
		}catch( IOException e ){
			e.printStackTrace();
		}
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
	 */
	public static void reassemble(String iPath, String[] fileChunks, String oPath,
			String outputFile) {

		try{
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
							System.out.println("Reading: "+ block[i] + " i: "+ i + " pData: "+ pData);
						}else{
							done = true;
						}
					}
				}

				if( regenerate >= 0){
					System.out.println("Regenerating: " + (char)pData);
					block[regenerate] = pData;
				}

				for(int i=0; i < m; i++){
					if( i != parity && block[i] >= 0){
						fo.write(block[i]);
					}
				}
			}

		}catch( IOException e ){
			e.printStackTrace();
		}

	}
}