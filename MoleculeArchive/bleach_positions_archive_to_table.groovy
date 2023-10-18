#@ MoleculeArchive archive
#@OUTPUT MarsTable table

//This script creates a table of the bleach times from molecule positions
//Bleach times in fluorescence traces can be detected using the Mars>KCP>single change point finder
//The MarsTable that appears can be saved to csv for export to other software

import de.mpg.biochem.mars.table.MarsTable
import org.scijava.table.DoubleColumn

def position = "bleach"

def col = new DoubleColumn(position)
archive.molecules().filter{ molecule -> molecule.hasPosition(position) }.forEach{ molecule ->
	col.add(molecule.getPosition(position).getPosition())
}

table = new MarsTable("position_list")
table.add(col)
