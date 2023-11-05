#@ MoleculeArchive archive
#@OUTPUT MarsTable outputTable 

import org.scijava.table.*
import de.mpg.biochem.mars.table.*

outputTable = new MarsTable(archive.getName() + "_global_segment_tables", "X1", "Y1", "X2", "Y2", "A", "Sigma_A", "B", "Sigma_B")

//Add two string columns
outputTable.add(new GenericColumn("UID"))
outputTable.add(new GenericColumn("Region"))

def rowIndex = 0
archive.molecules().forEach{ molecule ->	
	def names = molecule.getSegmentsTableNames()
	if (names.size() > 0) {
		for (name : names) {
			def table = molecule.getSegmentsTable(name)
			table.rows().forEach{ row -> 
				outputTable.appendRow()
				
				row.columnNames().forEach{ col -> outputTable.setValue(col, rowIndex, row.getValue(col))}
				outputTable.setValue("UID", rowIndex, molecule.getUID())
				if (name.size() > 2) outputTable.setValue("Region", rowIndex, name[2])
				
				rowIndex++
			}
		}
	}
}