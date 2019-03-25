package gov.usgs.locator;

import gov.usgs.traveltime.TauUtil;

/**
 * Keep all the data for one hypocenter together.
 * 
 * @author Ray Buland
 *
 */
public class Hypocenter {
	/** 
   * A double containing the origin time in seconds since the epoch.
   */
	private double originTime; 
	
	/** 
   * A double containing the geographic latitude in degrees.
   */
  private double latitude; 

  /**
   * A double containing the geographic longitude in degrees.
   */
  private double longitude;

  /**
   * A double containing the depth in kilometers.
   */  
	private double depth;
	
  /** 
   * A double containing the Bayesian depth in kilometers.
   */
	private double bayesianDepth; 
	
  /** 
   * A double containing the Bayesian depth spread in kilometers.
   */
	private double bayesianDepthSpread;
	
  /**
   * A double containing the bayesian depth residual in kilometers
   */	
	private double bayesianDepthResidual;

	  /**
   * A double containing the bayesian depth weight
   */	
	private double bayesianDepthWeight;

	/**
	 * An int containing the degrees of freedom
	 */
	private int degreesOfFreedom;
	
  /**
   * A double containing the geocentric colatitude in degrees.
   */
  private double coLatitude;

  /**
   * A double containing the sine of the geocentric colatitude in degrees.
   */
  private double coLatitudeSine; 

  /**
   * A double containing the cosine of the geocentric colatitude in degrees.
   */
  private double coLatitudeCosine; 

  /**
   * A double containing the sine of the geocentric longitude in degrees.
   */
  private double longitudeSine; 

  /**
   * A double containing the cosine of the geocentric longitude in degrees.
   */
	private double longitudeCosine; 
	
	/**
   * An int containing the number of times step length damping has been applied.
   */
	private int numOfTimesStepLengthDampening;

	/**
   * A double containing the linear estimate of the origin time shift in seconds.
   */
	private double linearTimeShiftEstimate;

  /**
   * A double containing the R-estimator dispersion or penalty value
   */	
	private double estimatorDispersionValue;

  /**
   * A double containing the R-estimator equivalent of the least squares RMS
   */		
	private double estimatorRMSEquivalent;	

	/**
   * A double containing the step length in kilometers
   */
	private double stepLength;

	/**
   * A double containing the horizontal (tangential) step length in kilometers
   */	
	private double horizontalStepLength;

	/**
   * A double containing the vertical (depth) step length in kilometers
   */		
	private double verticalStepLength;

	/**
   * A double array containing the spatial local Cartesian step direction unit 
	 * vector
   */		
	private double[] stepDirectionUnitVector;
	
	/**
   * Function to return the origin time.
   * 
   * @return A double containing the origin time in double precision 
   *         seconds since the epoch
	 */
	public double getOriginTime() { 
		return originTime;
	}
	
	/**
   * Function to return the latitude.
   * 
   * @return A double containing the epicenter geographic latitude in degrees
   */
	public double getLatitude() {
		return latitude;
	}
	
	/**
   * Function to return the longitude.
   * 
   * @return A double containing the epicenter geographic longitude in degrees
	 */
  public double getLongitude() {
		return longitude;
	}
  
  /**
   * Function to return the depth.
   * 
   * @return A double containing the hypocenter depth in kilometers
   */
  public double getDepth() {
		return depth;
	}
	
  /**
   * Function to return the bayesian depth.
   * 
   * @return A double containing the hypocenter bayesian depth in kilometers
   */	
	public double getBayesianDepth() {
		return bayesianDepth;
	}

  /**
   * Function to return the bayesian depth. spread
   * 
   * @return A double containing the hypocenter bayesian depth spread in 
	 * 				 kilometers
   */	
	public double getBayesianDepthSpread() {
		return bayesianDepthSpread;
	}

  /**
   * Function to return the bayesian depth residual.
   * 
   * @return A double containing the hypocenter bayesian depth residual in kilometers
   */	
	public double getBayesianDepthResidual() {
		return bayesianDepthResidual;
	}

	/**
   * Function to return the bayesian depth weight.
   * 
   * @return A double containing the hypocenter bayesian depth weight
   */	
	public double getBayesianDepthWeight() {
		return bayesianDepthWeight;
	}

