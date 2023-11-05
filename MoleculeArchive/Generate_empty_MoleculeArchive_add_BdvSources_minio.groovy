#@OUTPUT MoleculeArchive archive
#@ String fullpath
#@ String channel1Name
#@ String channel2Name
//#@ String channel3Name
#@ OMEXMLService omexmlService
#@ TranslatorService translatorService
#@ Context scijavaContext
#@ LogService logService

import de.mpg.biochem.mars.molecule.*
import de.mpg.biochem.mars.metadata.*
import io.scif.Metadata
import io.scif.ome.OMEMetadata
import loci.common.services.ServiceException
import de.mpg.biochem.mars.util.MarsMath
import de.mpg.biochem.mars.metadata.*
import java.util.Arrays
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import org.apache.commons.io.IOUtils

import de.mpg.biochem.mars.n5.MarsN5ViewerReaderFun
import org.janelia.saalfeldlab.n5.N5Reader
import de.mpg.biochem.mars.scifio.MarsMicromanagerFormat

//We assume the fullpath ends with .n5 and use the text before as the name
def archiveName = fullpath.substring(fullpath.lastIndexOf("/") + 1, fullpath.length() - 3)
archive = new DnaMoleculeArchive(archiveName)

def n5 = new MarsN5ViewerReaderFun().apply(fullpath)
def keyValueAccess = n5.getKeyValueAccess()

def uri = new URI(fullpath)
def metadataPaths = []
def positionNames = []

//We only look for up to 100 positions..
for (int i=0;i<100;i++) {
	def metadataPath = uri.getPath() + "/Pos" + i + "/metadata.txt"
	if (keyValueAccess.exists(metadataPath)) {
		metadataPaths.add(metadataPath)
		positionNames.add("Pos" + i)
	} else break
}

for (int j = 0; j < metadataPaths.size(); j++) {
	println "Processing " + metadataPaths[j]

	InputStream inputStream = keyValueAccess.lockForReading(metadataPaths[j]).newInputStream()
	String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8)
	String[] jsonData = new String[1]
	jsonData[0] = result

	MarsMicromanagerFormat.Parser parser = new MarsMicromanagerFormat.Parser()
	MarsMicromanagerFormat.Metadata source = new MarsMicromanagerFormat.Metadata()

	if (source.getContext() == null) source.setContext(scijavaContext)
	if (parser.getContext() == null) parser.setContext(scijavaContext)

	def posList = []
	MarsMicromanagerFormat.Position p = new MarsMicromanagerFormat.Position()
	posList.add(p)
	source.setPositions(posList)

	parser.populateMetadata(jsonData, source, source, false)
	source.populateImageMetadata()

	OMEMetadata omeMeta = new OMEMetadata(scijavaContext)
	if (!translatorService.translate(source, omeMeta, true)) {
		logService.info("Unable to extract OME Metadata. Aborting...")
		break
	}
	def omexmlMetadata = omeMeta.getRoot()
	omexmlMetadata.setImageName(source.get(0).getName(), 0)

	def marsMetadata = new MarsOMEMetadata(MarsMath.getUUID58().substring(0, 10), omexmlMetadata)

	//Add MarsBdvSources
	def source1 = new MarsBdvSource(channel1Name)
	source1.setPath(fullpath)
	source1.setCorrectDrift(false)
	source1.setN5(true)
	source1.setChannel(0)
	source1.setN5Dataset(positionNames[j])
	//source1.setAffineTransform2D(double m00, double m01, double m02, double m10, double m11, double m12)
	//source1.setAffineTransform2D(1.002760279297564, 2.08945392555E-4, 1.712362557896246, 2.67010028894E-4, 1.003121285602708, 506.91025885480565)
	marsMetadata.putBdvSource(source1)

	def source2 = new MarsBdvSource(channel2Name)
	source2.setPath(fullpath)
	source2.setCorrectDrift(false)
	source2.setN5(true)
	source2.setChannel(1)
	source2.setN5Dataset(positionNames[j])
	//source1.setAffineTransform2D(double m00, double m01, double m02, double m10, double m11, double m12)
	source2.setAffineTransform2D(1.002760279297564, 2.08945392555E-4, 1.712362557896246, 2.67010028894E-4, 1.003121285602708, 506.91025885480565)
	marsMetadata.putBdvSource(source2)

	//def source3 = new MarsBdvSource(channel3name)
	//source3.setPath(fullpath)
	//source3.setCorrectDrift(false)
	//source3.setN5(true)
	//source3.setChannel(2)
	//source3.setN5Dataset(positionNames[j])
	//source1.setAffineTransform2D(double m00, double m01, double m02, double m10, double m11, double m12)
	//metadata.putBdvSource(source3)

	archive.putMetadata(marsMetadata)
}
