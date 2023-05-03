#@ String filepath //The full file to the MicroManager folder containing the PosX subfolders
#@ OpService opService

import java.io.FileFilter
import java.io.File
import ij.ImagePlus
import ij.io.FileSaver
import ij.IJ
import ij.process.ImageProcessor
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream
import java.util.List
import java.util.ArrayList
import java.util.stream.Collectors
import java.lang.Runnable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import ij.process.ImageConverter

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import net.imglib2.img.ImagePlusAdapter

int nThreads = 30

String convertedDatasetSuffix = "_32bit"

if (new File(filepath + convertedDatasetSuffix).exists()) {
	println "Converted dataset already exists."
} else {
	for (int i=0; i < 4; i++) {
		def original = new File(filepath + "/Pos" + i)
		if (!original.exists())
			continue

		def pos = new File(filepath + convertedDatasetSuffix + "/Pos" + i)
		pos.mkdirs()

		File inFile = new File(filepath + "/Pos" + i + "/metadata.txt")
		File outFile = new File(filepath + convertedDatasetSuffix + "/Pos" + i + "/metadata.txt")
		BufferedReader bufferedReader = new BufferedReader(new FileReader(inFile))
	    if (!outFile.exists()) {
	        outFile.createNewFile()
	    }
	    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outFile))

			Pattern pattern1 = Pattern.compile("GRAY16")
			String replaceMent1 = "GRAY32"
			Pattern pattern2 = Pattern.compile("\"scalar\": \"16bit\"")
			String replaceMent2 = "\"scalar\": \"32bit\""
			Pattern pattern3 = Pattern.compile("\"BitDepth\": 16,")
			String replaceMent3 = "\"BitDepth\": 32,"
			Pattern pattern4 = Pattern.compile("\"IJType\": 1,")
			String replaceMent4 = "\"IJType\": 2,"
	    try {
	        String line
	        while ((line = bufferedReader.readLine()) != null) {
							String line2 = pattern1.matcher(line).replaceAll(replaceMent1)
							String line3 = pattern2.matcher(line2).replaceAll(replaceMent2)
							String line4 = pattern3.matcher(line3).replaceAll(replaceMent3)
							String line5 = pattern4.matcher(line4).replaceAll(replaceMent4)
	            bufferedWriter.write(line5)
	            bufferedWriter.newLine()
	        }
	    } finally {
	        bufferedReader.close()
	        bufferedWriter.flush()
	        bufferedWriter.close()
	    }

		File[] files = original.listFiles(new FileFilter() {
		            public boolean accept(File file) {
	                    if(file.getName().endsWith(".tif"))
	                        return true
	                    else
	                		return false
		            }
		        })

		List<Runnable> tasks = new ArrayList<Runnable>()
		for (def file : files) {
		 	 def fullFilePath = file.getAbsolutePath()
		 	 def filename = file.getName()
			 tasks.add(new Runnable() {
						@Override
					  public void run() {
								def image = new ImagePlus(fullFilePath)
								ImageProcessor processor = image.getProcessor()
								def filesaver = new FileSaver(new ImagePlus("32bitImage", processor.convertToFloatProcessor()))
								filesaver.saveAsTiff(filepath + convertedDatasetSuffix + "/Pos" + i + "/" + filename)
					  }
			 })
		}

		ExecutorService threadPool = Executors.newFixedThreadPool(nThreads)
		try {
			tasks.forEach{task -> threadPool.submit(task)}
			threadPool.shutdown()
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
		}
		catch (InterruptedException e) {
			// handle exceptions
			e.getStackTrace()
		}
	}
}
