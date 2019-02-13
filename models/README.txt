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