  /**
   * Function to return the degrees of freedom for this hypocenter
   * 
   * @return An int containing the degrees of freedom
   */	
	public int getDegreesOfFreedom() {
		return degreesOfFreedom;
	}

	/**
   * Function to return the colatitude.
   * 
   * @return A double containing the colatitude in degrees
   */
  public double getCoLatitude() {
    return coLatitude;
  }

 	/**
   * Function to return the sine of the colatitude.
   * 
   * @return A double containing the sine of the colatitude in degrees
   */
  public double getCoLatitudeSine() {
    return coLatitudeSine;
  }

  /**
   * Function to return the cosine of the colatitude.
   * 
   * @return A double containing the cosine of the colatitude in degrees
   */
  public double getCoLatitudeCosine() {
    return coLatitudeCosine;
  }

  /**
   * Function to return the sine of the longitude.
   * 
   * @return A double containing the sine of the longitude in degrees
   */
  public double getLongitudeSine() {
    return longitudeSine;
  }

  /**
   * Function to return the cosine of the longitude.
   * 
   * @return A double containing the cosine of the longitude in degrees
   */
  public double getLongitudeCosine() {
    return longitudeCosine;
  }

	/**
   * Function to get the number of times step length damping has been 
	 * applied.
   * 
   * @return An int containing the number of times step length damping has been 
	 * 				 applied.
   */	
	public int getNumOfTimesStepLengthDampening() {
		return numOfTimesStepLengthDampening;
	}

  /**
   * Function to return the linear estimate of the origin time shift
   * 
   * @return A double containing the linear estimate of the origin time shift in 
	 * 				 seconds.
   */
  public double getLinearTimeShiftEstimate() {
    return linearTimeShiftEstimate;
  }

  /**
   * Function to return the R-estimator dispersion or penalty value
   * 
   * @return A double containing the R-estimator dispersion or penalty value
   */
  public double getEstimatorDispersionValue() {
    return estimatorDispersionValue;
  }

  /**
   * Function to return the R-estimator equivalent of the least squares RMS
   * 
   * @return A double containing the R-estimator equivalent of the least squares 
	 * 				 RMS
   */
  public double getEstimatorRMSEquivalent() {
    return estimatorRMSEquivalent;
	}

 	/**
   * Function to return the step length
   * 
   * @return A double containing the step length in kilometers
   */
  public double getStepLength() {
    return stepLength;
  }

 	/**
   * Function to return the horizontal (tangential) step length
   * 
   * @return A double containing the horizontal (tangential) step length in 
	 * 				 kilometers
   */
  public double getHorizontalStepLength() {
    return horizontalStepLength;
  }

 	/**
   * Function to return the vertical (depth) step length
   * 
   * @return A double containing the vertical (depth) step length in kilometers
   */
  public double getVerticalStepLength() {
    return verticalStepLength;
  }	

 	/**
   * Function to return the spatial local Cartesian step direction unit vector
   * 
   * @return A double[] the spatial local Cartesian step direction unit vector
   */
  public double[] getStepDirectionUnitVector() {
    return stepDirectionUnitVector;
  }	

	/**
   * Function to set the bayesian depth weight.
   * 
   * @param bayesianDepthWeight A double containing the hypocenter bayesian 
	 * 														depth weight
   */	
	public void setBayesianDepthWeight(double bayesianDepthWeight) {
		this.bayesianDepthWeight = bayesianDepthWeight;
	}

	/**
   * Function to set the number of times step length damping has been 
	 * applied.
   * 
   * @param numOfTimesStepLengthDampening An int containing the number of times 
	 * 																		 step length damping has been applied.
   */	
	public void setNumOfTimesStepLengthDampening(int numOfTimesStepLengthDampening) {
		this.numOfTimesStepLengthDampening = numOfTimesStepLengthDampening;
	}

/**
   * Function to set the linear estimate of the origin time shift
   * 
   * @param linearTimeShiftEstimate A double containing the linear estimate of 
	 * 				 												the origin time shift in seconds.
   */
  public void setLinearTimeShiftEstimate(double linearTimeShiftEstimate) {
    this.linearTimeShiftEstimate = linearTimeShiftEstimate;
  }

  /**
   * Function to set the R-estimator dispersion or penalty value
   * 
   * @param estimatorDispersionValue A double containing the R-estimator 
	 * 																 dispersion or penalty value
   */
  public void setEstimatorDispersionValue(double estimatorDispersionValue) {
    this.estimatorDispersionValue = estimatorDispersionValue;
  }

