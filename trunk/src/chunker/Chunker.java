package chunker;

import java.io.*;

/*
 * @author Kevin Cheek
 *
 *
 * Adapted from Reed-soloman coding paper and
 * Reed Solomon Encoder/Decoder by Henry Minsky
 */
public class Chunker {
/*
	private short w; // W size 16bits


	private int			  m			= 3;
	private int			  n			= 3;
	private static final int prim_poly_4  = 023;
	private static final int prim_poly_8  = 0435;
	private static final int prim_poly_16 = 0210013;
	private static short	 gflog[], gfilog[];
	private static int	   Vandermonde[][];
	 */

	public static void main() {
		// Chunker chk = new Chunker("/Users/kevincheek/Desktop/monalisa.jpg",
		// 3, 3);

	}

	public Chunker() {
		// setupTables( 4 );

	/*
		  for( int i=0; i < gflog.length; i++){ System.out.print(gflog[i] + "
		  "); } System.out.println(); for( int i=0; i < gfilog.length; i++){
		  System.out.print(gfilog[i] + " "); }*/

		// chunk("test.txt", 3, 3);
		String path = "/Users/kevincheek/Desktop/t/";
	//	raddChunk(path, "test.txt", 5);
	//	raddChunk(path, "monalisa.jpg", 5);
		String[] fileChunks = new String[5];
		for( int i=0; i < 5; i++){
	//		fileChunks[i] = i+"_"+"test.txt";
			fileChunks[i] = i+"_"+"monalisa.jpg";
		}
		reassemble("/Users/kevincheek/Desktop/t/", fileChunks, "/Users/kevincheek/Desktop/t/", "monalisaOut.jpg");
	//	reassemble("/Users/kevincheek/Desktop/t/", fileChunks, "/Users/kevincheek/Desktop/t/", "testOut.txt");
	}

	public void raddChunk(String path, String fName, int m) {
		// String path = "/Users/kevincheek/Desktop/t/";
		System.out.println("test");

		try{
			File file = new File(path + fName);
			FileInputStream fi = new FileInputStream(file);
			// long size = file.length();
			// long chunkSize = size / m;
			FileOutputStream fos[] = new FileOutputStream[m];
			for( int i = 0; i < m; i++ ){
				fos[ i ] = new FileOutputStream(path + i + "_" + fName);
			}
			// for( int i=0; i < m; i++){
			int block;
	//		long written = 0;
			int parity = m;
			int pData = 0;
			while( fi.available() > 0 ){
				pData = 0;
				parity = ++parity%m;
		/*		parity--;
				if( parity < 0 )
					parity = m - 1;*/
				for( int i = 0; i < m; i++ ){
					if( i != parity && fi.available() > 0 ){
						block = fi.read();
						fos[ i ].write(block);
						pData = pData ^ block;
						System.out.println("data: " + (char)block + " i: "+ i);
					}
//					written++;
				}
	//			System.out.println("Parity: " + parity + " written: " + written);
				fos[ parity ].write(pData);

			}
		}catch( FileNotFoundException e ){
			e.printStackTrace();
		}catch( IOException e ){
			e.printStackTrace();
		}
	}

	/*
	 * FileChunks must be in order from 0 -> M with null being given to missing
	 * files
	 */
	public void reassemble(String iPath, String[] fileChunks, String oPath,
			String outputFile) {
		try{
			int m = fileChunks.length;
			FileOutputStream fo = new FileOutputStream(oPath + outputFile);

			FileInputStream fis[] = new FileInputStream[m];
			for( int i = 0; i < m; i++ ){
				try{
					fis[ i ] = new FileInputStream(iPath + fileChunks[ i ]);
				}catch( FileNotFoundException e ){
					fis[i] = null;
				}
			}
			int block[] = new int[m];

			int pData = 0;
			boolean done = false;
			int regenerate = -1;
			int parity = m;
			while(!done){
				pData = 0;
				parity = ++parity%m;
		/*		parity--;
				if( parity < 0 )
					parity = fileChunks.length - 1;*/
				for( int i=0; i < m; i++){
					if( fis[i] != null){
						block[i] = fis[i].read();
						if( block[i] >= 0){
							pData = (block[i] ^ pData);
							System.out.println("Reading: "+ block[i] + " i: "+ i + " pData: "+ pData);
						}else{
							done = true;
						}
					}else if( fis[i] == null){
						regenerate = i;
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

		}catch( FileNotFoundException e ){
			e.printStackTrace();
		}catch( IOException e ){
			e.printStackTrace();
		}

	}


/*	  public void chunk(String fName, int m, int n) {
		String path = "/Users/kevincheek/Desktop/t/";
		System.out.println("test");
		try{
			File file = new File(path + fName);
			FileInputStream fi = new FileInputStream(file);
			long size = file.length();
			FileOutputStream fos;
			long chunkSize = size / m;
			for( int i = 0; i < m; i++ ){
				fos = new FileOutputStream(path + i + "_" + fName);
				long written = 0;
				while( fi.available() > 0 && written < chunkSize ){
					fos.write(fi.read());
					written++;
					System.out.println("Writing");
				}
			}
		}catch( FileNotFoundException e ){
			e.printStackTrace();
		}catch( IOException e ){
			e.printStackTrace();
		}
	}

	public String[] Split() {
		return null;
	}

	private int setupTables(int w) {
		int b, log, x_to_w, prim_poly;
		switch( w ){
			case 4:
				prim_poly = prim_poly_4;
				break;
			case 8:
				prim_poly = prim_poly_8;
				break;
			case 16:
				prim_poly = prim_poly_16;
				break;
			default:
				return -1;
		}
		x_to_w = 1 << w;
		System.out.println("x to w: " + x_to_w);
		gflog = new short[x_to_w];
		gfilog = new short[x_to_w];
		b = 1;
		for( log = 0; log < x_to_w - 1; log++ ){
			System.out.println("log: " + log);
			gflog[ b ] = (short) log;
			gfilog[ log ] = (short) b;
			b = b << 1;
			if( (b & x_to_w) != 0 )
				b = b ^ prim_poly;
		}
		return 0;
	}*/

}
