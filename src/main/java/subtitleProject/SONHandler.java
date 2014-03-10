package subtitleProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

/**
 * Class to operate on .son file generated by projectX
 * @author Jacob
 *
 */
public class SONHandler {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);
	/**
	 * Iterates through .SON subtitlefile, and generates subtitleFragments based on loginfo and ocr result 
	 * @param sonFiles to parse
	 * @param properties
	 * @return Map with subtitleFramgnets with pid as key
	 * @throws IOException if .sonfile don't exists
	 */
	public static Map<String, List<SubtitleFragment>> sonHandler(Map<String, File> sonFiles, ResourceLinks resources) throws IOException{
		Map<String, List<SubtitleFragment>> subsToPids = new HashMap<String, List<SubtitleFragment>>();
		Iterator<String> it = sonFiles.keySet().iterator();
		while(it.hasNext()){
			//BufferedReader reader = null;
			String currentPid = it.next();
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sonFiles.get(currentPid)), "UTF-8"))){
			String line;
			List<SubtitleFragment> subtitleFragments = new ArrayList<SubtitleFragment>();
			while ((line = reader.readLine()) != null)
			{
				if(line.contains("Display_Area")){
					line = reader.readLine();
					String[] temp = line.split("\t");
					int no = Integer.parseInt(temp[0]);
					String[] timeStamp1 = temp[2].split(":");
					String[] timeStamp2 = temp[3].split(":");
					String timeStamp = timeStamp1[0]+":"+timeStamp1[1]+":"+timeStamp1[2]+","+timeStamp1[3]+"0 --> "+timeStamp2[0]+":"+timeStamp2[1]+":"+timeStamp2[2]+","+timeStamp2[3]+"0";
					File bmpFile = new File(resources.getOutput()+temp[4]);
					String pngFileName = bmpFile.getAbsolutePath().replaceFirst("\\.bmp$", ".png");
					//log.debug("Running commandline: "+resources.getConvert()+" "+bmpFile.getAbsolutePath() + " "+ pngFileName);
					ProcessRunner pr = new ProcessRunner("bash","-c",resources.getConvert()+" "+bmpFile.getAbsolutePath() + " "+ pngFileName);
					pr.run();
					bmpFile.delete();
					subtitleFragments.add(ocrFrame(new File(pngFileName), resources, timeStamp, no));
				}
			}
			
			//reader.close();
			subsToPids.put(currentPid, subtitleFragments);
			}
			}
		return subsToPids;
	}

	/**
	 * Image manipulation using ImageMagick, and the Ocr using Tesseract
	 * @param file image to ocr
	 * @param properties 
	 * @return SubtitleFragment with ocr result and timestamp
	 */
	private static SubtitleFragment ocrFrame(File file, ResourceLinks resources, String timeStamp, int number){
		//log.debug("Running commandline: "+resources.getConvert()+" -black-threshold 70% "+file.getAbsolutePath());
		ProcessRunner pr1 = new ProcessRunner("bash","-c",resources.getConvert()+" -black-threshold 70% "+file.getAbsolutePath());
		pr1.run(); 

		//String StringOutput1 = pr1.getProcessOutputAsString();
		//String StringError1 = pr1.getProcessErrorAsString();
		//log.debug(StringOutput1);
		//log.debug(StringError1);

		//log.debug("Running commandline: "+resources.getTesseract()+" "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig());
		ProcessRunner pr = new ProcessRunner("bash","-c",resources.getTesseract()+" "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig());
		pr.run();
		//String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
		File ocrTxt = new File(file.getAbsolutePath()+".txt");

		String line ="";
		String content ="";
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ocrTxt), "UTF-8"))){

			while ((line = reader.readLine()) != null)
			{			
				content += line.trim().toLowerCase()+"\n";
			}

			//reader.close();
			ocrTxt.delete();
			file.delete();
		}
		catch(Exception e){
			throw new RuntimeException(e.getMessage());
		}
		
//		if(!validText(content, resources)){
//			content = "";
//		}

		SubtitleFragment sf = new SubtitleFragment(number,timeStamp,content);
		return sf;
	}
}