package edu.unh.cs.EntityLinking;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.*;

import java.lang.Object;
import java.nio.charset.StandardCharsets;

import org.apache.lucene.util.IOUtils;

public class Entity {
	public static void main(String[] args) throws IOException, InterruptedException {
		System.setProperty("file.encoding", "UTF-8");
		
		if (args.length < 1) {
			System.out.println("Usage : <CBOR File>");
			System.exit(-1);
		}
		
		final String paragraphsFile = args[0];
		final FileInputStream fis = new FileInputStream(new File(paragraphsFile));
		for (Data.Paragraph p : DeserializeData.iterableParagraphs(fis)) {
			// System.out.println(p.getTextOnly());	
		}
		
		String command = "curl --version";
		String URL = "http://model.dbpedia-spotlight.org/en/annotate ";
		String option_1 = "--data-urlencode ";
		String query_text = "\"text=President Obama called Wednesday on Congress to extend a tax break for students included in last year's economic stimulus package, arguing that the policy provides more generous assistance.\" ";
		String option_2 = "--data \"confidence=0.35\" ";
		String option_3 = "-H \"Accept: application/json\"";
		
		String[] c = new String[] {command, URL, option_1, query_text, option_2, option_3};
		// System.out.println(Arrays.toString(command));
		Process p = Runtime.getRuntime().exec(command);
		
		BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		
		System.out.println("Output :");
		String out = null;
		while ((out = output.readLine()) != null) {
		    System.out.println(out);
		}
		
		System.out.println("Error :");
		while ((out = error.readLine()) != null) {
		    System.out.println(out);
		}
				
		
		// ProcessBuilder p = new ProcessBuilder( command, URL, option_1, query_text, option_2, option_3);
		
		// Process proc = p.start();
		// System.out.println(output(proc.getInputStream())); 
		// String result = IOUtils.toString(proc.getInputStream(), StandardCharsets.UTF_8);
		/*
		String c = "curl http://model.dbpedia-spotlight.org/en/annotate --data-urlencode \"text=President Obama called Wednesday on Congress to extend a tax break for students included in last year's economic stimulus package, arguing that the policy provides more generous assistance.\" --data \"confidence=0.35\" -H \"Accept: application/json\"";
		System.out.println(c);
		Process proc = Runtime.getRuntime().exec(c); // p.start();
		*/
	}
	
	private static String output(InputStream inputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new InputStreamReader(inputStream));
		    String line = null;
		    while ((line = br.readLine()) != null) {
		        sb.append(line + System.getProperty("line.separator"));
		
            }
		} 
		finally {
            br.close();
        }
		return sb.toString();
	}

		
}