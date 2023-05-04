#@ String filepath
#@ DatasetIOService ioService

import org.janelia.saalfeldlab.n5.*
import org.janelia.saalfeldlab.n5.imglib2.N5Utils
import java.util.concurrent.Executors

def inputPath = new File(filepath)
def positions = []

if (inputPath.isDirectory()) {
	def files = inputPath.listFiles()
	for (file : files)
		if (file.isDirectory())
			positions.add(new File(file.getAbsolutePath() + "/metadata.txt"))

    if (positions.size() == 0)
			positions.add(new File(inputPath.getAbsolutePath() + "/metadata.txt"))
} else {
	positions.add(inputPath)
}

for (file : positions) {
	println "Processing " + file.getAbsolutePath()

	if (!file.exists()) {
		println "Unrecognized format, could not locate metadata.txt file."
		continue
	}

	def dataset = ioService.open(file.getAbsolutePath())
	println "Dimensions " + dataset.dimensionsAsLongArray()

	def blockSize
	if (dataset.numDimensions() == 5) {
		blockSize = new int[] {128, 128, 1, 1, 64}
	} else if (dataset.numDimensions() == 4) {
		blockSize = new int[] {128, 128, 1, 64}
	} else if (dataset.numDimensions() == 3) {
		blockSize = new int[] {128, 128, 64}
	} else {
		println "Unexpected number of dimensions " + dataset.numDimensions() + ". Aborting."
		continue
	}

	N5Utils.save(
	    dataset.getImgPlus(),
	    new N5FSWriter(file.getParent() + ".n5"),
	    "/data/" + file.getParentFile().getName(),
	    blockSize,
	    new GzipCompression(),
		  Executors.newFixedThreadPool( 30 ))
}

println "Done converting dataset to N5"
System.exit(0);
