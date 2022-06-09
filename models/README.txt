Model files are so named because they deal with the Earth model and earthquake 
statistic.  The first time the Locator code is run, it will create a properties file called locator.prop in a directory called Properties under the callers home directory.  
This file should be edited so that the modelPath variable points to a directory where 
the model files will reside.  the eventPath variable should point to a directory 
where earthquake input files could be placed.  Note that the NEIC Hydra style input 
files have been used for testing.  It is anticipated that input in production will be 
via the JSON interface.  The model files currently included are:

cratons: Boundaries of cratons.  These are used to control the tectonic flag.

zonekey.dat: Indicies into zonestat.dat.

zonestat.dat: Earthquake statistics in Marsden squares.  These statistics are used 
   to set default Bayesian depth constraints.

ellip.txt: Corrections for the ellipticity of the Earth.  These corrections are common 
   to all Earth models.

groups.txt: This specifies all possible phases in the current set up and their 
   relationship to each other.

mak135.mod: The ak135 Earth model which will drive the Java travel-time table 
   generation.  Ak135 is currently the NEIC global default.

mcia.mod: The Central Italian Apennines Earth model which will drive the Java travel-
   time table generation.  This model works in parts of California.

mcus.mod: The Central US Earth model which will drive the Java travel-time table 
   generation.  This model is useful east of the Rocky Mountains.

mogs.mod: The Oklahoma Geological Survey Earth model which will drive the Java 
   travel-time table generation.  This model is useful in Oklahoma which seems to 
   be somewhere between the Central and Western US models.

mwus.mod: The Western US Earth model which will drive the Java travel-time table 
   generation.  This model is useful west of the Great Plains.

phases.txt: This specifies the phase segments that will be created during the travel-
   time generation process.

topo.dat: A topographic map of the Earth that is used to compute bounce point 
   corrections.

ttstats.txt: Travel-time statistics, used to weight phases and generate "add-on" 
   phases.