  /**
   * Function to set the R-estimator equivalent of the least squares RMS
   * 
   * @param estimatorRMSEquivalent A double containing the R-estimator 
	 * 															 equivalent of the least squares RMS
   */
  public void setEstimatorRMSEquivalent(double estimatorRMSEquivalent) {
    this.estimatorRMSEquivalent = estimatorRMSEquivalent;
	}

	/**
   * Function to set the step length
   * 
   * @param stepLength A double containing tthe step length in kilometers
   */
  public void setStepLength(double stepLength) {
    this.stepLength = stepLength;
  }

	/**
   * Function to set the horizontal (tangential) step length
   * 
   * @param horizontalStepLength A double containing the horizontal (tangential) 
	 * 														 step length in kilometers
   */
  public void setHorizontalStepLength(double horizontalStepLength) {
    this.horizontalStepLength = horizontalStepLength;
  }

 	/**
   * Function to set the vertical (depth) step length
   * 
   * @param verticalStepLength A double containing the vertical (depth) step 
	 * 													 length in kilometers
   */
  public void setVerticalStepLength(double verticalStepLength) {
    this.verticalStepLength = verticalStepLength;
  }	

	/**
   * Function to set the spatial local Cartesian step direction unit vector
   * 
   * @param stepDirectionUnitVector A double[] the spatial local Cartesian step 
	 * 																direction unit vector
   */	
	public void setStepDirectionUnitVector(double[] stepDirectionUnitVector) {
		this.stepDirectionUnitVector = stepDirectionUnitVector;
	}

	/**
	 * The Hypocenter constructor. Initializes a hypocenter with enough 
	 * information to start a location pass.
	 * 
	 * @param originTime A double containing the origin time in seconds since the 
	 * 									 epoch
	 * @param latitude A double containing the geographic latitude in degrees
	 * @param longitude A double containing the geographic longitude in degrees
	 * @param depth A double containing the depth in kilometers
	 */
	public Hypocenter(double originTime, double latitude, double longitude, 
			double depth) {
		// Set up the hypocenter.
		depth = Math.min(Math.max(depth, LocUtil.DEPTHMIN), 
				LocUtil.DEPTHMAX);
		updateHypo(originTime, latitude, longitude, depth);

		// Set defaults for the rest.
		bayesianDepth = Double.NaN;
		bayesianDepthSpread = Double.NaN;
		bayesianDepthResidual = Double.NaN;
		bayesianDepthWeight = Double.NaN;
		linearTimeShiftEstimate = 0d;
		stepLength = 0d;
		horizontalStepLength = 0d;
		verticalStepLength = 0d;
	}
	
	/**
	 * This functino sets an analyst requested Bayesian depth.  Note that this 
	 * forces the event starting depth to the Bayesian depth.
	 * 
	 * @param bayesianDepth A double containing the Bayesian depth in kilometers
	 * @param bayesianDepthSpread A double containing the uncertainty of the 
	 * 														Bayesian depth in kilometers
	 */
	public void addBayes(double bayesianDepth, double bayesianDepthSpread) {
		this.bayesianDepth = Math.min(Math.max(bayesianDepth, LocUtil.DEPTHMIN), 
				LocUtil.DEPTHMAX);
		this.bayesianDepthSpread = bayesianDepthSpread;

		depth = bayesianDepth;
		bayesianDepthResidual = 0d;
		bayesianDepthWeight = 3d / bayesianDepthSpread;
	}
	
	/**
	 * This function sets the number of event degrees of freedom to be determined.
	 * 
	 * @param heldLoc A boolean flag, true if the hypocenter will be held constant
	 * @param heldDepth A boolean flag, true if the depth will be held constant
	 */
	public void setDegreesOfFreedom(boolean heldLoc, boolean heldDepth) {
		if(heldDepth) {
			degreesOfFreedom = 2;
		} else {
			degreesOfFreedom = 3;
		}

		if(degreesOfFreedom > 0) {
			stepDirectionUnitVector = new double[degreesOfFreedom];
		}
	}
	
