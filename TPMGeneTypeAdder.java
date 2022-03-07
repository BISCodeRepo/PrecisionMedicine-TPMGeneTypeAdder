

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

public class TPMGeneTypeAdder {

	public static int totalOfNAs = 0;
	public static int totalOfTPMs = 0;
	public static int processedTPMs = 0;
	
	public static void main(String[] args) throws IOException {
		
		Hashtable<String, String> argMap = parseArgument(args);
		
		if(argMap.size() != 2) {
			System.out.println("Usage: java -jar geneTypeMapper.jar -gtf [gtf file path] -tpm [tpm folder or file path]");
			System.out.println("Note that tpm file name must end with tpm.txt");
			System.out.println("Otherwise the file will be ignored.");
			System.exit(1);
		}
		
		String gtfFilePath = argMap.get("gtf");
		String tpmFolderPath = argMap.get("tpm");
		
		Hashtable<String, String> gIDtoTypeMapper = getGeneIDtoTypeMapper(gtfFilePath);
		
		// read TPM file or folder
		File[] tpmFiles = null;
		File tpmFolder = new File(tpmFolderPath);
		if(!tpmFolder.isDirectory()) {
			System.out.println(tpmFolder.getName()+" is not folder.");
			System.out.println("Consider this is tpm file");
			tpmFiles = new File[1];
			tpmFiles[0] = tpmFolder;
		} else {
			tpmFiles = tpmFolder.listFiles();
		}
		
		for(File file : tpmFiles) {
			if(file.getName().endsWith("tpm.txt")) {
				totalOfTPMs++;
			}
		}
		
		for(File file : tpmFiles) {
			if(!file.getName().endsWith("tpm.txt")) {
				System.out.println(file.getName()+" is not tpm file. skipped");
				continue;
			}
			
			processedTPMs++;
			
			System.out.println(processedTPMs+"/"+totalOfTPMs);
			
			BufferedReader BR = new BufferedReader(new FileReader(file));
			BufferedWriter BW = new BufferedWriter(new FileWriter(file.getAbsolutePath().replace("tpm.txt", "tpm.type.txt")));
			String header = BR.readLine();
			String line = null;
			
			BW.append(header);
			BW.newLine();
			while((line = BR.readLine()) != null) {
				String[] fields = line.split("\t");
				String geneID = fields[1].replace("\"", "");
				String geneType = gIDtoTypeMapper.get(geneID);
				
				if(geneType == null) {
					System.out.println(geneID+" is not included in the given GTF file");
					System.out.println("This will be added as \"NA\"");
					BW.append(line+"\t\""+"NA"+"\"");
					BW.newLine();
					totalOfNAs++;
				} else {
					BW.append(line+"\t\""+geneType+"\"");
					BW.newLine();
				}
			}
			
			BR.close();
			BW.close();
		}
		
		if(totalOfNAs == 0) {
			System.out.println(" All files are completely processed.");
		} else {
			System.out.println("A total of "+totalOfNAs+" were found. You should check GTF version and TPM version");
		}
	}
	
	public static void createNewTPMTable (File file) throws IOException {
		
	}
	
	/**
	 * Read GTF file and return a mapper such as: <br>
	 * key: gene id with version number
	 * value: gene type (ex. protein_coding)
	 * 
	 * @param gtfFilePath
	 * @return
	 * @throws IOException
	 */
	public static Hashtable<String, String> getGeneIDtoTypeMapper (String gtfFilePath) throws IOException {
		Hashtable<String, String> mapper = new Hashtable<String, String>();
		File file = new File(gtfFilePath);
		
		System.out.println("reading "+file.getName());
		if(!file.exists()) {
			System.out.println("Wrong file path: "+gtfFilePath);
			System.exit(1);
		}
		
		BufferedReader BR = new BufferedReader(new FileReader(file));
		
		String line = null;
		
		while((line = BR.readLine()) != null) {
			// pass meta data
			if(line.startsWith("#")) {
				continue;
			}
			
			String[] fields = line.split("\t");
			// select only gene feature
			if(fields[2].equalsIgnoreCase("gene")) {
				// get attributes
				String[] attrs = fields[8].split("\\;");
				
				String geneID = null;
				String geneType = null;
				
				for(String attr : attrs) {
					attr = attr.trim();
					if(attr.startsWith("gene_id")) {
						geneID = attr.split("\"")[1];
					} else if(attr.startsWith("gene_type")) {
						geneType = attr.split("\"")[1];
					}
				}
				
				if(geneID == null || geneType == null) {
					System.out.println("-- wrong format was detected --");
					System.out.println(line);
				} else {
					
					if(mapper.get(geneID) != null) {
						System.out.println("-- duplciated gene id was detected --");
						System.out.println(geneID);
					}
					
					mapper.put(geneID, geneType);
				}
			}
		}
		
		BR.close();
		
		System.out.println("A total of "+mapper.size()+" genes were retrieved");
		
		return mapper;
	}
	
	/**
	 * Parsing arguments
	 * 
	 * @param args
	 * @return
	 */
	public static Hashtable<String, String> parseArgument (String[] args) {
		Hashtable<String, String> argMap = new Hashtable<String, String>();
		
		for(int i=0; i<args.length; i++) {
			if(args[i].equalsIgnoreCase("-gtf")) {
				argMap.put("gtf", args[++i]);
				System.out.println("GTF path: "+args[i]);
			} else if(args[i].equalsIgnoreCase("-tpm")) {
				argMap.put("tpm", args[++i]);
				System.out.println("TPM file path: "+args[i]);
			}
		}
		
		return argMap;
	}
}