	/**
	 * Update the hypocentral parameters and recompute the sines and cosines.
	 * 
	 * @param originTime Event origin time in seconds since the epoch
	 * @param latitude Geographic latitude in degrees
	 * @param longitude Longitude in degrees
	 * @param depth Depth in kilometers
	 */
	public void updateHypo(double originTime, double latitude, double longitude, 
			double depth) {
		// Update the hypocenter.
		this.originTime = originTime;
		this.latitude = latitude;
		this.longitude = longitude;
		this.depth = depth;
		// Update the sines and cosines.
		coLatitude = TauUtil.geoCen(latitude);
		updateSines();
		// Update the Bayesian depth residual.
		if(!Double.isNaN(bayesianDepth)) bayesianDepthResidual = bayesianDepth-depth;
	}
	
	/**
	 * Move the hypocenter based on the linearized optimal step.
	 * 
	 * @param stepLength Step length in kilometers
	 * @param dT Origin time shift in seconds
	 */
	public void updateHypo(double stepLength, double dT) {
		double tmpDepth;
		
		// Save the convergence variable.
		this.stepLength = stepLength;
		// Update the origin time.
		originTime += dT;
		// Compute the tangential step length for tracking purposes.
		horizontalStepLength = Math.sqrt(Math.pow(stepLength*stepDirectionUnitVector[0], 2d)+
				Math.pow(stepLength*stepDirectionUnitVector[1], 2d));
		// Update the colatitude and longitude.
		coLatitude += stepLength*stepDirectionUnitVector[0]/LocUtil.DEG2KM;
		longitude += stepLength*stepDirectionUnitVector[1]/(LocUtil.DEG2KM*coLatitudeSine);
		// Make sure the colatitude is legal.
		if(coLatitude < 0d) {
			coLatitude = Math.abs(coLatitude);
			longitude += 180d;
		} else if(coLatitude > 180d) {
			coLatitude = 360d-coLatitude;
			longitude += 180d;
		}
		// Make sure the longitude is legal.
		if(longitude < -180d) {
			longitude += 360d;
		} else if(longitude > 180d) {
			longitude -= 360d;
		}
		// Deal with depth separately.
		if(degreesOfFreedom > 2) {
			tmpDepth = Math.min(Math.max(depth+stepLength*stepDirectionUnitVector[2], 
					LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);
			verticalStepLength = tmpDepth-depth;
			depth = tmpDepth;
		}
		// Compute the geographic latitude.
		latitude = TauUtil.geoLat(coLatitude);
		// Update the sines and cosines.
		updateSines();
		// Update the Bayesian depth residual.
		if(!Double.isNaN(bayesianDepth)) bayesianDepthResidual = bayesianDepth-depth;
	}
	
	/**
	 * Compute the sines and cosines of colatitude and longitude.
	 */
	private void updateSines() {
		coLatitudeSine = Math.sin(Math.toRadians(coLatitude));
		coLatitudeCosine = Math.cos(Math.toRadians(coLatitude));
		longitudeSine = Math.sin(Math.toRadians(longitude));
		longitudeCosine = Math.cos(Math.toRadians(longitude));
	}
	
	/**
	 * Update the origin time.
	 * 
	 * @param dT Shift in the origin time in seconds
	 */
	public void updateOrigin(double dT) {
		originTime += dT;
	}
	
	/**
	 * Update the Bayesian depth (if not set by an analyst).
	 * 
	 * @param bayesianDepth Bayesian depth in kilometers
	 * @param bayesianDepthSpread Uncertainty of the Bayesian depth in kilometers
	 */
	public void updateBayes(double bayesianDepth, double bayesianDepthSpread) {
		this.bayesianDepth = bayesianDepth;
		this.bayesianDepthSpread = bayesianDepthSpread;
		bayesianDepthResidual = bayesianDepth-depth;
		// The Bayesian spread is actually taken as a 90th percentile.
		bayesianDepthWeight = 3d/bayesianDepthSpread;
	}
	
	/**
	 * Reset key hypocentral parameters to a backup (used for step length 
	 * damping).
	 * 
	 * @param backup Hypocenter audit record
	 */
	public void resetHypo(HypoAudit backup) {
		originTime = backup.getOriginTime();
		latitude = backup.getLatitude();
		longitude = backup.getLongitude();
		depth = backup.getDepth();
	}
	
	/**
	 * A convenient way of dumping the current hypocenter.
	 */
	@Override
	public String toString() {
		return String.format("%14.3f %8.4f %9.4f %6.2f", originTime, latitude, 
				longitude, depth);
	}
}
